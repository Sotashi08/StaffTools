package com.uwdie.stafftools.client.db;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.uwdie.stafftools.client.chat.NickCache;
import com.uwdie.stafftools.client.ui.Ui;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.List;
import java.util.Locale;

/**
 * {@code /stdb} — database management commands.
 *
 * <ul>
 *   <li>{@code /stdb list [page]} — list nicks stored in the DB
 *       (paginated, 20 per page).</li>
 *   <li>{@code /stdb status} — DB file size, row counts.</li>
 *   <li>{@code /stdb reload} — reload the nick cache from the DB.</li>
 *   <li>{@code /stdb clear} — wipe all nicks from the DB.</li>
 * </ul>
 */
public final class DbCommand {

    private static final int PAGE_SIZE = 20;

    private DbCommand() {
    }

    private static TextColor accent() {
        return TextColor.fromRgb(Ui.ACCENT);
    }

    private static TextColor text() {
        return TextColor.fromRgb(Ui.TEXT);
    }

    private static TextColor muted() {
        return TextColor.fromRgb(Ui.TEXT_MUTED);
    }

    public static void register() {

        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) ->
                        dispatcher.register(
                                ClientCommandManager.literal("stdb")
                                        .then(ClientCommandManager
                                                .literal("list")
                                                .executes(ctx -> {
                                                    listPage(0);
                                                    return 1;
                                                })
                                                .then(ClientCommandManager
                                                        .argument("page",
                                                                IntegerArgumentType
                                                                        .integer(0))
                                                        .executes(ctx -> {
                                                            listPage(
                                                                    IntegerArgumentType
                                                                            .getInteger(
                                                                                    ctx,
                                                                                    "page"));
                                                            return 1;
                                                        })))
                                        .then(ClientCommandManager
                                                .literal("status")
                                                .executes(ctx -> {
                                                    status();
                                                    return 1;
                                                }))
                                        .then(ClientCommandManager
                                                .literal("reload")
                                                .executes(ctx -> {
                                                    reload();
                                                    return 1;
                                                }))
                                        .then(ClientCommandManager
                                                .literal("hardsearch")
                                                .then(ClientCommandManager
                                                        .argument("text",
                                                                com.mojang.brigadier
                                                                        .arguments
                                                                        .StringArgumentType
                                                                        .greedyString())
                                                        .executes(ctx -> {
                                                            search(
                                                                    com.mojang.brigadier
                                                                            .arguments
                                                                            .StringArgumentType
                                                                            .getString(
                                                                                    ctx,
                                                                                    "text"));
                                                            return 1;
                                                        })))
                                        .then(ClientCommandManager
                                                .literal("search")
                                                .then(ClientCommandManager
                                                        .argument("text",
                                                                com.mojang.brigadier
                                                                        .arguments
                                                                        .StringArgumentType
                                                                        .greedyString())
                                                        .executes(ctx -> {
                                                            searchContains(
                                                                    com.mojang.brigadier
                                                                            .arguments
                                                                            .StringArgumentType
                                                                            .getString(
                                                                                    ctx,
                                                                                    "text"));
                                                            return 1;
                                                        })))
                                        .then(ClientCommandManager
                                                .literal("delete")
                                                .then(ClientCommandManager
                                                        .argument("nick",
                                                                com.mojang.brigadier
                                                                        .arguments
                                                                        .StringArgumentType
                                                                        .word())
                                                        .executes(ctx -> {
                                                            delete(
                                                                    com.mojang.brigadier
                                                                            .arguments
                                                                            .StringArgumentType
                                                                            .getString(
                                                                                    ctx,
                                                                                    "nick"));
                                                            return 1;
                                                        })))
                                        .then(ClientCommandManager
                                                .literal("export")
                                                .executes(ctx -> {
                                                    export();
                                                    return 1;
                                                }))
                                        .then(ClientCommandManager
                                                .literal("clearaudit")
                                                .executes(ctx -> {
                                                    clearAudit();
                                                    return 1;
                                                }))));
    }

    private static void listPage(int page) {

        MinecraftClient client = MinecraftClient.getInstance();

        if (client.inGameHud == null) {
            return;
        }

        Database db = Database.get();

        if (db == null) {
            msg("Database not initialized");
            return;
        }

        List<String> nicks = db.allNicks();

        int totalPages = Math.max(
                1, (nicks.size() + PAGE_SIZE - 1) / PAGE_SIZE);

        page = Math.clamp(page, 0, totalPages - 1);

        int from = page * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, nicks.size());

        header("Nicks (page " + (page + 1) + "/" + totalPages
                + ", total " + nicks.size() + ")");

        for (int i = from; i < to; i++) {
            String nick = nicks.get(i);
            NickCache.get().markIgnored(nick);
            client.inGameHud.getChatHud().addMessage(
                    Text.literal("  " + nick).setStyle(
                            Style.EMPTY.withColor(text())
                                    .withClickEvent(
                                            new net.minecraft.text.ClickEvent
                                                    .CopyToClipboard(
                                                    nick))));
        }

        if (nicks.isEmpty()) {
            msg("no nicks stored");
        }

        // single-line nav: only arrows clickable
        Text nav = Text.literal("[ ").setStyle(
                Style.EMPTY.withColor(muted()));

        if (page > 0) {
            nav = nav.copy().append(
                    Text.literal("<<").setStyle(
                            Style.EMPTY.withColor(accent())
                                    .withClickEvent(
                                            new net.minecraft.text.ClickEvent
                                                    .RunCommand(
                                                    "/stdb list "
                                                            + (page - 1)))));
        } else {
            nav = nav.copy().append(
                    Text.literal("<<").setStyle(
                            Style.EMPTY.withColor(muted())));
        }

        nav = nav.copy().append(
                Text.literal(" Page " + (page + 1) + "/" + totalPages
                        + " ").setStyle(
                        Style.EMPTY.withColor(muted())));

        if (page < totalPages - 1) {
            nav = nav.copy().append(
                    Text.literal(">>").setStyle(
                            Style.EMPTY.withColor(accent())
                                    .withClickEvent(
                                            new net.minecraft.text.ClickEvent
                                                    .RunCommand(
                                                    "/stdb list "
                                                            + (page + 1)))));
        } else {
            nav = nav.copy().append(
                    Text.literal(">>").setStyle(
                            Style.EMPTY.withColor(muted())));
        }

        nav = nav.copy().append(
                Text.literal(" ]").setStyle(
                        Style.EMPTY.withColor(muted())));

        client.inGameHud.getChatHud().addMessage(nav);
    }

    private static void status() {

        MinecraftClient client = MinecraftClient.getInstance();

        if (client.inGameHud == null) {
            return;
        }

        Database db = Database.get();

        if (db == null) {
            msg("Database not initialized");
            return;
        }

        var stats = db.stats();

        header("Database status");
        row("File size", formatBytes(stats.fileBytes()));
        row("Nicks", String.valueOf(stats.nickCount()));
        row("Punishments", String.valueOf(stats.punishmentCount()));
        row("Audit entries", String.valueOf(stats.auditCount()));
    }

    private static void reload() {

        MinecraftClient client = MinecraftClient.getInstance();

        if (client.inGameHud == null) {
            return;
        }

        // force a fresh load from DB
        NickCache.get().loadFromDatabase(true);

        msg("Nick cache reloaded from database");
    }

    private static void search(String text) {

        MinecraftClient client = MinecraftClient.getInstance();

        if (client.inGameHud == null) {
            return;
        }

        Database db = Database.get();

        if (db == null) {
            msg("Database not initialized");
            return;
        }

        var results = db.searchNicks(text);

        header("Search: " + text + " (" + results.size() + " found)");

        if (results.isEmpty()) {
            msg("no matches");
            return;
        }

        for (var r : results) {
            String nick = r.name();
            NickCache.get().markIgnored(nick);
            String line = "  " + nick;
            if (r.server() != null && !r.server().isBlank()) {
                line += " §8@ " + r.server();
            }
            client.inGameHud.getChatHud().addMessage(
                    Text.literal(line).setStyle(
                            Style.EMPTY.withColor(text())
                                    .withClickEvent(
                                            new net.minecraft.text.ClickEvent
                                                    .CopyToClipboard(
                                                    nick))));
        }
    }

    /** Contains-match search with pagination (like the old /stnicks). */
    private static void searchContains(String text) {

        MinecraftClient client = MinecraftClient.getInstance();

        if (client.inGameHud == null) {
            return;
        }

        Database db = Database.get();

        if (db == null) {
            msg("Database not initialized");
            return;
        }

        String lower = text.toLowerCase(Locale.ROOT);

        var all = db.allNicks().stream()
                .filter(n -> n.toLowerCase(Locale.ROOT).contains(lower))
                .sorted(String::compareToIgnoreCase)
                .toList();

        int totalPages = Math.max(
                1, (all.size() + PAGE_SIZE - 1) / PAGE_SIZE);

        header("Nicks containing " + text + " ("
                + all.size() + " found, page 1/" + totalPages + ")");

        int to = Math.min(PAGE_SIZE, all.size());

        for (int i = 0; i < to; i++) {
            String nick = all.get(i);
            NickCache.get().markIgnored(nick);
            client.inGameHud.getChatHud().addMessage(
                    Text.literal("  " + nick).setStyle(
                            Style.EMPTY.withColor(text())
                                    .withClickEvent(
                                            new net.minecraft.text.ClickEvent
                                                    .CopyToClipboard(
                                                    nick))));
        }

        if (all.isEmpty()) {
            msg("no matches");
        }
    }

    private static void delete(String nick) {

        MinecraftClient client = MinecraftClient.getInstance();

        if (client.inGameHud == null) {
            return;
        }

        Database db = Database.get();

        if (db == null) {
            msg("Database not initialized");
            return;
        }

        boolean removed = db.deleteNick(nick);

        if (removed) {
            msg("Deleted nick: " + nick);
        } else {
            msg("Nick not found: " + nick);
        }
    }

    private static void export() {

        MinecraftClient client = MinecraftClient.getInstance();

        if (client.inGameHud == null) {
            return;
        }

        Database db = Database.get();

        if (db == null) {
            msg("Database not initialized");
            return;
        }

        try {
            var dir = net.fabricmc.loader.api.FabricLoader
                    .getInstance().getConfigDir();

            // export nicks
            var nicks = db.allNicks();
            var nicksCsv = new StringBuilder("nick\n");
            for (String n : nicks) nicksCsv.append(n).append('\n');
            java.nio.file.Files.writeString(
                    dir.resolve("stafftools_export_nicks.csv"),
                    nicksCsv.toString());

            // export punishments
            var punishCsv = new StringBuilder(
                    "id,player,type,command,moderator,status,server,created_at\n");
            try (var st = db.conn().createStatement();
                    var rs = st.executeQuery(
                            "SELECT id,player,type,command,moderator,"
                                    + "status,server,created_at "
                                    + "FROM punishments ORDER BY created_at")) {
                while (rs.next()) {
                    punishCsv.append(rs.getInt(1)).append(',')
                            .append(csv(rs.getString(2))).append(',')
                            .append(csv(rs.getString(3))).append(',')
                            .append(csv(rs.getString(4))).append(',')
                            .append(csv(rs.getString(5))).append(',')
                            .append(csv(rs.getString(6))).append(',')
                            .append(csv(rs.getString(7))).append(',')
                            .append(rs.getLong(8)).append('\n');
                }
            }
            java.nio.file.Files.writeString(
                    dir.resolve("stafftools_export_punishments.csv"),
                    punishCsv.toString());

            // export audit
            var auditCsv = new StringBuilder(
                    "id,category,level,message,created_at\n");
            try (var st = db.conn().createStatement();
                    var rs = st.executeQuery(
                            "SELECT id,category,level,message,created_at "
                                    + "FROM audit ORDER BY created_at")) {
                while (rs.next()) {
                    auditCsv.append(rs.getInt(1)).append(',')
                            .append(csv(rs.getString(2))).append(',')
                            .append(csv(rs.getString(3))).append(',')
                            .append(csv(rs.getString(4))).append(',')
                            .append(rs.getLong(5)).append('\n');
                }
            }
            java.nio.file.Files.writeString(
                    dir.resolve("stafftools_export_audit.csv"),
                    auditCsv.toString());

            msg("Exported full DB to 3 CSV files in config/");

        } catch (Exception e) {
            msg("Export failed: " + e.getMessage());
        }
    }

    private static String csv(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private static void clearAudit() {

        MinecraftClient client = MinecraftClient.getInstance();

        if (client.inGameHud == null) {
            return;
        }

        Database db = Database.get();

        if (db == null) {
            msg("Database not initialized");
            return;
        }

        try (var st = db.conn().createStatement()) {
            st.execute("DELETE FROM audit");
        } catch (Exception e) {
            msg("Clear audit failed: " + e.getMessage());
            return;
        }

        msg("Audit log cleared");
    }

    // ---- helpers ----

    private static void header(String s) {
        MinecraftClient.getInstance().inGameHud.getChatHud()
                .addMessage(Text.literal("StaffTools ")
                        .setStyle(Style.EMPTY.withColor(muted()))
                        .copy().append(
                                Text.literal("— " + s).setStyle(
                                        Style.EMPTY.withColor(accent()))));
    }

    private static void row(String label, String value) {
        MinecraftClient.getInstance().inGameHud.getChatHud()
                .addMessage(Text.literal("  " + label + ": ")
                        .setStyle(Style.EMPTY.withColor(muted()))
                        .copy().append(
                                Text.literal(value).setStyle(
                                        Style.EMPTY.withColor(text()))));
    }

    private static void msg(String s) {
        MinecraftClient.getInstance().inGameHud.getChatHud()
                .addMessage(Text.literal(s).setStyle(
                        Style.EMPTY.withColor(muted())));
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}