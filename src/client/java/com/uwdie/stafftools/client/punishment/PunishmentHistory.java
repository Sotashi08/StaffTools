package com.uwdie.stafftools.client.punishment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores the full punishment history, auto-detects the punishment type from
 * sent commands and captures the server's chat response when possible.
 */
public final class PunishmentHistory {

    private static final int MAX_RECORDS = 300;
    private static final Type LIST_TYPE =
            new TypeToken<List<PunishmentRecord>>() {
            }.getType();

    private static final Gson GSON =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

    private static PunishmentHistory instance;

    private final Path path;
    private final List<PunishmentRecord> records =
            new ArrayList<>();

    /**
     * Macro or action that is currently being executed, so every record
     * its commands produce can name it. Kept alive for a short window: a
     * macro may run several commands, and a name must never leak into an
     * unrelated command typed much later.
     */
    private String pendingSource;
    private PunishmentRecord.SourceKind pendingKind;
    private long pendingMacroAt;

    /** How long a macro name stays attached to the commands after it. */
    private static final long MACRO_WINDOW_MS = 2_000L;

    private PunishmentHistory(Path path) {
        this.path = path;
    }

    public static void init(Path path) {
        instance = new PunishmentHistory(path);
        instance.load();
    }

    public static PunishmentHistory get() {
        return instance;
    }

    /** Called whenever the player sends a chat command. */
    public void onCommandSent(String command) {

        PunishmentRecord record =
                CommandAnalyzer.analyze(command);

        if (record != null) {

            // who issued it: the local player, since the command was
            // sent by this client. Broadcast issuers are never used here.
            MinecraftClient client =
                    MinecraftClient.getInstance();

            if (client.player != null) {

                record.setModerator(
                        client.player.getName().getString()
                );
            }

            record.setSourceName(consumeSource());
            record.setSourceKind(consumeKind());

            records.add(0, record);
            trim();
            save();
        }

        expireMacro();
    }

    /**
     * Names the macro or action that is about to be executed, so the
     * punishments its commands produce can reference it.
     */
    public void setPendingSource(
            String sourceName,
            PunishmentRecord.SourceKind sourceKind
    ) {

        this.pendingSource = sourceName;
        this.pendingKind = sourceKind;
        this.pendingMacroAt = System.currentTimeMillis();
    }

    /** The source name if it is still inside its window, else null. */
    private String consumeSource() {

        if (pendingSource == null) {
            return null;
        }

        if (System.currentTimeMillis() - pendingMacroAt
                > MACRO_WINDOW_MS) {

            pendingSource = null;
            return null;
        }

        return pendingSource;
    }

    /** Kind of the source that is still inside its window. */
    private PunishmentRecord.SourceKind consumeKind() {

        if (pendingSource == null) {
            return PunishmentRecord.SourceKind.MANUAL;
        }

        if (System.currentTimeMillis() - pendingMacroAt
                > MACRO_WINDOW_MS) {

            return PunishmentRecord.SourceKind.MANUAL;
        }

        return pendingKind == null
                ? PunishmentRecord.SourceKind.MANUAL
                : pendingKind;
    }

    /** Drops a stale source name so it cannot tag later commands. */
    private void expireMacro() {

        if (pendingSource != null &&
                System.currentTimeMillis() - pendingMacroAt
                        > MACRO_WINDOW_MS) {

            pendingSource = null;
        }
    }

    /** Called for every chat message to try to match pending punishments. */
    public void onMessageReceived(String message) {

        if (message == null ||
                message.isBlank() ||
                records.isEmpty()) {

            return;
        }

        String lower =
                CommandAnalyzer.normalize(message);

        String selfName = null;

        MinecraftClient client =
                MinecraftClient.getInstance();

        if (client.player != null) {

            selfName = CommandAnalyzer.normalize(
                    client.player.getName().getString()
            );
        }

        String issuer = CommandAnalyzer.extractIssuer(lower);

        boolean foreignIssued =
                issuer != null &&
                        selfName != null &&
                        !CommandAnalyzer.sameName(issuer, selfName);

        // a rejection skips the nick test: "недостаточно прав" is often
        // the whole reply
        boolean failure = CommandAnalyzer.isFailure(lower);

        PunishmentRecord best = null;
        int bestScore = -1;

        for (PunishmentRecord record : records) {

            if (record.getStatus() !=
                    PunishmentStatus.PENDING) {

                continue;
            }

            // A broadcast about another staff member's punishment
            // must never resolve OUR pending record.
            if (foreignIssued) {
                continue;
            }

            String player =
                    record.getPlayerName();

            if (player != null &&
                    !player.isBlank() &&
                    CommandAnalyzer.containsName(lower, player)) {

                // a nick match is the strongest signal there is
                int score =
                        100 + CommandAnalyzer.confirmationScore(
                                lower,
                                record.getType()
                        );

                if (score > bestScore) {
                    best = record;
                    bestScore = score;
                }

                continue;
            }

            // fallback: no nick in the reply, but the reply quotes the
            // command itself ("unban: недостаточно прав")
            if (failure &&
                    record.getType().mentionsOutcome(lower)) {

                best = record;
                bestScore = 50;
                break;
            }
        }

        if (best == null) {

            if (!failure) {
                return;
            }

            // nothing named at all: resolve the newest pending entry
            for (PunishmentRecord record : records) {

                if (record.getStatus() ==
                        PunishmentStatus.PENDING) {

                    best = record;
                    break;
                }
            }

            if (best == null) {
                return;
            }
        }

        PunishmentStatus outcome =
                failure
                        ? PunishmentStatus.FAILED
                        : CommandAnalyzer.resolveOutcome(
                                lower,
                                best.getType()
                        );

        if (outcome == null) {

            // the reply is about this player but proves nothing: leave
            // the entry pending, the timeout will handle it
            return;
        }

        best.setStatus(outcome);
        best.setResponse(message);

        save();
    }

    public List<PunishmentRecord> getRecords() {
        return List.copyOf(records);
    }

    public void clear() {
        records.clear();
        save();
    }

    private void trim() {
        while (records.size() > MAX_RECORDS) {
            records.remove(records.size() - 1);
        }
    }

    private void load() {
        try {

            if (!Files.exists(path)) {
                return;
            }

            String json = Files.readString(path);

            List<PunishmentRecord> loaded =
                    GSON.fromJson(json, LIST_TYPE);

            if (loaded != null) {

                records.clear();
                records.addAll(loaded);

                trim();
            }

        } catch (Exception e) {
            System.err.println(
                    "[StaffTools] Failed to load punishment history."
            );
        }
    }

    private void save() {
        try {

            Files.createDirectories(path.getParent());

            Files.writeString(
                    path,
                    GSON.toJson(records)
            );

        } catch (IOException e) {
            System.err.println(
                    "[StaffTools] Failed to save punishment history."
            );
        }
    }
}
