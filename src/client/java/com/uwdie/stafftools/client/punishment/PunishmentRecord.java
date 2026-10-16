package com.uwdie.stafftools.client.punishment;

/**
 * A single punishment entry recorded by the mod.
 */
public class PunishmentRecord {

    public static final long RESPONSE_TIMEOUT_MS = 30_000;

    private String playerName;
    private PunishmentType type;
    private String command;
    private long timestamp;
    private PunishmentStatus status;
    private String response;

    /** Raw address of the server the punishment was issued on. */
    private String serverKey = "";

    /** Local staff member who issued the punishment. */
    private String moderator;

    /** What produced the punishment: a macro, an action or a raw command. */
    private SourceKind sourceKind = SourceKind.MANUAL;

    /** Name of the macro or action the punishment came from. */
    private String sourceName;

    /** Where a punishment came from. */
    public enum SourceKind {

        /** Typed by hand. */
        MANUAL,

        /** Sent by one of the user's macros. */
        MACRO,

        /** Sent by one of the player-action entries. */
        ACTION
    }

    public PunishmentRecord() {
        this.type = PunishmentType.OTHER;
        this.status = PunishmentStatus.PENDING;
    }

    public PunishmentRecord(
            String playerName,
            PunishmentType type,
            String command
    ) {

        this.playerName = playerName;
        this.type = type != null ? type : PunishmentType.OTHER;
        this.command = command;
        this.timestamp = System.currentTimeMillis();
        this.status = PunishmentStatus.PENDING;
        this.response = null;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public PunishmentType getType() {
        return type;
    }

    public void setType(PunishmentType type) {
        this.type = type;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public PunishmentStatus getStatus() {
        // records written by an older build may carry no status at all
        return status == null
                ? PunishmentStatus.NO_RESPONSE
                : status;
    }

    public PunishmentStatus getEffectiveStatus() {

        if (status == null) {
            return PunishmentStatus.NO_RESPONSE;
        }

        if (status == PunishmentStatus.PENDING &&
                System.currentTimeMillis() - timestamp >
                        RESPONSE_TIMEOUT_MS) {

            return PunishmentStatus.NO_RESPONSE;
        }
        return status;
    }

    public void setStatus(PunishmentStatus status) {
        this.status = status != null
                ? status
                : PunishmentStatus.PENDING;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getResponse() {
        return response;
    }

    /** Raw address of the server the punishment was issued on. */
    public String getServerKey() {
        return serverKey == null || serverKey.isBlank()
                ? ""
                : serverKey;
    }

    public void setServerKey(String serverKey) {
        this.serverKey = serverKey == null ? "" : serverKey;
    }

    /** Local staff member who issued the punishment. */
    public String getModerator() {
        return moderator == null || moderator.isBlank()
                ? "—"
                : moderator;
    }

    public void setModerator(String moderator) {
        this.moderator = moderator;
    }

    /** Source of the punishment: a macro, an action or a raw command. */
    public SourceKind getSourceKind() {
        return sourceKind == null
                ? SourceKind.MANUAL
                : sourceKind;
    }

    public void setSourceKind(SourceKind sourceKind) {
        this.sourceKind = sourceKind == null
                ? SourceKind.MANUAL
                : sourceKind;
    }

    /**
     * Name of the macro or action that produced this punishment, or null
     * when it was typed by hand.
     */
    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    /** Name of the macro, kept for records written by older builds. */
    public String getMacroName() {
        return sourceName;
    }

    public void setMacroName(String macroName) {
        this.sourceName = macroName;

        // old builds only ever recorded macros
        if (macroName != null && !macroName.isBlank()) {
            this.sourceKind = SourceKind.MACRO;
        }
    }

    /** Command as it was sent, with the nick already substituted. */
    public String getFullCommand() {
        return command == null || command.isBlank()
                ? "—"
                : command;
    }

    /** Resolved staff member name, or null when unknown. */
    public String getModeratorOrNull() {
        return moderator == null || moderator.isBlank()
                ? null
                : moderator;
    }
}
