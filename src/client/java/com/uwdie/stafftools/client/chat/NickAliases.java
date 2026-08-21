package com.uwdie.stafftools.client.chat;

import com.uwdie.stafftools.client.player.PlayerContext;
import com.uwdie.stafftools.client.player.PlayerResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Learned mappings "custom displayed nick -> real player".
 *
 * When the mod identifies a player through hover/click events or a
 * display-name match, the surrounding visible tokens are remembered as
 * aliases. Later these aliases are detected in ANY chat text (including
 * what the user types), even on servers where the real nick is never shown.
 */
public final class NickAliases {

    private static final Map<String, PlayerContext> ALIASES =
            new ConcurrentHashMap<>();

    private static final int MAX_ALIASES = 500;

    private NickAliases() {
    }

    /**
     * Learns alias tokens from a short (likely name-only) text component
     * once its real player is known through another channel.
     */
    public static void learn(
            String visibleText,
            PlayerContext player
    ) {

        if (visibleText == null ||
                visibleText.isBlank() ||
                player == null) {

            return;
        }

        if (visibleText.length() > 24) {
            return;
        }

        for (String token :
                visibleText.split("[^A-Za-z0-9_]")) {

            if (token.length() < 3 || token.length() > 16) {
                continue;
            }

            String lower = token.toLowerCase();

            if (lower.equalsIgnoreCase(player.name())) {
                continue;
            }

            if (PlayerResolver.resolve(token).isPresent()) {
                continue;
            }

            if (ALIASES.size() >= MAX_ALIASES &&
                    !ALIASES.containsKey(lower)) {

                continue;
            }

            ALIASES.put(lower, player);
        }
    }

    /** Finds learned alias occurrences in arbitrary text. */
    public static List<PlayerMention> detect(String message) {

        List<PlayerMention> result = new ArrayList<>();

        if (message == null || message.isBlank()) {
            return result;
        }

        String lower = message.toLowerCase();

        for (Map.Entry<String, PlayerContext> e :
                ALIASES.entrySet()) {

            String alias = e.getKey();

            int index = lower.indexOf(alias);

            while (index >= 0) {

                boolean left =
                        index == 0 ||
                                !isNameChar(lower.charAt(index - 1));

                boolean right =
                        index + alias.length() >= lower.length() ||
                                !isNameChar(
                                        lower.charAt(index + alias.length())
                                );

                if (left && right) {

                    result.add(
                            new PlayerMention(
                                    e.getValue(),
                                    index,
                                    index + alias.length()
                            )
                    );
                }

                index = lower.indexOf(alias, index + 1);
            }
        }

        return result;
    }

    private static boolean isNameChar(char c) {
        return Character.isLetterOrDigit(c)
                || c == '_';
    }
}
