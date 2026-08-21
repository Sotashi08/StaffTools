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

        if (record == null) {
            return;
        }

        records.add(0, record);
        trim();
        save();
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
                        !issuer.equals(selfName);

        boolean changed = false;

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

            if (player == null ||
                    player.isBlank()) {

                continue;
            }

            if (!lower.contains(
                    CommandAnalyzer.normalize(player)
            )) {

                continue;
            }

            if (record.getType().matchesResponse(lower) ||
                    CommandAnalyzer.isFailure(lower)) {

                record.setStatus(
                        PunishmentStatus.DONE
                );

                record.setResponse(message);
                changed = true;
            }
        }

        if (changed) {
            save();
        }
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
