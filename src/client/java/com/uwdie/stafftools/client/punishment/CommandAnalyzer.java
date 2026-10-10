package com.uwdie.stafftools.client.punishment;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analyzes a chat command and builds a punishment record when the command
 * looks like a known punishment (ban/mute/kick/warn/freeze/unban/unmute).
 *
 * Also resolves the outcome of a punishment from the server reply, using
 * several independent signals so different plugins still match.
 */
public final class CommandAnalyzer {

    /**
     * Detects the issuing staff member in server broadcasts like
     * "Player was banned by Admin" so punishments issued by OTHER
     * staff members never resolve our pending records.
     */
    private static final Pattern ISSUER = Pattern.compile(
            "(?:by|от|модератором|администратором|модером|админом|игроком)"
                    + "\\s+([a-z0-9_]{2,16})",
            Pattern.CASE_INSENSITIVE
    );

    /** Anything matching these means the command was REJECTED. */
    private static final String[] FAILURE_KEYWORDS = {
            "not found",
            "unknown player",
            "player does not exist",
            "player not online",
            "player not found",
            "cannot find",
            "no such player",
            "insufficient permission",
            "no permission",
            "missing permission",
            "unknown command",
            "invalid usage",
            "wrong usage",
            "incorrect usage",
            "only players",
            "only in-game",
            "not allowed",
            "forbidden",
            "denied",
            "error",
            "failed",
            "не найден",
            "не существует",
            "нет такого",
            "не найден на сервере",
            "не найден онлайн",
            "недостаточно прав",
            "нет прав",
            "не хватает прав",
            "некорректная команда",
            "неверная команда",
            "неизвестная команда",
            "ошибка",
            "отказано",
            "запрещено",
            "нельзя",
            "не указан",
            "укажите игрока",
            "usage",
            "использование"
    };

    /** Generic success wording, plugin independent. */
    private static final String[] SUCCESS_KEYWORDS = {
            "успешно",
            "успех",
            "выполнено",
            "выполнен",
            "готово",
            "success",
            "successfully",
            "completed",
            "done",
            "applied"
    };

    /**
     * Never treat these as a permission failure: some plugins quote them
     * inside a normal confirmation ("player not found in cache, banned").
     */
    private static final String[] FALSE_FAILURE = {
            "not found in cache",
            "не найден в кэше"
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

        for (String noise : FALSE_FAILURE) {

            if (lowerText.contains(noise)) {
                return false;
            }
        }

        for (String keyword : FAILURE_KEYWORDS) {

            if (lowerText.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    /**
     * How strongly the reply confirms this punishment type: phrase
     * matches, outcome mentions and generic success wording all count.
     */
    public static int confirmationScore(
            String lowerText,
            PunishmentType type
    ) {

        int score = type.confirmationScore(lowerText);

        for (String keyword : SUCCESS_KEYWORDS) {

            if (lowerText.contains(keyword)) {

                score += 1;
                break;
            }
        }

        return score;
    }

    /**
     * Resolves the outcome of a command from its reply. Returns
     * {@code DONE}, {@code FAILED} or null when nothing conclusive was
     * found — a null result leaves the record pending.
     */
    public static PunishmentStatus resolveOutcome(
            String lowerText,
            PunishmentType type
    ) {

        // a rejection first: usage/error text often quotes the command
        // itself, which would otherwise read as a success
        if (isFailure(lowerText)) {
            return PunishmentStatus.FAILED;
        }

        if (confirmationScore(lowerText, type) > 0) {
            return PunishmentStatus.DONE;
        }

        return null;
    }

    /**
     * Loose nick lookup for server replies. Servers wrap nicks in every
     * kind of decoration (§-codes, brackets, arrows), so after the plain
     * check fails the text is stripped of anything that is not part of
     * a nick and compared again.
     */
    public static boolean containsName(
            String lowerText,
            String playerName
    ) {

        String name = normalize(playerName);

        if (name.isEmpty()) {
            return false;
        }

        if (lowerText.contains(name)) {
            return true;
        }

        // too short to risk a stripped comparison
        if (name.length() < 3) {
            return false;
        }

        String bare = name.replaceAll("[^a-z0-9_]", "");

        if (bare.length() < 3) {
            return false;
        }

        return lowerText
                .replaceAll("[^a-z0-9_]", "")
                .contains(bare);
    }

    /** Same nick, ignoring case and decorations. */
    public static boolean sameName(String a, String b) {

        if (a == null || b == null) {
            return false;
        }

        return normalize(a)
                .replaceAll("[^a-z0-9_]", "")
                .equals(
                        normalize(b)
                                .replaceAll("[^a-z0-9_]", "")
                );
    }

    /**
     * Returns the staff member name mentioned as the punishment issuer,
     * or null when the message does not name one.
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
