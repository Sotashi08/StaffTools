package com.uwdie.stafftools.client.chat;

import com.uwdie.stafftools.client.player.PlayerContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Local memory of every nick the mod has ever seen in this session:
 * the current tab list plus everything resolved from chat, hover
 * tooltips and command click-events.
 *
 * The vanilla tab list only knows ONLINE players, so a punishment aimed
 * at somebody who left two minutes ago would not resolve at all — this
 * cache makes it resolve anyway (offline-mode UUID derived from the nick).
 */
public final class NickCache {

    private static final NickCache INSTANCE =
            new NickCache();

    /** Insertion ordered: the newest nick wins on a cap. */
    private final Map<String, CachedNick> nicks =
            new LinkedHashMap<>();

    /** Maximum entries before LRU eviction. */
    private static final int MAX_ENTRIES = 600;

    /** Entries not accessed for this long are considered stale (1 hour). */
    private static final long STALE_THRESHOLD_MS = 60 * 60 * 1000L;

    /** Minimum interval between automatic cleanups (5 minutes). */
    private static final long CLEANUP_INTERVAL_MS = 5 * 60 * 1000L;

    private long lastCleanupAt = 0;

    private NickCache() {
    }

    public static NickCache get() {
        return INSTANCE;
    }

    /** Drops everything remembered from the current server session. */
    public void clear() {
        nicks.clear();
        lastCleanupAt = 0;
    }

    /** Entry with last-access timestamp for stale cleanup. */
    private static final class CachedNick {
        final PlayerContext player;
        long lastAccessed;

        CachedNick(PlayerContext player) {
            this.player = player;
            this.lastAccessed = System.currentTimeMillis();
        }

        void touch() {
            this.lastAccessed = System.currentTimeMillis();
        }
    }

    public void remember(String name) {

        if (name == null || name.isBlank()) {
            return;
        }

        String key = name.toLowerCase();

        CachedNick existing = nicks.get(key);
        if (existing != null) {
            existing.touch();
            return;
        }

        nicks.put(
                key,
                new CachedNick(new PlayerContext(name, offlineUuid(name)))
        );

        if (nicks.size() > MAX_ENTRIES) {
            evictOldest();
        }

        maybeCleanup();
    }

    public void remember(PlayerContext player) {

        if (player != null && player.name() != null) {
            remember(player.name());
        }
    }

    /** Takes a snapshot of everyone currently in the tab list. */
    public void refreshFromTabList() {

        var handler = MinecraftClient.getInstance()
                .getNetworkHandler();

        if (handler == null) {
            return;
        }

        for (PlayerListEntry entry : handler.getPlayerList()) {

            String name = entry.getProfile().name();

            if (name != null) {
                remember(name);
            }
        }
    }

    /** Exact, case-insensitive lookup. */
    public Optional<PlayerContext> resolve(String name) {

        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        String key = name.trim().toLowerCase();
        CachedNick cached = nicks.get(key);

        if (cached != null) {
            cached.touch();
            return Optional.of(cached.player);
        }

        return Optional.empty();
    }

    /** Best nick that differs from the target by at most one edit. */
    public Optional<PlayerContext> resolveFuzzy(String name) {

        if (name == null || name.length() < 4) {
            return Optional.empty();
        }

        String target = name.trim().toLowerCase();

        PlayerContext best = null;
        int bestDistance = Integer.MAX_VALUE;

        for (Map.Entry<String, CachedNick> entry :
                nicks.entrySet()) {

            String candidate = entry.getKey();

            if (Math.abs(candidate.length() - target.length()) > 1) {
                continue;
            }

            int distance = levenshtein(target, candidate);

            if (distance < bestDistance) {
                bestDistance = distance;
                best = entry.getValue().player;
            }
        }

        if (best != null && bestDistance <= 1) {
            // Touch the matched entry
            for (CachedNick cn : nicks.values()) {
                if (cn.player.name().equalsIgnoreCase(best.name())) {
                    cn.touch();
                    break;
                }
            }
            return Optional.of(best);
        }

        return Optional.empty();
    }

    /** All nicks that appear inside the text (longest first). */
    public List<PlayerContext> findIn(String text) {

        List<PlayerContext> result = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return result;
        }

        String lower = text.toLowerCase();

        for (CachedNick cached : nicks.values()) {

            String name = cached.player.name().toLowerCase();

            if (containsWord(lower, name)) {
                cached.touch();
                result.add(cached.player);
            }
        }

        result.sort(
                (a, b) -> b.name().length() - a.name().length()
        );

        return result;
    }

    /** Removes entries not accessed within the stale threshold. */
    public void cleanupStaleEntries() {
        long now = System.currentTimeMillis();
        long cutoff = now - STALE_THRESHOLD_MS;

        nicks.entrySet().removeIf(entry ->
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
        if (!nicks.isEmpty()) {
            String oldest = nicks.keySet().iterator().next();
            nicks.remove(oldest);
        }
    }

    private static boolean containsWord(String text, String name) {

        int index = text.indexOf(name);

        while (index >= 0) {

            int end = index + name.length();

            boolean left =
                    index == 0 ||
                            !TextNormalizer.isNameChar(
                                    text.charAt(index - 1)
                            );

            boolean right =
                    end >= text.length() ||
                            !TextNormalizer.isNameChar(
                                    text.charAt(end)
                            );

            if (left && right) {
                return true;
            }

            index = text.indexOf(name, index + 1);
        }

        return false;
    }

    private static int levenshtein(String a, String b) {

        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];

        for (int j = 0; j <= b.length(); j++) {
            previous[j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {

            current[0] = i;

            for (int j = 1; j <= b.length(); j++) {

                int cost = a.charAt(i - 1) == b.charAt(j - 1)
                        ? 0
                        : 1;

                current[j] = Math.min(
                        Math.min(
                                current[j - 1] + 1,
                                previous[j] + 1
                        ),
                        previous[j - 1] + cost
                );
            }

            int[] swap = previous;
            previous = current;
            current = swap;
        }

        return previous[b.length()];
    }

    /** Same offline-style UUID scheme as {@code ChatTextProcessor}. */
    private static UUID offlineUuid(String name) {
        return UUID.nameUUIDFromBytes(
                ("OfflinePlayer:" + name)
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
    }
}
