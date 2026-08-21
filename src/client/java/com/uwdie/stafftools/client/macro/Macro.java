package com.uwdie.stafftools.client.macro;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Macro {

    private UUID id;
    private String name;
    private String description;

    private List<String> commands;

    private List<String> aliases;

    private boolean enabled;
    private boolean dangerous;
    private boolean confirmationRequired;

    public Macro(
            UUID id,
            String name,
            String description,
            List<String> commands
    ) {

        this.id = id;
        this.name = name;
        this.description = description;

        this.commands =
                new ArrayList<>(commands);

        this.aliases =
                new ArrayList<>();

        this.enabled = true;
        this.dangerous = false;
        this.confirmationRequired = false;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getCommands() {
        return List.copyOf(commands);
    }

    public List<String> getAliases() {
        return List.copyOf(aliases);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isDangerous() {
        return dangerous;
    }

    public boolean isConfirmationRequired() {
        return confirmationRequired;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCommands(
            List<String> commands
    ) {

        this.commands =
                new ArrayList<>(commands);
    }

    public void setAliases(
            List<String> aliases
    ) {

        this.aliases =
                new ArrayList<>(aliases);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setDangerous(boolean dangerous) {
        this.dangerous = dangerous;
    }

    public void setConfirmationRequired(
            boolean confirmationRequired
    ) {

        this.confirmationRequired =
                confirmationRequired;
    }

}

