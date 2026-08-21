package com.uwdie.stafftools.client.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

import java.util.*;

public class PlayerResolver {
    private PlayerResolver() {
    }

    public static Optional<PlayerContext> resolve(String name) {

        MinecraftClient client = MinecraftClient.getInstance();

        if (client.getNetworkHandler() == null) {
            return Optional.empty();
        }

        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        for (PlayerListEntry entry :
                client.getNetworkHandler().getPlayerList()) {

            String playerName = entry.getProfile().getName();

            if (playerName.equalsIgnoreCase(name.trim())) {

                return Optional.of(
                        new PlayerContext(
                                playerName,
                                entry.getProfile().getId()
                        )
                );
            }
        }

        return Optional.empty();
    }

    /**
     * Finds a player whose TAB-LIST DISPLAY NAME appears in the given text.
     * Chat plugins often replace the visible nick with a custom display name
     * (prefixes, colors, placeholders) while keeping it in the tab list.
     */
    public static Optional<PlayerContext> resolveByDisplayName(String text) {

        MinecraftClient client = MinecraftClient.getInstance();

        if (client.getNetworkHandler() == null) {
            return Optional.empty();
        }

        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        for (PlayerListEntry entry :
                client.getNetworkHandler().getPlayerList()) {

            var displayName = entry.getDisplayName();

            if (displayName == null) {
                continue;
            }

            String dn = displayName.getString();

            if (dn == null || dn.isBlank() || dn.length() < 3) {
                continue;
            }

            if (text.contains(dn)) {

                return Optional.of(
                        new PlayerContext(
                                entry.getProfile().getName(),
                                entry.getProfile().getId()
                        )
                );
            }
        }

        return Optional.empty();
    }

    public static Optional<PlayerContext> resolveByUuid(UUID uuid) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.getNetworkHandler() == null) {
            return Optional.empty();
        }

        if (uuid == null) {
            return Optional.empty();
        }

        for (PlayerListEntry entry :
                client.getNetworkHandler().getPlayerList()) {

            if (uuid.equals(
                    entry.getProfile().getId()
            )) {

                return Optional.of(
                        new PlayerContext(
                                entry.getProfile().getName(),
                                uuid
                        )
                );
            }
        }

        return Optional.empty();
    }

    public static List<PlayerContext> findPlayers(String text) {

        MinecraftClient client = MinecraftClient.getInstance();

        if (client.getNetworkHandler() == null) {
            return List.of();
        }

        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<PlayerContext> result = new ArrayList<>();

        for (PlayerListEntry entry :
                client.getNetworkHandler().getPlayerList()) {

            String name = entry.getProfile().getName();

            if (containsName(text, name)) {

                result.add(
                        new PlayerContext(
                                name,
                                entry.getProfile().getId()
                        )
                );
            }
        }

        result.sort(
                Comparator.comparingInt(
                        (PlayerContext player) ->
                                player.name().length()
                ).reversed()
        );

        return result;
    }

    private static boolean containsName(
            String text,
            String name
    ) {

        String source = text.toLowerCase();
        String target = name.toLowerCase();

        int index = source.indexOf(target);

        while (index >= 0) {

            int end = index + target.length();

            boolean left =
                    index == 0 ||
                            !isNameCharacter(
                                    source.charAt(index - 1)
                            );

            boolean right =
                    end >= source.length() ||
                            !isNameCharacter(
                                    source.charAt(end)
                            );

            if (left && right) {
                return true;
            }

            index = source.indexOf(
                    target,
                    index + 1
            );
        }

        return false;
    }

    private static boolean isNameCharacter(char c) {
        return Character.isLetterOrDigit(c)
                || c == '_';
    }
}
