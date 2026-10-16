package com.uwdie.stafftools.client.punishment;

import java.util.UUID;

/**
 * One server the punishment history is kept for.
 *
 * A record only stores the raw address {@code key} it was issued on; the
 * display name is a nickname the user may give it ("Hypixel", "CubeCraft"),
 * so the history log can be split without remembering IP addresses.
 */
public class ServerProfile {

    private String id = UUID.randomUUID().toString();

    /** Raw server address, e.g. {@code mc.hypixel.net} or {@code 127.0.0.1}. */
    private String key;

    /** Display name, defaults to the address. */
    private String name;

    private long firstSeen;
    private long lastSeen;

    public ServerProfile() {
    }

    public ServerProfile(String key, String name, long at) {

        this.key = key == null ? "" : key;
        this.name = (name == null || name.isBlank())
                ? fallback(this.key)
                : name;

        this.firstSeen = at;
        this.lastSeen = at;
    }

    /** Address as the fallback display for singleplayer / missing names. */
    private static String fallback(String key) {
        return key == null || key.isBlank()
                ? "—"
                : key;
    }

    public String getId() {
        return id;
    }

    public String getKey() {
        return key == null ? "" : key;
    }

    public void setKey(String key) {
        this.key = key == null ? "" : key;
    }

    /** Display name, never blank. */
    public String getName() {
        return name == null || name.isBlank()
                ? fallback(key)
                : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getFirstSeen() {
        return firstSeen;
    }

    public void setFirstSeen(long firstSeen) {
        this.firstSeen = firstSeen;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    /** Short label for the sidebar: name, and the address when different. */
    public String label() {

        String display = getName();
        String address = getKey();

        return display.equals(address)
                ? display
                : display + " (" + address + ")";
    }

    public void touch(long at) {

        if (firstSeen <= 0L) {
            firstSeen = at;
        }

        lastSeen = at;
    }
}
