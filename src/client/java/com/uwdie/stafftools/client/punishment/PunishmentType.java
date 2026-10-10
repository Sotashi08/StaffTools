package com.uwdie.stafftools.client.punishment;

import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;

import java.util.Locale;

/**
 * Punishment type with command aliases (for detection) and two grades of
 * response wording: full phrases (a strong signal) and bare stems (a weak
 * one). Aliases are matched by their longest form first, so {@code /unban}
 * is detected as an UNBAN and never as a BAN.
 */
public enum PunishmentType {

    UNBAN(
            Key.PUN_TYPE_UNBAN,
            "\uD83D\uDD13",
            new String[]{"unban", "unbanip", "pardon"},
            new String[]{"unbanned", "разбанен", "разбанена",
                    "снят бан", "был разбанен", "разблокирован"},
            new String[]{"разбан", "разблок", "pardon", "unblock"}
    ),
    UNMUTE(
            Key.PUN_TYPE_UNMUTE,
            "\uD83D\uDD0A",
            new String[]{"unmute", "unmuteall", "unsilence"},
            new String[]{"unmuted", "размучен", "размьючен",
                    "снят мут", "разглушён"},
            new String[]{"размут", "разглуш", "unsilence"}
    ),
    BAN(
            Key.PUN_TYPE_BAN,
            "\uD83D\uDD28",
            new String[]{"ban", "banip", "tempban", "ipban"},
            new String[]{"banned", "забанен", "забанена",
                    "забанен на", "был забанен", "заблокирован"},
            new String[]{"бан", "bann", "блокир", "block"}
    ),
    MUTE(
            Key.PUN_TYPE_MUTE,
            "\uD83D\uDD07",
            new String[]{"mute", "tempmute", "silence"},
            new String[]{"muted", "замучен", "замьючен",
                    "был замучен"},
            new String[]{"мут", "silence", "мьют"}
    ),
    KICK(
            Key.PUN_TYPE_KICK,
            "\uD83D\uDC62",
            new String[]{"kick"},
            new String[]{"kicked", "кикнут", "был кикнут",
                    "выгнан"},
            new String[]{"кик", "kick"}
    ),
    WARN(
            Key.PUN_TYPE_WARN,
            "⚠",
            new String[]{"warn"},
            new String[]{"warned", "предупреждён",
                    "был предупреждён"},
            new String[]{"варн", "warn", "пред"}
    ),
    FREEZE(
            Key.PUN_TYPE_FREEZE,
            "\uD83E\uDDCA",
            new String[]{"freeze", "vanish"},
            new String[]{"frozen", "заморожен", "был заморожен",
                    "заморожен на"},
            new String[]{"мороз", "freez", "заморож"}
    ),
    OTHER(
            Key.PUN_TYPE_OTHER,
            "✏",
            new String[0],
            new String[0],
            new String[0]
    );

    private final String labelKey;
    private final String icon;
    private final String[] commandAliases;

    /** Wording that almost certainly confirms THIS punishment. */
    private final String[] responsePhrases;

    /** Wording that merely points at this kind of punishment. */
    private final String[] responseStems;

    PunishmentType(
            String labelKey,
            String icon,
            String[] commandAliases,
            String[] responsePhrases,
            String[] responseStems
    ) {

        this.labelKey = labelKey;
        this.icon = icon;
        this.commandAliases = commandAliases;
        this.responsePhrases = responsePhrases;
        this.responseStems = responseStems;
    }

    public String getLabel() {
        return Lang.t(labelKey);
    }

    public String getIcon() {
        return icon;
    }

    public String getLabelKey() {
        return labelKey;
    }

    public boolean matchesCommand(String head) {

        String lower = head.toLowerCase(Locale.ROOT);

        for (String alias : commandAliases) {

            if (alias.equals(lower)) {
                return true;
            }
        }

        return false;
    }

    /** True when the reply uses one of the full confirmation phrases. */
    public boolean matchesResponse(String lowerText) {
        return scorePhrases(lowerText) > 0;
    }

    /** Confidence that the reply confirms this punishment: 0..4. */
    public int confirmationScore(String lowerText) {

        int score = scorePhrases(lowerText) * 3
                + scoreStems(lowerText);

        // "was banned", "has been kicked": the command itself named
        if (mentionsOutcome(lowerText)) {
            score += 2;
        }

        return score;
    }

    private int scorePhrases(String lowerText) {

        int hits = 0;

        for (String phrase : responsePhrases) {

            if (lowerText.contains(phrase)) {
                hits++;
            }
        }

        return hits;
    }

    private int scoreStems(String lowerText) {

        int hits = 0;

        for (String stem : responseStems) {

            if (lowerText.contains(stem)) {
                hits++;
            }
        }

        return hits;
    }

    /**
     * True when the reply carries the outcome of THIS command,
     * e.g. "Notch was unbanned" for an UNBAN.
     */
    public boolean mentionsOutcome(String lowerText) {

        for (String alias : commandAliases) {

            if (lowerText.contains(alias)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Detects the type of a command head. Longest alias wins, so
     * {@code unban} can never be read as a {@code ban}.
     */
    public static PunishmentType match(String commandHead) {

        if (commandHead == null) {
            return null;
        }

        String lower = commandHead.toLowerCase(Locale.ROOT);

        PunishmentType best = null;
        int bestLength = 0;

        for (PunishmentType type : values()) {

            for (String alias : type.commandAliases) {

                if (!alias.equals(lower)) {
                    continue;
                }

                if (alias.length() > bestLength) {

                    best = type;
                    bestLength = alias.length();
                }
            }
        }

        return best == OTHER ? null : best;
    }
}
