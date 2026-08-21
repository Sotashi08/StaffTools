package com.uwdie.stafftools.client.punishment;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analyzes a chat command and builds a punishment record when the command
 * looks like a known punishment (ban/mute/kick/warn/freeze).
 */
public final class CommandAnalyzer {

    /**
     * Detects the issuing staff member in server broadcasts like
     * "Player was banned by Admin" so punishments issued by OTHER
     * staff members never resolve our pending records.
     */
    private static final Pattern ISSUER = Pattern.compile(
            "(?:by|от|модератором|администратором|модером|админом)"
                    + "\\s+([a-z0-9_]{2,16})",
            Pattern.CASE_INSENSITIVE
    );

    private static final String[] FAILURE_KEYWORDS = {
            "not found",
            "unknown player",
            "player does not exist",
            "player not online",
            "не найден",
            "не существует",
            "нет такого",
            "не найден на сервере"
    };

    private CommandAnalyzer() {
    }

    public static PunishmentRecord analyze(String rawCommand) {

        if (rawCommand == null) {
            return null;
        }

        String command = rawCommand.trim();

        if (command.isEmpty()) {
            return null;
        }

        if (command.startsWith("/")) {
            command = command.substring(1);
        }

        String[] parts = command.split("\\s+");

        if (parts.length == 0) {
            return null;
        }

        PunishmentType type =
                PunishmentType.match(parts[0]);

        if (type == null) {
            return null;
        }

        String playerName = null;

        for (int i = 1; i < parts.length; i++) {

            if (parts[i].startsWith("-")) {
                continue;
            }

            playerName = parts[i];
            break;
        }

        return new PunishmentRecord(
                playerName,
                type,
                rawCommand.trim()
        );
    }

    public static boolean isFailure(String lowerText) {
        for (String keyword : FAILURE_KEYWORDS) {
            if (lowerText.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the staff member name mentioned as the punishment issuer,
     * or null if the message does not name one.
     */
    public static String extractIssuer(String lowerText) {

        if (lowerText == null) {
            return null;
        }

        Matcher matcher = ISSUER.matcher(lowerText);

        return matcher.find()
                ? matcher.group(1)
                : null;
    }

    public static String normalize(String text) {
        return text == null
                ? ""
                : text.toLowerCase(Locale.ROOT);
    }
}
