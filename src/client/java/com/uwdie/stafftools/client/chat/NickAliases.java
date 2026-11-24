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

    /** Maximum entries before LRU eviction. */
    private static final int MAX_ENTRIES = 500;

    /** Entries not accessed for this long are considered stale (1 hour). */
    private static final long STALE_THRESHOLD_MS = 60 * 60 * 1000L;

    /** Minimum interval between automatic cleanups (5 minutes). */
    private static final long CLEANUP_INTERVAL_MS = 5 * 60 * 1000L;

    /** alias (lower case) -> tracked entry with last-access timestamp */
    private static final Map<String, TrackedAlias> ALIASES =
            new ConcurrentHashMap<>();

    private static long lastCleanupAt = 0;

    /** Entry with last-access timestamp for stale cleanup. */
    private static final class TrackedAlias {
        final PlayerContext player;
        long lastAccessed;

        TrackedAlias(PlayerContext player) {
            this.player = player;
            this.lastAccessed = System.currentTimeMillis();
        }

        void touch() {
            this.lastAccessed = System.currentTimeMillis();
        }
    }

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

            TrackedAlias existing = ALIASES.get(lower);
            if (existing != null) {
                existing.touch();
                continue;
            }

            if (ALIASES.size() >= MAX_ENTRIES) {
                evictOldest();
            }

            ALIASES.put(lower, new TrackedAlias(player));
            maybeCleanup();
        }
    }

    /** Exact (case-insensitive) alias lookup, used by the click triggers. */
    public static java.util.Optional<PlayerContext> lookup(
            String alias
    ) {

        if (alias == null || alias.isBlank()) {
            return java.util.Optional.empty();
        }

        String key = alias.trim().toLowerCase();
        TrackedAlias tracked = ALIASES.get(key);

        if (tracked != null) {
            tracked.touch();
            return java.util.Optional.of(tracked.player);
        }

        return java.util.Optional.empty();
    }

    /** Finds learned alias occurrences in arbitrary text. */
    public static List<PlayerMention> detect(String message) {
        List<PlayerMention> result = new ArrayList<>();

        if (message == null || message.isBlank()) {
            return result;
        }

        String lower = message.toLowerCase();

        for (Map.Entry<String, TrackedAlias> e :
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

                    e.getValue().touch();
                    result.add(
                            new PlayerMention(
                                    e.getValue().player,
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

    /** Removes entries not accessed within the stale threshold. */
    public static void cleanupStaleEntries() {
        long now = System.currentTimeMillis();
        long cutoff = now - STALE_THRESHOLD_MS;

        ALIASES.entrySet().removeIf(entry ->
                entry.getValue().lastAccessed < cutoff
        );

        lastCleanupAt = now;
    }

    /** Triggers cleanup if enough time has passed since last cleanup. */
    private static void maybeCleanup() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupAt > CLEANUP_INTERVAL_MS) {
            cleanupStaleEntries();
        }
    }

    /** Evicts the oldest entry (LRU). */
    private static void evictOldest() {
        if (!ALIASES.isEmpty()) {
            String oldest = ALIASES.keySet().iterator().next();
            ALIASES.remove(oldest);
        }
    }

    private static boolean isNameChar(char c) {
        return Character.isLetterOrDigit(c)
                || c == '_';
    }
}
