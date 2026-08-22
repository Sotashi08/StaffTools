package com.uwdie.stafftools.client.macro;

import com.uwdie.stafftools.client.player.PlayerContext;
import net.minecraft.client.MinecraftClient;

import java.util.List;

public final class MacroExecutor {

    private MacroExecutor() {
    }

    public static void execute(
            Macro macro,
            PlayerContext target
    ) {

        if (macro == null ||
                !macro.isEnabled()) {

            return;
        }

        executeCommands(
                macro.getCommands(),
                target
        );
    }

    /**
     * True if any of the macro's commands contain the {@code <alias>}
     * placeholder and therefore need a user-picked variant.
     */
    public static boolean requiresAlias(Macro macro) {

        if (macro == null) {
            return false;
        }

        for (String command : macro.getCommands()) {

            if (command != null &&
                    command.contains("<alias>")) {

                return true;
            }
        }

        return false;
    }

    /**
     * Executes the macro with {@code <alias>} replaced by the picked
     * variant BEFORE regular placeholder resolution. An empty alias is
     * allowed; leftover double spaces are collapsed.
     */
    public static void executeWithAlias(
            Macro macro,
            PlayerContext target,
            String alias
    ) {

        if (macro == null ||
                !macro.isEnabled()) {

            return;
        }

        String safeAlias =
                alias == null ? "" : alias.trim();

        List<String> raw = new java.util.ArrayList<>();

        for (String command : macro.getCommands()) {

            if (command == null) {
                continue;
            }

            String resolved = command
                    .replace("<alias>", safeAlias)
                    .replaceAll(" {2,}", " ")
                    .trim();

            raw.add(resolved);
        }

        executeCommands(raw, target);
    }

    public static void executeCommands(
            List<String> rawCommands,
            PlayerContext target
    ) {

        MinecraftClient client =
                MinecraftClient.getInstance();

        if (client.player == null ||
                client.getNetworkHandler() == null) {

            return;
        }

        MacroContext context =
                MacroContext.create(target);

        for (String raw :
                rawCommands) {

            if (raw == null ||
                    raw.isBlank()) {

                continue;
            }

            String command =
                    PlaceholderEngine.resolve(
                            raw,
                            context
                    ).trim();

            if (command.isEmpty()) {
                continue;
            }

            if (command.startsWith("/")) {

                command =
                        command.substring(1);
            }

            client.getNetworkHandler()
                    .sendChatCommand(command);
        }
    }
}