package com.uwdie.stafftools.client.config;

import java.util.ArrayList;
import java.util.List;

public class ActionEntry {

    private String icon;
    private String label;
    private String command;
    private boolean copyName;
    private boolean enabled;
    private boolean dangerous;
    private boolean confirmationRequired;

    public ActionEntry() {
        this.icon = "";
        this.label = "New action";
        this.command = "";
        this.copyName = false;
        this.enabled = true;
        this.dangerous = false;
        this.confirmationRequired = false;
    }

    public ActionEntry(
            String icon,
            String label,
            String command,
            boolean copyName,
            boolean enabled,
            boolean dangerous,
            boolean confirmationRequired
    ) {

        this.icon = icon;
        this.label = label;
        this.command = command;
        this.copyName = copyName;
        this.enabled = enabled;
        this.dangerous = dangerous;
        this.confirmationRequired = confirmationRequired;
    }

    public static List<ActionEntry> defaults() {

        List<ActionEntry> list = new ArrayList<>();

        list.add(new ActionEntry(
                "🔇", "Mute", "/mute <player> 10m",
                false, true, true, true
        ));

        list.add(new ActionEntry(
                "🚫", "Ban", "/ban <player>",
                false, true, true, true
        ));

        list.add(new ActionEntry(
                "⚠", "Warn", "/warn <player>",
                false, true, false, false
        ));

        list.add(new ActionEntry(
                "👢", "Kick", "/kick <player>",
                false, true, true, true
        ));

        list.add(new ActionEntry(
                "📋", "Copy name", "",
                true, true, false, false
        ));

        return list;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon != null ? icon : "";
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label != null ? label : "";
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command != null ? command : "";
    }

    public boolean isCopyName() {
        return copyName;
    }

    public void setCopyName(boolean copyName) {
        this.copyName = copyName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDangerous() {
        return dangerous;
    }

    public void setDangerous(boolean dangerous) {
        this.dangerous = dangerous;
    }

    public boolean isConfirmationRequired() {
        return confirmationRequired;
    }

    public void setConfirmationRequired(boolean confirmationRequired) {
        this.confirmationRequired = confirmationRequired;
    }
}
