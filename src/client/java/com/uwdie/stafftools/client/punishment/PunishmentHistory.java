package com.uwdie.stafftools.client.punishment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores the full punishment history, auto-detects the punishment type from
 * sent commands and captures the server's chat response when possible.
 *
 * Every record is tagged with the raw address of the server it was issued
 * on, and each address gets a {@link ServerProfile} the user may name.
 * The log is then either read as one shared journal or filtered down to
 * one server.
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

    /** Servers seen so far, keyed by raw address. */
    private final Map<String, ServerProfile> servers =
            new LinkedHashMap<>();

    /** File layout: before per-server support it was a bare record list. */
    private static class Store {

        private List<PunishmentRecord> records = new ArrayList<>();
        private List<ServerProfile> servers = new ArrayList<>();
    }

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

            record.setServerKey(currentServerKey());

            record.setSourceName(consumeSource());
            record.setSourceKind(consumeKind());

            rememberServer(record.getServerKey());
            rememberServer(currentSessionServer().getKey());

            records.add(0, record);
            trim();
            save();
        }

        expireMacro();
    }

    /** Raw address of the server the client is on right now. */
    public static String currentServerKey() {
        return currentSessionServer().getKey();
    }

    /** Server of the session, with a placeholder when not connected. */
    public static ServerProfile currentSessionServer() {

        MinecraftClient client = MinecraftClient.getInstance();
        ServerInfo entry = client.getCurrentServerEntry();

        if (entry == null) {
            return new ServerProfile("", "", 0L);
        }

        String address = entry.address == null ? "" : entry.address;

        return new ServerProfile(
                address,
                address,
                0L
        );
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
            // must never resolve OUR pending record. Only entries of
            // the server we are on right now may be answered by it.
            if (foreignIssued ||
                    !record.getServerKey()
                            .equals(currentServerKey())) {

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

    // ------------------------------------------------------------------
    // servers
    // ------------------------------------------------------------------

    /** Makes sure a server entry exists for an address. */
    private void rememberServer(String key) {

        if (key == null || key.isBlank()) {
            return;
        }

        long now = System.currentTimeMillis();

        ServerProfile existing = servers.get(key);

        if (existing == null) {

            ServerProfile profile =
                    new ServerProfile(key, key, now);

            servers.put(key, profile);
        } else {
            existing.touch(now);
        }
    }

    /** Servers the log knows about, newest touched first. */
    public List<ServerProfile> getServers() {

        List<ServerProfile> list =
                new ArrayList<>(servers.values());

        list.sort((a, b) ->
                Long.compare(b.getLastSeen(), a.getLastSeen()));

        return list;
    }

    /** Display name of a server address, or the address itself. */
    public String serverName(String key) {

        ServerProfile profile = servers.get(key);

        return profile == null
                ? (key == null || key.isBlank() ? "—" : key)
                : profile.getName();
    }

    /** Renames a server; unknown addresses are created on the fly. */
    public void renameServer(String key, String name) {

        if (key == null || key.isBlank()) {
            return;
        }

        rememberServer(key);

        servers.get(key).setName(name);

        save();
    }

    /** Drops a server entry: the address itself is never lost. */
    public void forgetServer(String key) {

        if (key != null && servers.remove(key) != null) {
            save();
        }
    }

    // ------------------------------------------------------------------
    // reading the log
    // ------------------------------------------------------------------

    public List<PunishmentRecord> getRecords() {
        return List.copyOf(records);
    }

    /** The shared journal: every server together. */
    public List<PunishmentRecord> getRecordsOf() {
        return getRecords();
    }

    /** Only the records issued on one server. */
    public List<PunishmentRecord> getRecordsOf(String serverKey) {

        if (serverKey == null || serverKey.isBlank()) {
            return getRecords();
        }

        List<PunishmentRecord> out = new ArrayList<>();

        for (PunishmentRecord record : records) {

            if (serverKey.equals(record.getServerKey())) {
                out.add(record);
            }
        }

        return out;
    }

    public void clear() {
        records.clear();
        save();
    }

    /** Clears only one server's log, leaving the rest untouched. */
    public void clearServer(String serverKey) {

        if (serverKey == null || serverKey.isBlank()) {
            return;
        }

        records.removeIf(record ->
                serverKey.equals(record.getServerKey()));

        forgetServer(serverKey);

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
            String trimmed = json.trim();

            // files written before per-server history were a bare list
            if (trimmed.isEmpty()) {
                return;
            }

            if (trimmed.charAt(0) == '[') {

                List<PunishmentRecord> loaded =
                        GSON.fromJson(trimmed, LIST_TYPE);

                if (loaded != null) {

                    records.clear();
                    records.addAll(loaded);

                    for (PunishmentRecord record : records) {
                        rememberServer(record.getServerKey());
                    }

                    trim();
                }

                return;
            }

            Store store = GSON.fromJson(trimmed, Store.class);

            if (store == null) {
                return;
            }

            records.clear();

            if (store.records != null) {
                records.addAll(store.records);
            }

            servers.clear();

            if (store.servers != null) {
                for (ServerProfile profile : store.servers) {
                    if (profile.getKey() != null &&
                            !profile.getKey().isBlank()) {

                        servers.put(profile.getKey(), profile);
                    }
                }
            }

            for (PunishmentRecord record : records) {
                rememberServer(record.getServerKey());
            }

            trim();

        } catch (Exception e) {
            System.err.println(
                    "[StaffTools] Failed to load punishment history."
            );
        }
    }

    private void save() {
        try {

            Files.createDirectories(path.getParent());

            Store store = new Store();

            store.records = records;
            store.servers = new ArrayList<>(servers.values());

            Files.writeString(
                    path,
                    GSON.toJson(store, Store.class)
            );

        } catch (IOException e) {
            System.err.println(
                    "[StaffTools] Failed to save punishment history."
            );
        }
    }
}
