package com.uwdie.stafftools.client.config;

import com.uwdie.stafftools.client.macro.Macro;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One saved staff setup: its own macros, own player actions, own theme
 * and its own set of toggles.
 *
 * Profiles live inside {@link StaffToolsConfig}; exactly one of them is
 * active at a time and is mirrored into the live settings, so the rest
 * of the mod keeps reading a single flat config.
 */
public class Profile {

    private String id = UUID.randomUUID().toString();
    private String name = "Profile";
    private int themeIndex = 0;

    private ProfileSettings settings = new ProfileSettings();

    private List<Macro> macros = new ArrayList<>();

    private List<ActionEntry> actionEntries =
            ActionEntry.defaults();

    public Profile() {
    }

    /** Deep-ish copy: every nested object is cloned. */
    public Profile copy() {

        Profile clone = new Profile();

        clone.id = id;
        clone.name = name;
        clone.themeIndex = themeIndex;

        clone.macros = new ArrayList<>();

        for (Macro macro : macros) {
            clone.macros.add(macro.copy());
        }

        clone.actionEntries = new ArrayList<>();

        for (ActionEntry entry : actionEntries) {
            clone.actionEntries.add(entry.copy());
        }

        return clone;
    }

    /** Fresh id, used for duplicates and imported profiles. */
    public void assignNewId() {
        this.id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null || name.isBlank()
                ? "Profile"
                : name.trim();
    }

    public int getThemeIndex() {
        return themeIndex;
    }

    public void setThemeIndex(int index) {
        this.themeIndex = Math.max(0, index);
    }

    public ProfileSettings getSettings() {
        return settings;
    }

    public void setSettings(ProfileSettings settings) {
        this.settings = settings != null
                ? settings
                : new ProfileSettings();
    }

    public List<Macro> getMacros() {
        return macros != null ? macros : new ArrayList<>();
    }

    public void setMacros(List<Macro> macros) {
        this.macros = macros != null ? macros : new ArrayList<>();
    }

    public List<ActionEntry> getActionEntries() {
        return actionEntries != null
                ? actionEntries
                : ActionEntry.defaults();
    }

    public void setActionEntries(List<ActionEntry> entries) {
        this.actionEntries = entries != null ? entries : new ArrayList<>();
    }

    /** Short one-line summary used by the profile list UI. */
    public String summary() {
        return getMacros().size() + " / " + getActionEntries().size();
    }

    /** Snapshot of the live config into this profile. */
    public void capture(StaffToolsConfig config) {

        settings.readFrom(config);
        themeIndex = config.getThemeIndex();

        macros = new ArrayList<>();

        for (Macro macro : config.getMacros()) {
            macros.add(macro.copy());
        }

        actionEntries = new ArrayList<>();

        for (ActionEntry entry : config.getActionEntries()) {
            actionEntries.add(entry.copy());
        }
    }

    /** Pushes this profile into the live config (does NOT switch yet). */
    public void restore(StaffToolsConfig config) {

        settings.writeTo(config);

        config.setThemeIndex(themeIndex);

        config.setMacros(new ArrayList<>(getMacros()));

        config.setActionEntries(
                new ArrayList<>(getActionEntries())
        );
    }
}
