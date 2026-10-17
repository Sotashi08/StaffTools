package com.uwdie.stafftools.client.chat;

import com.uwdie.stafftools.client.StafftoolsClient;
import com.uwdie.stafftools.client.config.StaffToolsConfig;
import com.uwdie.stafftools.client.player.PlayerContext;
import com.uwdie.stafftools.client.player.PlayerResolver;

import java.util.ArrayList;
import java.util.Comparator;
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

        // Every layer is consulted, not just the first one that hits.
        // A single line routinely mixes a real nick from the tab list
        // with a /nick player known only through the tracker, and an
        // early return used to hide the second one.
        List<PlayerMention> all = new ArrayList<>();

        // 1. real nicks from the tab list and the session cache
        all.addAll(findIn(message));

        // 2. players known only under a nick they changed to
        if (config.isDetectAliases()) {
            all.addAll(findRenamed(message));
        }

        // 3. the same two after folding look-alike unicode
        if (config.isDetectUnicode()) {

            String folded = TextNormalizer.unicodeFold(message);

            if (!folded.equals(message)) {
                all.addAll(findIn(folded));
                all.addAll(findRenamed(folded));
            }
        }

        // 4. the same two after turning decorations into spaces
        if (config.isDetectDecor()) {

            String folded = TextNormalizer.decorationFold(message);

            if (!folded.equals(message)) {
                all.addAll(findIn(folded));
                all.addAll(findRenamed(folded));
            }
        }

        // 5. the same two with legacy colour codes removed
        all.addAll(detectStripped(message));
        all.addAll(detectStrippedRenamed(message));

        List<PlayerMention> merged = merge(all);

        if (!merged.isEmpty()) {
            return merged;
        }

        // 6. aliases learned from click / hover events, used as a
        //    fallback: they are the loosest layer
        if (config.isDetectAliases()) {
            return NickAliases.detect(message);
        }

        return List.of();
    }

    /**
     * Sorts the findings and removes the ones that collide.
     *
     * Layers overlap by nature — the tab list and the tracker can both
     * claim the same range — and two layers can claim intersecting
     * ranges, which would otherwise render as duplicated or garbled
     * text. The longest range at the earliest position wins, so a real
     * nick is never chopped by a shorter match inside it.
     */
    private static List<PlayerMention> merge(
            List<PlayerMention> mentions
    ) {

        if (mentions.size() < 2) {
            return mentions;
        }

        List<PlayerMention> sorted = new ArrayList<>(mentions);

        // longest first so that, at the same position, the widest range
        // is considered before the narrow ones
        sorted.sort(
                Comparator
                        .comparingInt(PlayerMention::start)
                        .thenComparing(
                                (PlayerMention m) ->
                                        m.end() - m.start(),
                                Comparator.reverseOrder()
                        )
        );

        List<PlayerMention> result = new ArrayList<>();

        int cursor = 0;

        for (PlayerMention mention : sorted) {

            if (mention.start() < cursor ||
                    mention.start() >= mention.end()) {

                continue;
            }

            // the same range found twice by two layers: keep the one
            // that may be repainted, so a /nick guess never hides a
            // real nick
            if (!result.isEmpty()) {

                PlayerMention last =
                        result.get(result.size() - 1);

                if (last.start() == mention.start() &&
                        last.end() == mention.end()) {

                    if (mention.highlight() &&
                            !last.highlight()) {

                        result.set(
                                result.size() - 1,
                                mention
                        );
                    }

                    continue;
                }
            }

            result.add(mention);

            cursor = mention.end();
        }

        return result;
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
