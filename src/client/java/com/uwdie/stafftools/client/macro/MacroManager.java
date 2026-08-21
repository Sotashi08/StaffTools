package com.uwdie.stafftools.client.macro;

import com.uwdie.stafftools.client.config.ConfigManager;
import com.uwdie.stafftools.client.config.StaffToolsConfig;

import java.util.List;
import java.util.UUID;

public class MacroManager {
    private final StaffToolsConfig config;
    private final ConfigManager configManager;

    public MacroManager(
            StaffToolsConfig config,
            ConfigManager configManager
    ) {

        this.config = config;
        this.configManager = configManager;
    }

    public void initialize() {

        if (config.getMacros().isEmpty()) {
            createDefaults();
            configManager.save();
        }
    }

    private void createDefaults() {

        Macro day =
                new Macro(
                        UUID.randomUUID(),
                        "Day",
                        "Set the world time to day.",
                        List.of(
                                "/time set day"
                        )
                );

        Macro night =
                new Macro(
                        UUID.randomUUID(),
                        "Night",
                        "Set the world time to night.",
                        List.of(
                                "/time set night"
                        )
                );

        Macro teleport =
                new Macro(
                        UUID.randomUUID(),
                        "Teleport",
                        "Teleport to selected player.",
                        List.of(
                                "/tp <player>"
                        )
                );

        Macro mute =
                new Macro(
                        UUID.randomUUID(),
                        "Mute",
                        "Mute selected player.",
                        List.of(
                                "/mute <player> 10m"
                        )
                );

        mute.setDangerous(true);
        mute.setConfirmationRequired(true);

        register(day);
        register(night);
        register(teleport);
        register(mute);
    }

    public void register(
            Macro macro
    ) {

        config.getMacros().removeIf(
                existing ->
                        existing.getId()
                                .equals(macro.getId())
        );

        config.getMacros().add(macro);

        configManager.save();
    }

    public void remove(
            UUID id
    ) {

        config.getMacros().removeIf(
                macro ->
                        macro.getId().equals(id)
        );

        configManager.save();
    }

    public List<Macro> getMacros() {
        return List.copyOf(config.getMacros());
    }
}
