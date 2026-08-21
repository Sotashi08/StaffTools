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