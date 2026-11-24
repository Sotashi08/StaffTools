package com.uwdie.stafftools.client.chat;

import com.uwdie.stafftools.client.player.PlayerContext;
import com.uwdie.stafftools.client.player.PlayerResolver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
 * Whether a learned nick is highlighted is controlled separately from
 * whether it is detected, so it remains clickable when highlighting is off.
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

    /** Finds name-shaped words inside decorated tab-list display names. */
    private static final Pattern DISPLAY_NAME_WORD =
            Pattern.compile("[A-Za-z0-9_]{2,16}");

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
                    "one", "two", "the", "and", "for", "you",
                    "ct", "cт", "adm", "мод",
                    "модер", "модератор", "админ", "администратор",
                    "хелпер", "саппорт", "вип", "премиум",
                    "игрок", "участник", "владелец"
            );

    /** Maximum entries before LRU eviction. */
    private static final int MAX_ENTRIES = 400;

    /** Entries not accessed for this long are considered stale (1 hour). */
    private static final long STALE_THRESHOLD_MS = 60 * 60 * 1000L;

    /** Minimum interval between automatic cleanups (5 minutes). */
    private static final long CLEANUP_INTERVAL_MS = 5 * 60 * 1000L;

    /** visible nick (lower case) -> the account behind it with last-access timestamp */
    private final Map<String, TrackedNick> byNick =
            new LinkedHashMap<>();

    private long lastCleanupAt = 0;

    /** Entry with last-access timestamp for stale cleanup. */
    private static final class TrackedNick {
        final PlayerContext owner;
        long lastAccessed;

        TrackedNick(PlayerContext owner) {
            this.owner = owner;
            this.lastAccessed = System.currentTimeMillis();
        }

        void touch() {
            this.lastAccessed = System.currentTimeMillis();
        }
    }

    private EssentialsNickTracker() {
    }

    public static EssentialsNickTracker get() {
        return INSTANCE;
    }

    public void clear() {

        byNick.clear();
        lastCleanupAt = 0;
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

            PlayerContext owner =
                    new PlayerContext(
                            real,
                            entry.getProfile().id()
                    );

            if (nick != null) {
                learn(nick, owner);
            }

            learnFromDisplayName(raw, owner);
        }
    }

    /**
     * Learns name-shaped words from a changed tab-list display name.
     *
     * Rank labels and suffix words are filtered by {@link #NOT_A_NICK}.
     * The real account name is excluded, so disabling Essentials
     * highlighting cannot repaint an ordinary real nick.
     */
    public void learnFromDisplayName(
            String displayName,
            PlayerContext owner
    ) {

        if (displayName == null ||
                displayName.isBlank() ||
                owner == null ||
                owner.name() == null ||
                displayName.equalsIgnoreCase(owner.name())) {

            return;
        }

        String cleaned = stripLegacy(displayName).trim();

        if (cleaned.isEmpty()) {
            return;
        }

        String folded =
                TextNormalizer.unicodeFold(cleaned)
                        .toLowerCase(Locale.ROOT);

        if (isOnlyCosmeticNameChange(cleaned, owner.name())) {
            return;
        }

        if (!folded.contains(" ") &&
                folded.chars().allMatch(c ->
                        TextNormalizer.isNameChar((char) c)
                )) {

            learn(folded, owner);
        }

        Matcher matcher = DISPLAY_NAME_WORD.matcher(folded);

        while (matcher.find()) {
            learn(matcher.group(), owner);
        }
    }

    /**
     * True when a display name contains the real account name and no
     * separate custom name. Formatting, rank prefixes and legacy codes
     * do not turn the original nick into an EssentialsX custom nick.
     */
    public static boolean isOnlyCosmeticNameChange(
            String displayName,
            String realName
    ) {

        if (displayName == null ||
                realName == null ||
                displayName.isBlank() ||
                realName.isBlank()) {

            return false;
        }

        String cleaned =
                stripLegacy(displayName).trim();

        if (cleaned.isEmpty()) {
            return false;
        }

        String folded =
                TextNormalizer.unicodeFold(cleaned)
                        .toLowerCase(Locale.ROOT);

        String real =
                TextNormalizer.unicodeFold(realName)
                        .toLowerCase(Locale.ROOT);

        Matcher matcher = DISPLAY_NAME_WORD.matcher(folded);
        boolean foundRealName = false;
        boolean foundCustomName = false;

        while (matcher.find()) {

            String word =
                    matcher.group().toLowerCase(Locale.ROOT);

            if (word.equals(real)) {
                foundRealName = true;
                continue;
            }

            if (!isNotANick(word) &&
                    !word.equalsIgnoreCase(realName)) {

                foundCustomName = true;
            }
        }

        return foundRealName && !foundCustomName;
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

        return NOT_A_NICK.contains(word.toLowerCase(Locale.ROOT));
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

        String key = clean.toLowerCase(Locale.ROOT);
        TrackedNick existing = byNick.get(key);
        if (existing != null) {
            existing.touch();
            return;
        }

        byNick.put(key, new TrackedNick(owner));

        if (byNick.size() > MAX_ENTRIES) {
            evictOldest();
        }

        maybeCleanup();
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
                TrackedNick tracked = byNick.get(
                        oldNick.toLowerCase(Locale.ROOT));
                if (tracked != null) {
                    owner = tracked.owner;
                }
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

        String key = nick.trim().toLowerCase(Locale.ROOT);
        TrackedNick tracked = byNick.get(key);

        if (tracked != null) {
            tracked.touch();
            return tracked.owner;
        }

        return null;
    }

    /** Every changed nick known for one account. */
    public List<String> nicksOf(PlayerContext player) {

        List<String> result = new ArrayList<>();

        if (player == null) {
            return result;
        }

        for (Map.Entry<String, TrackedNick> entry :
                byNick.entrySet()) {

            if (entry.getValue().owner.name()
                    .equalsIgnoreCase(player.name())) {

                result.add(entry.getKey());
            }
        }

        return result;
    }

    /**
     * Checks whether text contains a tracked EssentialsX nick owned by
     * this player. Formatting and Unicode look-alikes are normalized in
     * the same way as they are for nick detection.
     */
    public boolean containsNickOf(
            PlayerContext player,
            String text
    ) {

        if (player == null ||
                player.name() == null ||
                text == null ||
                text.isBlank()) {

            return false;
        }

        String lower = TextNormalizer.unicodeFold(stripLegacy(text))
                .toLowerCase(Locale.ROOT);

        for (Map.Entry<String, TrackedNick> entry : byNick.entrySet()) {

            TrackedNick tracked = entry.getValue();

            if (!belongsTo(tracked.owner, player)) {
                continue;
            }

            String nick = entry.getKey();
            int index = lower.indexOf(nick);

            while (index >= 0) {

                int end = index + nick.length();
                boolean left =
                        index == 0 ||
                                !TextNormalizer.isNameChar(
                                        lower.charAt(index - 1)
                                );
                boolean right =
                        end >= lower.length() ||
                                !TextNormalizer.isNameChar(
                                        lower.charAt(end)
                                );

                if (left && right) {
                    tracked.touch();
                    return true;
                }

                index = lower.indexOf(nick, index + 1);
            }
        }

        return false;
    }

    private static boolean belongsTo(
            PlayerContext first,
            PlayerContext second
    ) {

        if (first.uuid() != null && second.uuid() != null) {
            return first.uuid().equals(second.uuid());
        }

        return first.name() != null &&
                second.name() != null &&
                first.name().equalsIgnoreCase(second.name());
    }

    /** Accounts whose changed nick appears in the text. */
    public List<PlayerContext> findIn(String text) {

        List<PlayerContext> result = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return result;
        }

        String lower =
                TextNormalizer.unicodeFold(text)
                        .toLowerCase(Locale.ROOT);

        for (Map.Entry<String, TrackedNick> entry :
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

                    entry.getValue().touch();
                    result.add(entry.getValue().owner);

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

    /** Removes entries not accessed within the stale threshold. */
    public void cleanupStaleEntries() {
        long now = System.currentTimeMillis();
        long cutoff = now - STALE_THRESHOLD_MS;

        byNick.entrySet().removeIf(entry ->
                entry.getValue().lastAccessed < cutoff
        );

        lastCleanupAt = now;
    }

    /** Triggers cleanup if enough time has passed since last cleanup. */
    private void maybeCleanup() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupAt > CLEANUP_INTERVAL_MS) {
            cleanupStaleEntries();
        }
    }

    /** Evicts the oldest entry (LRU). */
    private void evictOldest() {
        if (!byNick.isEmpty()) {
            String oldest = byNick.keySet().iterator().next();
            byNick.remove(oldest);
        }
    }
}
