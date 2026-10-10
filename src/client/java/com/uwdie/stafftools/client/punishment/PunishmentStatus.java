package com.uwdie.stafftools.client.punishment;

/**
 * Lifecycle of one recorded punishment.
 *
 * PENDING is only a starting point: it flips on its own to NO_RESPONSE
 * once the server stays silent past the timeout.
 */
public enum PunishmentStatus {

    /** Sent, no conclusive reply seen yet. */
    PENDING,

    /** The server confirmed the punishment. */
    DONE,

    /** The server rejected it (player not found, no permission, ...). */
    FAILED,

    /** Nothing came back within the timeout. */
    NO_RESPONSE
}
