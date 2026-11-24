package com.uwdie.stafftools.client.audit;

/**
 * Severity of an audit entry. The level drives the colour of the row
 * and lets the audit screen hide the noise when only problems matter.
 */
public enum AuditLevel {

    /** Routine information: a macro ran, a nick was detected. */
    INFO(0xFFB0B0B0),

    /** Something succeeded that the user asked for. */
    SUCCESS(0xFF55DD77),

    /** Something was skipped or produced no result. */
    WARN(0xFFEEBB55),

    /** A failure: validation rejected a command, a reply never came. */
    ERROR(0xFFFF5555);

    private final int color;

    AuditLevel(int color) {
        this.color = color;
    }

    public int color() {
        return color;
    }
}
