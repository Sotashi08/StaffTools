package com.uwdie.stafftools.client.audit;

/**
 * Groups audit entries by the subsystem that produced them, so the
 * audit screen can filter the journal without parsing the message.
 */
public enum AuditCategory {

    /** Macro / action execution. */
    MACRO("macro", 0xFF7DBCFF),

    /** Nick detection layers and their outcomes. */
    DETECT("detect", 0xFF55DD77),

    /** Punishment history and server replies. */
    PUNISH("punish", 0xFFEEBB55),

    /** Configuration, profiles and themes. */
    CONFIG("config", 0xFFC3A3FF),

    /** Chat interaction: clicks, copies, popups. */
    CHAT("chat", 0xFF8FE8DB),

    /** Internal errors and rejected operations. */
    ERROR("error", 0xFFFF5555),

    /** Database lifecycle: init, migration, inserts. */
    DB("db", 0xFF5EC8F0);

    private final String id;
    private final int color;

    AuditCategory(String id, int color) {
        this.id = id;
        this.color = color;
    }

    /** Stable identifier used by the filter buttons. */
    public String id() {
        return id;
    }

    /** Accent colour of the category badge. */
    public int color() {
        return color;
    }
}
