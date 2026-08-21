package com.uwdie.stafftools.client.punishment;

import com.uwdie.stafftools.client.i18n.Lang;
import com.uwdie.stafftools.client.i18n.Lang.Key;

import java.util.Locale;

/**
 * Punishment type with command aliases (for detection) and chat response
 * keywords (for matching the server reply).
 */
public enum PunishmentType {

    BAN(
            Key.PUN_TYPE_BAN,
            "🔨",
            new String[]{"ban", "banip", "tempban", "ipban", "unban"},
            new String[]{"banned", "забанен", "забанен на", "был забанен"}
    ),
    MUTE(
            Key.PUN_TYPE_MUTE,
            "🔇",
            new String[]{"mute", "tempmute", "muffle", "unmute"},
            new String[]{"muted", "замучен", "замьючен", "был замучен"}
    ),
    KICK(
            Key.PUN_TYPE_KICK,
            "👢",
            new String[]{"kick"},
            new String[]{"kicked", "кикнут", "был кикнут"}
    ),
    WARN(
            Key.PUN_TYPE_WARN,
            "⚠",
            new String[]{"warn"},
            new String[]{"warned", "предупреждён", "был предупреждён"}
    ),
    FREEZE(
            Key.PUN_TYPE_FREEZE,
            "🧊",
            new String[]{"freeze", "unfreeze", "vanish"},
            new String[]{"frozen", "заморожен", "был заморожен"}
    ),
    OTHER(
            Key.PUN_TYPE_OTHER,
            "✏",
            new String[0],
            new String[0]
    );

    private final String labelKey;
    private final String icon;
    private final String[] commandAliases;
    private final String[] responseKeywords;

    PunishmentType(
            String labelKey,
            String icon,
            String[] commandAliases,
            String[] responseKeywords
    ) {

        this.labelKey = labelKey;
        this.icon = icon;
        this.commandAliases = commandAliases;
        this.responseKeywords = responseKeywords;
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

    public boolean matchesResponse(String lowerText) {
        for (String keyword : responseKeywords) {
            if (lowerText.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    public static PunishmentType match(String commandHead) {
        for (PunishmentType type : values()) {
            if (type != OTHER && type.matchesCommand(commandHead)) {
                return type;
            }
        }
        return null;
    }
}
