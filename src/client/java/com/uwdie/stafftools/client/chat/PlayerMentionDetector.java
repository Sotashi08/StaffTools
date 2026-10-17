package com.uwdie.stafftools.client.chat;

import com.uwdie.stafftools.client.StafftoolsClient;
import com.uwdie.stafftools.client.config.StaffToolsConfig;
import com.uwdie.stafftools.client.player.PlayerContext;
import com.uwdie.stafftools.client.player.PlayerResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects player mentions inside a plain chat string.
 *
 * Layers, in order (each one can be switched off in the detection menu):
 * 1. real nicks from the tab list,
 * 2. nicks from the local cache of everyone seen this session,
 * 3. the same two after folding look-alike unicode
 *    ("\u1D04ᴀᴘꜱ" -> "caps"),
 * 4. the same two after replacing decorations with spaces
 *    ("[VIP]Notch\u2605" -> " VIP Notch "),
 * 5. the same two after stripping legacy colour codes
 *    ("\u00A7aPl\u00A7fayer" -> "Player"),
 * 6. aliases learned from click / hover events,
 * 7. nicks learned from a /nick change (EssentialsX), where the tab list
 *    shows the new name while the real one is nowhere in the text.
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

        StaffToolsConfig config =
                StafftoolsClient.getConfig();

        List<PlayerMention> mentions = findIn(message);

        if (!mentions.isEmpty()) {
            return mentions;
        }

        // a player who changed their nick is only known by the new one
        if (config.isDetectAliases()) {

            mentions = findRenamed(message);

            if (!mentions.isEmpty()) {
                return mentions;
            }
        }

        if (config.isDetectUnicode()) {

            String folded = TextNormalizer.unicodeFold(message);

            if (!folded.equals(message)) {

                mentions = findIn(folded);

                if (!mentions.isEmpty()) {
                    return mentions;
                }

                mentions = findRenamed(folded);

                if (!mentions.isEmpty()) {
                    return mentions;
                }
            }
        }

        if (config.isDetectDecor()) {

            mentions = findIn(
                    TextNormalizer.decorationFold(message)
            );

            if (!mentions.isEmpty()) {
                return mentions;
            }

            mentions = findRenamed(
                    TextNormalizer.decorationFold(message)
            );

            if (!mentions.isEmpty()) {
                return mentions;
            }
        }

        mentions = detectStripped(message);

        if (!mentions.isEmpty()) {
            return mentions;
        }

        mentions = detectStrippedRenamed(message);

        if (!mentions.isEmpty()) {
            return mentions;
        }

        if (config.isDetectAliases()) {
            return NickAliases.detect(message);
        }

        return List.of();
    }

    /**
 * Mentions of players known only under a nick they changed to.
 *
 * These are NOT highlighted: the word belongs to somebody's sentence and
 * repainting it would turn unrelated chat words yellow.
 */
    private static List<PlayerMention> findRenamed(
            String message
    ) {

        List<PlayerMention> mentions = new ArrayList<>();

        String lower = message.toLowerCase();

        for (PlayerContext player :
                EssentialsNickTracker.get().findIn(message)) {

            String nick =
                    nickOf(player, message);

            if (nick == null) {
                continue;
            }

            int index = lower.indexOf(nick);

            while (index >= 0) {

                int end = index + nick.length();

                boolean left =
                        index == 0 ||
                                !TextNormalizer.isNameChar(
                                        lower.charAt(index - 1));

                boolean right =
                        end >= lower.length() ||
                                !TextNormalizer.isNameChar(
                                        lower.charAt(end));

                if (left && right) {

                    mentions.add(
                            new PlayerMention(
                                    player,
                                    index,
                                    end,
                                    false
                            )
                    );
                }

                index = lower.indexOf(nick, index + 1);
            }
        }

        return mentions;
    }

    /** The changed nick of a player as it appears in the text. */
    private static String nickOf(
            PlayerContext player,
            String message
    ) {

        for (String nick :
                EssentialsNickTracker.get()
                        .nicksOf(player)) {

            if (message.toLowerCase().contains(nick)) {
                return nick;
            }
        }

        return null;
    }

    /** Tab list + local cache matching on the given string. */
    private static List<PlayerMention> findIn(
            String message
    ) {

        List<PlayerMention> mentions =
                new ArrayList<>();

        if (StafftoolsClient.getConfig().isDetectPlain()) {

            mentions.addAll(
                    rangesOf(
                            message,
                            PlayerResolver.findPlayers(message)
                    )
            );
        }

        if (mentions.isEmpty() &&
                StafftoolsClient.getConfig().isDetectCache()) {

            mentions.addAll(
                    rangesOf(
                            message,
                            NickCache.get().findIn(message)
                    )
            );
        }

        return mentions;
    }

    /** Converts players into word-bounded mention ranges. */
    private static List<PlayerMention> rangesOf(
            String message,
            List<PlayerContext> players
    ) {

        List<PlayerMention> mentions =
                new ArrayList<>();

        String lower = message.toLowerCase();

        for (PlayerContext player : players) {

            String name = player.name();
            String target = name.toLowerCase();

            int index = lower.indexOf(target);

            while (index >= 0) {

                boolean left =
                        index == 0 ||
                                !TextNormalizer.isNameChar(
                                        lower.charAt(index - 1)
                                );

                boolean right =
                        index + target.length() >= lower.length() ||
                                !TextNormalizer.isNameChar(
                                        lower.charAt(index + target.length())
                                );

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
 * Strips legacy formatting codes ("§x") from the message while
 * keeping the indices stable, then runs the given detection on the
 * clean text and converts the resulting ranges back.
 */
private static List<PlayerMention> detectStripped(
        String message
) {

    int[] map = new int[message.length()];

    StringBuilder clean = new StringBuilder();

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

    return remap(findIn(clean.toString()), map);
}

/** Same, but over the players known only under a changed nick. */
private static List<PlayerMention> detectStrippedRenamed(
        String message
) {

    int[] map = new int[message.length()];

    StringBuilder clean = new StringBuilder();

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

    return remap(
            findRenamed(clean.toString()),
            map
    );
}

/** Converts ranges found in the cleaned text back to the original. */
private static List<PlayerMention> remap(
        List<PlayerMention> stripped,
        int[] map
) {

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
}
