package com.uwdie.stafftools.client.macro;

import com.uwdie.stafftools.client.player.PlayerContext;
import net.minecraft.client.MinecraftClient;

import java.util.LinkedHashMap;
import java.util.Map;

public class MacroContext {

    private final PlayerContext player;
    private final Map<String, String> variables;

    private MacroContext(
            PlayerContext player,
            Map<String, String> variables
    ) {

        this.player = player;
        this.variables =
                new LinkedHashMap<>(variables);
    }

    public static MacroContext create(
            PlayerContext target
    ) {

        MinecraftClient client =
                MinecraftClient.getInstance();

        Map<String, String> variables =
                new LinkedHashMap<>();

        if (target != null) {

            variables.put(
                    "player",
                    target.name()
            );

            variables.put(
                    "ping",
                    resolvePing(client, target)
            );
        }

        if (client.player != null) {

            variables.put(
                    "staff",
                    client.player
                            .getName()
                            .getString()
            );

            var blockPos =
                    client.player.getBlockPos();

            variables.put(
                    "x",
                    Integer.toString(blockPos.getX())
            );

            variables.put(
                    "y",
                    Integer.toString(blockPos.getY())
            );

            variables.put(
                    "z",
                    Integer.toString(blockPos.getZ())
            );
        }

        if (client.getCurrentServerEntry() != null) {

            String address =
                    client.getCurrentServerEntry().address;

            if (address != null) {
                variables.put("server", address);
            }
        }

        return new MacroContext(
                target,
                variables
        );
    }

    private static String resolvePing(
            MinecraftClient client,
            PlayerContext target
    ) {

        if (client.getNetworkHandler() == null) {
            return "";
        }

        for (var entry :
                client.getNetworkHandler().getPlayerList()) {

            if (target.uuid()
                    .equals(entry.getProfile().getId())) {

                return Integer.toString(
                        entry.getLatency()
                );
            }
        }

        return "";
    }

    public String get(
            String name
    ) {

        return variables.getOrDefault(
                name,
                ""
        );
    }
}
