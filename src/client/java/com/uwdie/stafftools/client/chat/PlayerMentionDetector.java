package com.uwdie.stafftools.client.chat;

import com.uwdie.stafftools.client.StafftoolsClient;
import com.uwdie.stafftools.client.audit.AuditCategory;
import com.uwdie.stafftools.client.audit.AuditLog;
import com.uwdie.stafftools.client.audit.Profiler;
import com.uwdie.stafftools.client.config.StaffToolsConfig;
import com.uwdie.stafftools.client.player.PlayerContext;
import com.uwdie.stafftools.client.player.PlayerResolver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Detects player mentions inside a plain chat string.
 *
 * <p>Only layers that are reliable in practice are kept. Each one is
 * independently switchable in the detection menu, and every layer that
 * produced a hit is reported to the audit journal so a moderator can see
 * <em>why</em> a nick was recognised.
 *
 * <ol>
 *   <li>real nicks from the tab list,</li>
 *   <li>nicks from the local cache of everyone seen this session,</li>
 *   <li>the same two after folding look-alike unicode
 *       ("\u1D04ᴀᴘꜱ" -> "caps"),</li>
 *   <li>the same two after replacing decorations with spaces
 *       ("[VIP]Notch\u2605" -> " VIP Notch "),</li>
 *   <li>the same two after stripping legacy colour codes
 *       ("\u00A7aPl\u00A7fayer" -> "Player"),</li>
 *   <li>nicks learned from a /nick change (EssentialsX), where the tab
 *       list shows the new name while the real one is nowhere in the
 *       text,</li>
 *   <li>aliases learned from click / hover events, as the loosest
 *       fallback.</li>
 * </ol>
 *
 * <p>The typo-tolerant layer was removed: a one-edit match against the
 * whole session cache turned ordinary chat words into false positives,
 * which is worse than missing a misspelled nick.
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

        long startedAt = System.nanoTime();

        try {
            return detectInternal(message);
        } finally {
            Profiler.get().record(
                    Profiler.DETECT,
                    System.nanoTime() - startedAt
            );
        }
    }

    private static List<PlayerMention> detectInternal(
            String message
    ) {

        StaffToolsConfig config =
                StafftoolsClient.getConfig();

        // Every layer is consulted, not just the first one that hits.
        // A single line routinely mixes a real nick from the tab list
        // with a /nick player known only through the tracker, and an
        // early return used to hide the second one.
        List<PlayerMention> all = new ArrayList<>();

        // 1. real nicks from the tab list and the session cache
        List<PlayerMention> plain = findIn(message);
        all.addAll(plain);

        // 2. players known only under a nick they changed to
        List<PlayerMention> renamed = List.of();

        if (config.isDetectAliases()) {
            renamed = findRenamed(message);
            all.addAll(renamed);
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

        boolean disableNickHighlight =
                config.isNickHighlightDisabled();
        List<PlayerMention> merged = merge(
                all,
                disableNickHighlight
        );

        if (!merged.isEmpty()) {

            auditLayers(plain, renamed, merged);

            return merged;
        }

        // 6. aliases learned from click / hover events, used as a
        //    fallback: they are the loosest layer
        if (config.isDetectAliases()) {

            List<PlayerMention> aliases = NickAliases.detect(message);

            if (!aliases.isEmpty()) {

                AuditLog.get().info(
                        AuditCategory.DETECT,
                        "Matched via learned alias: "
                                + aliases.get(0).player().name()
                );
            }

            return applyNickHighlightSetting(
                    aliases,
                    disableNickHighlight
            );
        }

        return List.of();
    }

    /**
     * Reports which layers contributed to a successful detection, so the
     * audit journal explains the result instead of only stating it.
     */
    private static void auditLayers(
            List<PlayerMention> plain,
            List<PlayerMention> renamed,
            List<PlayerMention> merged
    ) {

        StringBuilder layers = new StringBuilder();

        if (!plain.isEmpty()) {
            layers.append("tab/cache");
        }

        if (!renamed.isEmpty()) {
            layers.append(layers.length() > 0 ? "+nick" : "nick");
        }

        if (layers.length() == 0) {
            layers.append("folded");
        }

        AuditLog.get().info(
                AuditCategory.DETECT,
                "Layer " + layers + " matched "
                        + merged.size() + " nick(s)"
        );
    }

    /**
     * Runs the full detection and reports the outcome to the audit
     * journal. Used by the chat pipeline, where "no nick found" is just
     * as interesting as a hit.
     */
    public static List<PlayerMention> detectAndAudit(
            String message
    ) {

        List<PlayerMention> mentions = detect(message);

        if (mentions.isEmpty()) {

            AuditLog.get().info(
                    AuditCategory.DETECT,
                    "No nick detected in: " + preview(message)
            );

            return mentions;
        }

        StringBuilder names = new StringBuilder();

        for (PlayerMention mention : mentions) {

            if (names.length() > 0) {
                names.append(", ");
            }

            names.append(mention.player().name());

            if (!mention.highlight()) {
                names.append(" (nick)");
            }
        }

        AuditLog.get().success(
                AuditCategory.DETECT,
                "Detected " + names + " in: " + preview(message)
        );

        return mentions;
    }

    /** Short, single-line excerpt of a chat message for the journal. */
    private static String preview(String message) {

        if (message == null) {
            return "";
        }

        String flat = message.replace('\n', ' ').trim();

        return flat.length() <= 60
                ? flat
                : flat.substring(0, 57) + "...";
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
            List<PlayerMention> mentions,
            boolean disableNickHighlight
    ) {

        List<PlayerMention> adjusted =
                applyNickHighlightSetting(
                        mentions,
                        disableNickHighlight
                );

        if (adjusted.size() < 2) {
            return adjusted;
        }

        List<PlayerMention> sorted = new ArrayList<>(adjusted);

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

    static List<PlayerMention> applyNickHighlightSetting(
            List<PlayerMention> mentions,
            boolean disableNickHighlight
    ) {

        if (mentions.isEmpty() ||
                !disableNickHighlight) {

            return mentions;
        }

        List<PlayerMention> adjusted = null;

        for (int i = 0; i < mentions.size(); i++) {

            PlayerMention mention = mentions.get(i);

            if (!mention.highlight()) {
                continue;
            }

            if (adjusted == null) {
                adjusted = new ArrayList<>(mentions);
            }

            adjusted.set(
                    i,
                    new PlayerMention(
                            mention.player(),
                            mention.start(),
                            mention.end(),
                            false
                    )
            );
        }

        return adjusted == null ? mentions : adjusted;
    }

    /**
     * Mentions of players known only under a changed nick.
     */
    private static List<PlayerMention> findRenamed(
            String message
    ) {

        List<PlayerMention> mentions = new ArrayList<>();

        String lower = message.toLowerCase(Locale.ROOT);
        boolean highlight = !StafftoolsClient.getConfig()
                .isNickHighlightDisabled();

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
                                    highlight
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

            if (message.toLowerCase(Locale.ROOT).contains(nick)) {
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
                        map[m.end() - 1] + 1,
                        m.highlight()
                )
        );
    }

    return result;
}
}
