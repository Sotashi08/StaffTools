package com.uwdie.stafftools.client.ui;

import com.uwdie.stafftools.client.punishment.PunishmentRecord;
import com.uwdie.stafftools.client.punishment.PunishmentStatus;
import com.uwdie.stafftools.client.punishment.PunishmentType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Aggregated view over the punishment history, used by the history
 * screen to show summary numbers next to the log itself.
 */
public final class HistoryStats {

    private final int total;
    private final int confirmed;
    private final int failed;
    private final int awaiting;
    private final int unanswered;

    private final int uniquePlayers;
    private final String topPlayer;
    private final int topPlayerCount;

    private final int moderators;
    private final int fromMacros;
    private final int fromActions;

    /** Punishment types ordered by how often they were used. */
    private final Map<PunishmentType, Integer> byType;

    private HistoryStats(
            int total,
            int confirmed,
            int failed,
            int awaiting,
            int unanswered,
            int uniquePlayers,
            String topPlayer,
            int topPlayerCount,
            int moderators,
            int fromMacros,
            int fromActions,
            Map<PunishmentType, Integer> byType
    ) {

        this.total = total;
        this.confirmed = confirmed;
        this.failed = failed;
        this.awaiting = awaiting;
        this.unanswered = unanswered;
        this.uniquePlayers = uniquePlayers;
        this.topPlayer = topPlayer;
        this.topPlayerCount = topPlayerCount;
        this.moderators = moderators;
        this.fromMacros = fromMacros;
        this.fromActions = fromActions;
        this.byType = byType;
    }

    public static HistoryStats of(
            List<PunishmentRecord> records
    ) {

        int confirmed = 0;
        int failed = 0;
        int awaiting = 0;
        int unanswered = 0;
        int fromMacros = 0;
        int fromActions = 0;

        // case-insensitive keys: "Notch" and "notch" are one player
        Map<String, Integer> players = new TreeMap<>(
                String.CASE_INSENSITIVE_ORDER
        );

        Map<String, Integer> moderators = new TreeMap<>(
                String.CASE_INSENSITIVE_ORDER
        );

        Map<PunishmentType, Integer> byType =
                new LinkedHashMap<>();

        for (PunishmentType type : PunishmentType.values()) {
            byType.put(type, 0);
        }

        for (PunishmentRecord record : records) {

            switch (record.getEffectiveStatus()) {

                case DONE -> confirmed++;
                case FAILED -> failed++;
                case PENDING -> awaiting++;
                case NO_RESPONSE -> unanswered++;
            }

            if (record.getMacroName() != null &&
                    !record.getMacroName().isBlank()) {

                if (record.getSourceKind()
                        == PunishmentRecord.SourceKind.MACRO) {

                    fromMacros++;

                } else if (record.getSourceKind()
                        == PunishmentRecord.SourceKind.ACTION) {

                    fromActions++;
                }
            }

            String player = record.getPlayerName();

            if (player != null && !player.isBlank()) {

                players.merge(player, 1, Integer::sum);
            }

            String staff = record.getModeratorOrNull();

            if (staff != null) {
                moderators.merge(staff, 1, Integer::sum);
            }

            byType.merge(
                    record.getType(),
                    1,
                    Integer::sum
            );
        }

        String topPlayer = null;
        int topPlayerCount = 0;

        for (Map.Entry<String, Integer> entry :
                players.entrySet()) {

            if (entry.getValue() > topPlayerCount) {

                topPlayer = entry.getKey();
                topPlayerCount = entry.getValue();
            }
        }

        // most used first; ties keep the enum order
        List<Map.Entry<PunishmentType, Integer>> sorted =
                new ArrayList<>(byType.entrySet());

        sorted.sort(
                Comparator
                        .comparingInt(
                                (Map.Entry<PunishmentType, Integer> e)
                                        -> -e.getValue()
                        )
                        .thenComparingInt(
                                e -> e.getKey().ordinal()
                        )
        );

        Map<PunishmentType, Integer> ordered = new LinkedHashMap<>();

        for (Map.Entry<PunishmentType, Integer> entry : sorted) {

            if (entry.getValue() > 0) {
                ordered.put(entry.getKey(), entry.getValue());
            }
        }

        return new HistoryStats(
                records.size(),
                confirmed,
                failed,
                awaiting,
                unanswered,
                players.size(),
                topPlayer,
                topPlayerCount,
                moderators.size(),
                fromMacros,
                fromActions,
                ordered
        );
    }

    public int getTotal() {
        return total;
    }

    public int getConfirmed() {
        return confirmed;
    }

    public int getFailed() {
        return failed;
    }

    public int getAwaiting() {
        return awaiting;
    }

    public int getUnanswered() {
        return unanswered;
    }

    public int getUniquePlayers() {
        return uniquePlayers;
    }

    /** Most punished nick, or null when the log is empty. */
    public String getTopPlayer() {
        return topPlayer;
    }

    public int getTopPlayerCount() {
        return topPlayerCount;
    }

    public int getModerators() {
        return moderators;
    }

    public int getFromMacros() {
        return fromMacros;
    }

    public int getFromActions() {
        return fromActions;
    }

    /** Types with at least one entry, most used first. */
    public Map<PunishmentType, Integer> getByType() {
        return byType;
    }

    /** One-line breakdown of the type counts, e.g. "Бан 3 · Мут 2". */
    public String typeBreakdown() {

        if (byType.isEmpty()) {
            return null;
        }

        StringBuilder line = new StringBuilder();

        for (Map.Entry<PunishmentType, Integer> entry :
                byType.entrySet()) {

            if (line.length() > 0) {
                line.append(" · ");
            }

            line.append(
                    entry.getKey().getLabel()
                            .toLowerCase(Locale.ROOT)
            );

            line.append(' ')
                    .append(entry.getValue());
        }

        return line.toString();
    }

    /** Most punished nick with its count, or null when empty. */
    public String topPlayerLabel() {

        if (topPlayer == null) {
            return null;
        }

        return topPlayer + " (" + topPlayerCount + ")";
    }
}
