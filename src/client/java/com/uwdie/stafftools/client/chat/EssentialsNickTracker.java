package com.uwdie.stafftools.client.chat;

import com.uwdie.stafftools.client.player.PlayerContext;
import com.uwdie.stafftools.client.player.PlayerResolver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Knows which visible name belongs to which account on servers that let
 * players change their nick (EssentialsX {@code /nick}).
 *
 * <h2>Why this is delicate</h2>
 * After {@code /nick} the account name in the tab list stays untouched
 * while the entry's display name becomes something else, so a lookup by
 * profile name simply does not find those players. The tempting shortcut
 * is to treat every word of that display name as a name — and that is
 * exactly what breaks: an EssentialsX display name normally looks like
 * {@code "[VIP] CoolNick builder"}, where "VIP" is a rank and "builder"
 * is a suffix. Registering those words makes the mod repaint "VIP" and
 * "builder" wherever they appear and open the actions popup on them.
 *
 * <h2>What is accepted here</h2>
 * <ol>
 *   <li>A nick-change announcement, which names the <em>old</em> nick and
 *       the new one in the same sentence. That pairing is unambiguous
 *       and is the only source that can prove what the new word means.</li>
 *   <li>A display name that is <em>nothing but</em> one name-shaped word
 *       once decorations are stripped. Then that word is the nick with
 *       certainty, and nothing else in it can be mistaken for a name.</li>
 * </ol>
 * Decorative sub-words of a longer display name are deliberately NOT
 * registered.
 *
 * Nicks learned here are reported with {@link PlayerMention#highlight()}
 * set to {@code false}: they are ordinary words inside somebody's
 * message, so they must not be repainted.
 */
public final class EssentialsNickTracker {

    private static final EssentialsNickTracker INSTANCE =
            new EssentialsNickTracker();

    /** "oldnick is now known as newnick" and friends. */
    private static final Pattern[] CHANGE_PATTERNS = {
            Pattern.compile(
                    "([A-Za-z0-9_]{2,16})\\s+is\\s+now\\s+known\\s+as"
                            + "\\s+([A-Za-z0-9_]{2,16})",
                    Pattern.CASE_INSENSITIVE
            ),
            Pattern.compile(
                    "nick(?:name)?(?:\\s+for)?\\s+([A-Za-z0-9_]{2,16})"
                            + "\\s+(?:has\\s+been\\s+changed\\s+to|was\\s+changed"
                            + "\\s+to|is\\s+now|changed\\s+to)\\s+"
                            + "([A-Za-z0-9_]{2,16})",
                    Pattern.CASE_INSENSITIVE
            ),
            Pattern.compile(
                    "([A-Za-z0-9_]{2,16})\\s+(?:is\\s+now|has\\s+changed"
                            + "\\s+their\\s+name\\s+to|changed\\s+their"
                            + "\\s+name\\s+to)\\s+([A-Za-z0-9_]{2,16})",
                    Pattern.CASE_INSENSITIVE
            ),
            // locale independent: an arrow between the two names
            Pattern.compile(
                    "([A-Za-z0-9_]{2,16})\\s*(?:->|→)\\s*([A-Za-z0-9_]{2,16})"
            )
    };

    /**
     * Words that are ranks, suffixes or server furniture. They can never
     * be a /nick, so even an ambiguous source must not claim them.
     */
    private static final java.util.Set<String> NOT_A_NICK =
            java.util.Set.of(
                    "admin", "administrator", "moder", "moderator",
                    "mod", "helper", "support",
                    "vip", "vipplus", "premium", "premiumplus", "donor",
                    "sponsor", "youtube", "yt", "streamer", "creator",
                    "staff", "owner", "developer", "dev", "builder",
                    "member", "user", "player", "default", "none",
                    "online", "offline", "true", "false", "yes", "no",
                    "chat", "global", "local", "world", "title",
                    "health", "level", "rank", "group", "team", "clan",
                    "guild", "faction", "nation", "server", "lobby",
                    "hub", "survival", "creative", "skyblock",
                    "one", "two", "the", "and", "for", "you"
            );

    /** visible nick (lower case) -> the account behind it */
    private final Map<String, PlayerContext> byNick =
            new LinkedHashMap<>();

    private EssentialsNickTracker() {
    }

    public static EssentialsNickTracker get() {
        return INSTANCE;
    }

    public void clear() {

        byNick.clear();
    }


    /**
     * Reads the tab list for display names that consist of exactly one
     * name-shaped word.
     *
     * That is the only display-name shape that proves the word is the
     * player's own nick. Anything longer is left alone on purpose: its
     * individual words are ranks or suffixes, and treating them as nicks
     * is what made the mod highlight and link unrelated words.
     */
    public void refreshFromTabList() {

        var handler = MinecraftClient.getInstance()
                .getNetworkHandler();

        if (handler == null) {
            return;
        }

        for (PlayerListEntry entry : handler.getPlayerList()) {

            String real = entry.getProfile().name();

            if (real == null || real.isBlank()) {
                continue;
            }

            var display = entry.getDisplayName();

            if (display == null) {
                continue;
            }

            String raw = display.getString();

            if (raw == null ||
                    raw.equalsIgnoreCase(real)) {

                // the display name still is the account name: the nick
                // was never changed
                continue;
            }

            String nick = soleNickOf(raw);

            if (nick == null) {
                continue;
            }

            learn(
                    nick,
                    new PlayerContext(
                            real,
                            entry.getProfile().id()
                    )
            );
        }
    }

    /**
     * The one nick inside a display name, or {@code null} when the name
     * holds anything besides that single word.
     */
    private static String soleNickOf(String display) {

        String stripped = stripLegacy(display).trim();

        if (stripped.isEmpty()) {
            return null;
        }

        // decoration folding must not change the length, otherwise a
        // name like "A_B" could be mistaken for two words
        String folded =
                TextNormalizer.decorationFold(stripped);

        if (folded.length() != stripped.length()) {

            // the raw name contained a character that is part of it,
            // so it is taken verbatim below instead
            folded = stripped;
        }

        String collapsed = folded.trim();

        if (collapsed.isEmpty() ||
                collapsed.contains(" ")) {

            // more than one word: ranks and/or suffixes are present and
            // there is no way to tell which word is the nick
            return null;
        }

        if (isNotANick(collapsed)) {
            return null;
        }

        return collapsed;
    }

    private static boolean isNotANick(String word) {

        return NOT_A_NICK.contains(word.toLowerCase());
    }

    /** Drops "§x" pairs. */
    private static String stripLegacy(String source) {

        if (source == null || source.indexOf('\u00A7') < 0) {
            return source;
        }

        StringBuilder out = new StringBuilder();

        for (int i = 0; i < source.length(); i++) {

            char c = source.charAt(i);

            if (c == '\u00A7' && i + 1 < source.length()) {
                i++;
                continue;
            }

            out.append(c);
        }

        return out.toString();
    }

    /** Records a nick for an account. */
    public void learn(
            String nick,
            PlayerContext owner
    ) {

        if (nick == null ||
                owner == null ||
                owner.name() == null) {

            return;
        }

        String clean = nick.trim();

        if (clean.length() < 2 ||
                clean.length() > 16 ||
                isNotANick(clean)) {

            return;
        }

        if (clean.equalsIgnoreCase(owner.name())) {
            return;
        }

        byNick.put(clean.toLowerCase(), owner);

        if (byNick.size() > 400) {

            String oldest =
                    byNick.keySet().iterator().next();
            byNick.remove(oldest);
        }
    }

    /**
     * Parses a nick-change announcement.
     *
     * The announcement states both the old nick and the new one, and the
     * old one is resolvable — either it is an account we see online, or
     * it is a nick we already learned. That pairing is the only sound way
     * to learn a new nick.
     */
    public void observeMessage(String message) {

        if (message == null || message.isBlank()) {
            return;
        }

        // legacy codes would split the sentence apart
        String text = stripLegacy(message);

        for (Pattern pattern : CHANGE_PATTERNS) {

            Matcher matcher = pattern.matcher(text);

            if (!matcher.find()) {
                continue;
            }

            String oldNick = matcher.group(1);
            String newNick = matcher.group(2);

            if (newNick.equalsIgnoreCase(oldNick)) {
                continue;
            }

            PlayerContext owner =
                    PlayerResolver.resolve(oldNick)
                            .orElse(null);

            if (owner == null) {
                owner = byNick.get(
                        oldNick.toLowerCase());
            }

            if (owner != null) {
                learn(newNick, owner);
                return;
            }
        }
    }

    /** The account a nick belongs to, or {@code null}. */
    public PlayerContext resolve(String nick) {

        if (nick == null || nick.isBlank()) {
            return null;
        }

        return byNick.get(nick.trim().toLowerCase());
    }

    /** True when the given word is a known changed nick. */

    /** Every changed nick known for one account. */
    public List<String> nicksOf(PlayerContext player) {

        List<String> result = new ArrayList<>();

        if (player == null) {
            return result;
        }

        for (Map.Entry<String, PlayerContext> entry :
                byNick.entrySet()) {

            if (entry.getValue().name()
                    .equalsIgnoreCase(player.name())) {

                result.add(entry.getKey());
            }
        }

        return result;
    }

    /** Accounts whose changed nick appears in the text. */
    public List<PlayerContext> findIn(String text) {

        List<PlayerContext> result = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return result;
        }

        String lower =
                TextNormalizer.unicodeFold(text)
                        .toLowerCase();

        for (Map.Entry<String, PlayerContext> entry :
                byNick.entrySet()) {

            String nick = entry.getKey();

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

                    result.add(entry.getValue());

                    break;
                }

                index = lower.indexOf(nick, index + 1);
            }
        }

        result.sort(
                (a, b) ->
                        b.name().length() - a.name().length()
        );

        return result;
    }
}
