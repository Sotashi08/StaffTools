package com.uwdie.stafftools.client.audit;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * In-memory journal of everything the mod does.
 *
 * <p>Every subsystem reports here: which macro ran and which did not,
 * which nick was detected and which was not, how a punishment was
 * resolved, which setting changed. The audit screen reads the same
 * buffer, so the journal and the UI can never disagree.
 *
 * <p>The log is deliberately bounded and never persisted: it is a
 * debugging aid for the current session, not a permanent record. The
 * punishment history remains the durable log.
 */
public final class AuditLog {

    /** Entries kept before the oldest is dropped. */
    private static final int MAX_ENTRIES = 400;

    private static final AuditLog INSTANCE = new AuditLog();

    private final Deque<Entry> entries = new ArrayDeque<>();

    /** Total entries ever recorded, including the ones already dropped. */
    private long totalRecorded;

    private AuditLog() {
    }

    public static AuditLog get() {
        return INSTANCE;
    }

    /**
     * One journal line.
     *
     * @param at       wall-clock time of the event
     * @param category subsystem that produced it
     * @param level    severity
     * @param message  human readable text
     */
    public record Entry(
            long at,
            AuditCategory category,
            AuditLevel level,
            String message
    ) {
    }

    public void log(
            AuditCategory category,
            AuditLevel level,
            String message
    ) {

        if (category == null || level == null) {
            return;
        }

        if (!enabled()) {
            return;
        }

        synchronized (entries) {

            entries.addLast(
                    new Entry(
                            System.currentTimeMillis(),
                            category,
                            level,
                            message == null ? "" : message
                    )
            );

            while (entries.size() > MAX_ENTRIES) {
                entries.pollFirst();
            }

            totalRecorded++;
        }

        // persist to DB (permanent record; in-memory buffer stays for UI)
        com.uwdie.stafftools.client.db.Database db =
                com.uwdie.stafftools.client.db.Database.get();

        if (db != null) {
            db.insertAudit(
                    category.name(),
                    level.name(),
                    message == null ? "" : message);
        }

        // queue for batched DB write (flushed every few seconds)
        queueForDb(category, level, message == null ? "" : message);
    }

    /** Pending audit rows waiting to be flushed to the DB. */
    private static final java.util.List<String[]> pendingDb =
            new java.util.ArrayList<>();

    private static long lastFlushAt;

    private static final long FLUSH_INTERVAL_MS = 5000L;

    private static void queueForDb(
            AuditCategory category,
            AuditLevel level,
            String message
    ) {
        synchronized (pendingDb) {
            pendingDb.add(new String[]{
                    category.name(), level.name(), message});
        }
    }

    /** Writes queued audit rows to the DB. Call from the tick/render. */
    public static void flushDb() {

        long now = System.currentTimeMillis();

        if (now - lastFlushAt < FLUSH_INTERVAL_MS) {
            return;
        }

        lastFlushAt = now;

        com.uwdie.stafftools.client.db.Database db =
                com.uwdie.stafftools.client.db.Database.get();

        if (db == null) {
            return;
        }

        synchronized (pendingDb) {

            if (pendingDb.isEmpty()) {
                return;
            }

            for (String[] row : pendingDb) {
                db.insertAudit(row[0], row[1], row[2]);
            }

            pendingDb.clear();
        }
    }

    public void info(AuditCategory category, String message) {
        log(category, AuditLevel.INFO, message);
    }

    public void success(AuditCategory category, String message) {
        log(category, AuditLevel.SUCCESS, message);
    }

    public void warn(AuditCategory category, String message) {
        log(category, AuditLevel.WARN, message);
    }

    public void error(AuditCategory category, String message) {
        log(category, AuditLevel.ERROR, message);
    }

    /** Newest first, optionally filtered by category. */
    public List<Entry> snapshot(AuditCategory filter) {

        List<Entry> out = new ArrayList<>();

        synchronized (entries) {

            var it = entries.descendingIterator();

            while (it.hasNext()) {

                Entry entry = it.next();

                if (filter == null || entry.category() == filter) {
                    out.add(entry);
                }
            }
        }

        return out;
    }

    /** Number of entries currently held. */
    public int size() {

        synchronized (entries) {
            return entries.size();
        }
    }

    /** Entries recorded since the session started. */
    public long totalRecorded() {
        return totalRecorded;
    }

    public void clear() {

        synchronized (entries) {
            entries.clear();
        }
    }

    /**
     * The journal can be switched off in the settings; the check is
     * null-safe because the log is also written during early startup,
     * before the config exists.
     */
    private static boolean enabled() {

        try {
            var config = com.uwdie.stafftools.client.StafftoolsClient
                    .getConfig();

            return config == null || config.isAuditLogEnabled();
        } catch (RuntimeException notReadyYet) {
            return true;
        }
    }
}
