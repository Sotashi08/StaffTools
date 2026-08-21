package com.uwdie.stafftools.client.chat;

import com.uwdie.stafftools.client.player.PlayerContext;
import com.uwdie.stafftools.client.player.PlayerResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects player mentions in a chat string.
 *
 * Layer 1: direct case-insensitive matching of tab-list names.
 * Layer 2: same matching after stripping legacy color codes (§a, §l, ...)
 *          which many chat plugins embed directly into the JSON text,
 *          breaking naive matching ("§aPl§fayer" -> "Player").
 */
public final class PlayerMentionDetector {

    private PlayerMentionDetector() {
    }

    public static List<PlayerMention> detect(
            String message
    ) {

        if (message == null || message.isBlank()) {
            return List.of();
        }

        List<PlayerMention> mentions =
                findIn(message);

        if (!mentions.isEmpty()) {
            return mentions;
        }

        mentions = detectStripped(message);

        if (!mentions.isEmpty()) {
            return mentions;
        }

        return NickAliases.detect(message);
    }

    private static List<PlayerMention> findIn(
            String message
    ) {

        List<PlayerMention> mentions =
                new ArrayList<>();

        for (PlayerContext player :
                PlayerResolver.findPlayers(message)) {

            String name = player.name();

            String lower = message.toLowerCase();
            String target = name.toLowerCase();

            int index = lower.indexOf(target);

            while (index >= 0) {

                boolean left =
                        index == 0 ||
                                !isNameChar(lower.charAt(index - 1));

                boolean right =
                        index + target.length() >= lower.length() ||
                                !isNameChar(lower.charAt(index + target.length()));

                if (left && right) {

                    mentions.add(
                            new PlayerMention(
                                    player,
                                    index,
                                    index + name.length()
                            )
                    );
                }

                index = lower.indexOf(
                        target,
                        index + 1
                );
            }
        }

        return mentions;
    }

    /**
     * Strips legacy formatting codes ("§x") from the message while keeping
     * an index map back to the original string, then runs detection on the
     * clean text and converts the resulting ranges back.
     */
    private static List<PlayerMention> detectStripped(
            String message
    ) {

        StringBuilder clean = new StringBuilder();

        int[] map = new int[message.length()];
        int size = 0;

        for (int i = 0; i < message.length(); i++) {

            char c = message.charAt(i);

            if (c == '\u00A7' &&
                    i + 1 < message.length()) {

                i++;
                continue;
            }

            map[size++] = i;
            clean.append(c);
        }

        if (size == message.length()) {
            return List.of();
        }

        String cleaned = clean.toString();

        List<PlayerMention> stripped =
                findIn(cleaned);

        if (stripped.isEmpty()) {
            return List.of();
        }

        List<PlayerMention> result = new ArrayList<>();

        for (PlayerMention m : stripped) {

            result.add(
                    new PlayerMention(
                            m.player(),
                            map[m.start()],
                            map[m.end() - 1] + 1
                    )
            );
        }

        return result;
    }

    private static boolean isNameChar(char c) {
        return Character.isLetterOrDigit(c)
                || c == '_'
                || c == '\u00A7';
    }
}
