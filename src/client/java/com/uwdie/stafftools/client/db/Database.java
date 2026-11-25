package com.uwdie.stafftools.client.db;

import com.uwdie.stafftools.client.audit.AuditCategory;
import com.uwdie.stafftools.client.audit.AuditLog;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Embedded SQLite storage for nicks, punishments and the audit log.
 *
 * <p>One file ({@code config/stafftools.db}) holds everything. The
 * connection is single-threaded (the game client is single-threaded for
 * these paths); all writes go through {@link #get()} on the render/tick
 * thread. A small in-memory cache in front of the nick table keeps the
 * hot detection path fast.
 */
public final class Database {

    private static Database instance;

    private final Path path;
    private Connection connection;

    private Database(Path path) {
        this.path = path;
    }

    public static void init(Path path) {
        instance = new Database(path);
        instance.open();
        instance.migrate();

        AuditLog.get().info(
                AuditCategory.DB,
                "Database initialized: " + path.getFileName());
    }

    public static Database get() {
        return instance;
    }

    private void open() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + path);
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL;");
                st.execute("PRAGMA synchronous=NORMAL;");
                st.execute("PRAGMA foreign_keys=ON;");
            }
        } catch (Exception e) {
            System.err.println("[StaffTools] Failed to open database.");
            e.printStackTrace();
        }
    }

    private void migrate() {
        try (Statement st = connection.createStatement()) {

            st.execute("""
                    CREATE TABLE IF NOT EXISTS nicks (
                        name TEXT PRIMARY KEY,
                        uuid TEXT,
                        first_seen INTEGER NOT NULL,
                        last_seen INTEGER NOT NULL,
                        server TEXT
                    )""");

            st.execute("""
                    CREATE TABLE IF NOT EXISTS punishments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        player TEXT NOT NULL,
                        type TEXT NOT NULL,
                        command TEXT,
                        moderator TEXT,
                        status TEXT NOT NULL,
                        server TEXT,
                        source_name TEXT,
                        source_kind TEXT,
                        reply TEXT,
                        created_at INTEGER NOT NULL
                    )""");

            st.execute("""
                    CREATE TABLE IF NOT EXISTS audit (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        category TEXT NOT NULL,
                        level TEXT NOT NULL,
                        message TEXT NOT NULL,
                        created_at INTEGER NOT NULL
                    )""");

            st.execute(
                    "CREATE INDEX IF NOT EXISTS idx_punish_player "
                            + "ON punishments(player)");
            st.execute(
                    "CREATE INDEX IF NOT EXISTS idx_punish_created "
                            + "ON punishments(created_at)");
            st.execute(
                    "CREATE INDEX IF NOT EXISTS idx_audit_created "
                            + "ON audit(created_at)");

            // FTS5 virtual table for fast nick search
            st.execute("""
                    CREATE VIRTUAL TABLE IF NOT EXISTS nicks_fts
                    USING fts5(name, content='nicks', content_rowid='rowid')""");

            // keep FTS in sync with nicks via triggers
            st.execute("""
                    CREATE TRIGGER IF NOT EXISTS nicks_ai AFTER INSERT ON nicks BEGIN
                        INSERT INTO nicks_fts(rowid, name)
                        VALUES (new.rowid, new.name);
                    END""");
            st.execute("""
                    CREATE TRIGGER IF NOT EXISTS nicks_ad AFTER DELETE ON nicks BEGIN
                        INSERT INTO nicks_fts(nicks_fts, rowid, name)
                        VALUES('delete', old.rowid, old.name);
                    END""");

        } catch (Exception e) {
            System.err.println("[StaffTools] Database migration failed.");
            e.printStackTrace();
        }
    }

    public Connection conn() {
        return connection;
    }

    // ------------------------------------------------------------------
    // nicks
    // ------------------------------------------------------------------

    public void upsertNick(
            String name,
            String uuid,
            String server
    ) {

        if (name == null || name.isBlank()) {
            return;
        }

        long now = System.currentTimeMillis();

        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO nicks(name, uuid, first_seen, last_seen, server) "
                        + "VALUES(?,?,?,?,?) "
                        + "ON CONFLICT(name) DO UPDATE SET "
                        + "last_seen=excluded.last_seen, "
                        + "uuid=COALESCE(excluded.uuid, nicks.uuid), "
                        + "server=COALESCE(excluded.server, nicks.server)")) {

            ps.setString(1, name);
            ps.setString(2, uuid);
            ps.setLong(3, now);
            ps.setLong(4, now);
            ps.setString(5, server);
            ps.executeUpdate();

            AuditLog.get().info(
                    AuditCategory.DB,
                    "Nick saved: " + name);

        } catch (SQLException e) {
            AuditLog.get().error(
                    AuditCategory.ERROR,
                    "DB nick upsert failed: " + e.getMessage());
        }
    }

    /** All nicks ordered by last_seen desc, for autocomplete. */
    public List<String> allNicks() {

        List<String> out = new ArrayList<>();

        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery(
                        "SELECT name FROM nicks "
                                + "ORDER BY last_seen DESC")) {

            while (rs.next()) {
                out.add(rs.getString(1));
            }

        } catch (SQLException e) {
            AuditLog.get().error(
                    AuditCategory.ERROR,
                    "DB nick query failed: " + e.getMessage());
        }

        return out;
    }

    /** Total nick count in the database. */
    public int countNicks() {

        try (Statement st = connection.createStatement();
                ResultSet rs = st.executeQuery(
                        "SELECT COUNT(*) FROM nicks")) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException ignored) {
        }

        return 0;
    }

    /** One nick row for search results. */
    public record NickRow(
            String name,
            String server,
            long lastSeen
    ) {
    }

    /** Search nicks by substring (contains), returns matching rows. */
    public List<NickRow> searchNicks(String query) {

        List<NickRow> out = new ArrayList<>();

        if (query == null || query.isBlank()) {
            return out;
        }

        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT name, server, last_seen FROM nicks "
                        + "WHERE name LIKE ? COLLATE NOCASE "
                        + "ORDER BY last_seen DESC LIMIT 50")) {

            ps.setString(1, "%" + query + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new NickRow(
                            rs.getString(1),
                            rs.getString(2),
                            rs.getLong(3)));
                }
            }

        } catch (SQLException e) {
            AuditLog.get().error(
                    AuditCategory.ERROR,
                    "DB search failed: " + e.getMessage());
        }

        return out;
    }

    /** Deletes a nick from the database. Returns true if removed. */
    public boolean deleteNick(String name) {

        if (name == null || name.isBlank()) {
            return false;
        }

        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM nicks WHERE name = ? COLLATE NOCASE")) {
            ps.setString(1, name);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            AuditLog.get().error(
                    AuditCategory.ERROR,
                    "DB delete failed: " + e.getMessage());
        }

        return false;
    }

    // ------------------------------------------------------------------
    // punishments
    // ------------------------------------------------------------------

    public int insertPunishment(
            String player,
            String type,
            String command,
            String moderator,
            String status,
            String server,
            String sourceName,
            String sourceKind,
            String reply
    ) {

        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO punishments(player, type, command, moderator, "
                        + "status, server, source_name, source_kind, reply, "
                        + "created_at) VALUES(?,?,?,?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, player);
            ps.setString(2, type);
            ps.setString(3, command);
            ps.setString(4, moderator);
            ps.setString(5, status);
            ps.setString(6, server);
            ps.setString(7, sourceName);
            ps.setString(8, sourceKind);
            ps.setString(9, reply);
            ps.setLong(10, System.currentTimeMillis());
            ps.executeUpdate();

            try (var keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    AuditLog.get().info(
                            AuditCategory.DB,
                            "Punishment saved #" + id + ": " + player);
                    return id;
                }
            }

        } catch (SQLException e) {
            AuditLog.get().error(
                    AuditCategory.ERROR,
                    "DB punishment insert failed: " + e.getMessage());
        }

        return -1;
    }

    public void updatePunishmentStatus(int id, String status, String reply) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE punishments SET status=?, reply=? WHERE id=?")) {
            ps.setString(1, status);
            ps.setString(2, reply);
            ps.setInt(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            AuditLog.get().error(
                    AuditCategory.ERROR,
                    "DB punishment update failed: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // audit
    // ------------------------------------------------------------------

    public void insertAudit(
            String category,
            String level,
            String message
    ) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO audit(category, level, message, created_at) "
                        + "VALUES(?,?,?,?)")) {
            ps.setString(1, category);
            ps.setString(2, level);
            ps.setString(3, message);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println(
                    "[StaffTools] DB audit insert failed: "
                            + e.getMessage());
        }
    }

    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException ignored) {
        }
    }

    /** Snapshot of database stats for the audit screen. */
    public record DbStats(
            long fileBytes,
            int nickCount,
            int punishmentCount,
            int auditCount
    ) {
    }

    public DbStats stats() {

        long bytes = 0;

        try {
            if (java.nio.file.Files.exists(path)) {
                bytes = java.nio.file.Files.size(path);
            }
        } catch (Exception ignored) {
        }

        int nicks = 0;
        int punishments = 0;
        int audit = 0;

        try (Statement st = connection.createStatement()) {

            try (var rs = st.executeQuery(
                    "SELECT COUNT(*) FROM nicks")) {
                if (rs.next()) nicks = rs.getInt(1);
            }
            try (var rs = st.executeQuery(
                    "SELECT COUNT(*) FROM punishments")) {
                if (rs.next()) punishments = rs.getInt(1);
            }
            try (var rs = st.executeQuery(
                    "SELECT COUNT(*) FROM audit")) {
                if (rs.next()) audit = rs.getInt(1);
            }

        } catch (SQLException ignored) {
        }

        return new DbStats(bytes, nicks, punishments, audit);
    }
}