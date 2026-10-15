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
    private final Map<String, PlayerContext> nicks =
            new LinkedHashMap<>();

    private NickCache() {
    }

    public static NickCache get() {
        return INSTANCE;
    }

    /** Drops everything remembered from the current server session. */
    public void clear() {
        nicks.clear();
    }

    public void remember(String name) {

        if (name == null || name.isBlank()) {
            return;
        }

        String key = name.toLowerCase();

        if (nicks.containsKey(key)) {
            return;
        }

        nicks.put(
                key,
                new PlayerContext(name, offlineUuid(name))
        );

        if (nicks.size() > 600) {

            String oldest = nicks.keySet().iterator().next();
            nicks.remove(oldest);
        }
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

        return Optional.ofNullable(
                nicks.get(name.trim().toLowerCase())
        );
    }

    /** Best nick that differs from the target by at most one edit. */
    public Optional<PlayerContext> resolveFuzzy(String name) {

        if (name == null || name.length() < 4) {
            return Optional.empty();
        }

        String target = name.trim().toLowerCase();

        PlayerContext best = null;
        int bestDistance = Integer.MAX_VALUE;

        for (Map.Entry<String, PlayerContext> entry :
                nicks.entrySet()) {

            String candidate = entry.getKey();

            if (Math.abs(candidate.length() - target.length()) > 1) {
                continue;
            }

            int distance = levenshtein(target, candidate);

            if (distance < bestDistance) {
                bestDistance = distance;
                best = entry.getValue();
            }
        }

        return bestDistance <= 1
                ? Optional.ofNullable(best)
                : Optional.empty();
    }

    /** All nicks that appear inside the text (longest first). */
    public List<PlayerContext> findIn(String text) {

        List<PlayerContext> result = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return result;
        }

        String lower = text.toLowerCase();

        for (PlayerContext player : nicks.values()) {

            String name = player.name().toLowerCase();

            if (containsWord(lower, name)) {
                result.add(player);
            }
        }

        result.sort(
                (a, b) -> b.name().length() - a.name().length()
        );

        return result;
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
