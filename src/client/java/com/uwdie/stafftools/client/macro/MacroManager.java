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

    /**
     * Replaces the macro order with the given list. Used by the
     * drag-to-reorder list; unknown ids are ignored so a stale drag can
     * never resurrect a deleted macro.
     */
    public void reorder(List<Macro> ordered) {

        if (ordered == null || ordered.isEmpty()) {
            return;
        }

        List<Macro> current = config.getMacros();

        List<Macro> next = new java.util.ArrayList<>();

        for (Macro macro : ordered) {

            for (Macro existing : current) {

                if (existing.getId().equals(macro.getId())) {
                    next.add(existing);
                    break;
                }
            }
        }

        // keep any macro the caller did not know about
        for (Macro existing : current) {

            boolean present = next.stream().anyMatch(
                    m -> m.getId().equals(existing.getId())
            );

            if (!present) {
                next.add(existing);
            }
        }

        config.setMacros(next);

        configManager.save();
    }

    public List<Macro> getMacros() {
        return List.copyOf(config.getMacros());
    }
}
