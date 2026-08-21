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
        return status;
    }

    public PunishmentStatus getEffectiveStatus() {
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
}
