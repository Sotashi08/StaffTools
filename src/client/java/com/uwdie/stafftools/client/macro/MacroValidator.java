package com.uwdie.stafftools.client.macro;

import com.uwdie.stafftools.client.player.PlayerContext;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates macro/action commands before execution to prevent accidental
 * or malicious command injection.
 *
 * Custom server commands are accepted by default. Only explicitly
 * privileged commands are denied; basic formatting and target checks
 * remain in place for known moderation commands.
 */
public final class MacroValidator {

    /** Privileged commands which must never be run from a macro. */
    private static final Set<String> DENIED_COMMANDS = Set.of(
            "op", "deop",
            "give", "item", "enchant",
            "effect", "attribute",
            "execute", "function", "schedule",
            "data", "nbt", "loot",
            "advancement", "recipe", "tag",
            "team", "scoreboard",
            "worldborder", "spawnpoint", "setworldspawn",
            "difficulty", "gamerule", "defaultgamemode",
            "save-all", "save-off", "save-on",
            "stop", "restart", "reload",
            "whitelist", "banlist", "pardon",
            "permission", "permissions", "lp", "luckperms",
            "sudo", "fakeplayer", "npc",
            "commandblock", "structure", "clone", "fill",
            "summon", "kill",
            "tellraw", "title", "bossbar",
            "particle", "playsound", "stopsound",
            "camera", "cinematic", "fade",
            "trigger"
    );

    private static final Set<String> CREATIVE_GAMEMODE_ARGUMENTS =
            Set.of("creative", "c", "1");

    /** Commands whose arguments must still match the selected player. */
    private static final Set<String> TARGET_CHECK_COMMANDS = Set.of(
            "mute", "unmute", "tempmute",
            "ban", "unban", "tempban",
            "kick", "warn", "unwarn",
            "freeze", "unfreeze"
    );

    /** Characters that are never allowed in aliases. */
    private static final Pattern ALIAS_DANGEROUS_CHARS = Pattern.compile(
            "[;&|`$\\\\<>/]"
    );

    /** Maximum length for an alias. */
    private static final int MAX_ALIAS_LENGTH = 128;

    /** Maximum length for a resolved command. */
    private static final int MAX_COMMAND_LENGTH = 512;

    private MacroValidator() {
    }

    /**
     * Validates a list of commands before execution.
     *
     * @param commands  the commands to validate (after placeholder resolution)
     * @param target    the target player context
     * @param aliasUsed the alias that was substituted (if any)
     * @return ValidationResult with success/failure and details
     */
    public static ValidationResult validateCommands(
            List<String> commands,
            PlayerContext target,
            String aliasUsed
    ) {

        if (commands == null) {
            return ValidationResult.failure("No commands to execute");
        }

        // 1. Validate alias if provided
        if (aliasUsed != null && !aliasUsed.isBlank()) {
            ValidationResult aliasResult = validateAlias(aliasUsed);
            if (!aliasResult.isSuccess()) {
                return aliasResult;
            }
        }

        // 2. Validate each command
        boolean hasCommand = false;

        for (String command : commands) {
            if (command == null || command.isBlank()) {
                continue;
            }

            hasCommand = true;

            ValidationResult cmdResult = validateCommand(command);
            if (!cmdResult.isSuccess()) {
                return cmdResult;
            }
        }

        if (!hasCommand) {
            return ValidationResult.failure("No commands to execute");
        }

        // 3. Nick verification - cross-check that the target player
        //    matches known moderation commands. Unknown server commands
        //    do not have a standard argument layout, so do not reject them.
        ValidationResult nickResult = verifyNickMatch(commands, target);
        if (!nickResult.isSuccess()) {
            return nickResult;
        }

        return ValidationResult.success();
    }

    /** Validates a single command against the denylist and basic format. */
    private static ValidationResult validateCommand(String command) {

        if (command.chars().anyMatch(
                c -> Character.isISOControl(c) && c != '\t'
        )) {
            return ValidationResult.failure(
                    "Command contains control characters"
            );
        }

        String trimmed = command.trim();

        // Length check
        if (trimmed.length() > MAX_COMMAND_LENGTH) {
            return ValidationResult.failure(
                    "Command too long (max " + MAX_COMMAND_LENGTH + " chars): " + trimmed
            );
        }

        // Must start with /
        if (!trimmed.startsWith("/")) {
            return ValidationResult.failure(
                    "Command must start with '/': " + trimmed
            );
        }

        ParsedCommand parsed = parseCommand(trimmed);

        if (parsed == null) {
            return ValidationResult.failure(
                    "Command has an invalid name: " + trimmed
            );
        }

        if (DENIED_COMMANDS.contains(parsed.root())) {
            return ValidationResult.failure(
                    "Command blocked by denylist: /" + parsed.root()
            );
        }

        if (parsed.root().equals("gamemode") &&
                CREATIVE_GAMEMODE_ARGUMENTS.contains(
                        parsed.firstArgument()
                )) {

            return ValidationResult.failure(
                    "Command blocked by denylist: /gamemode "
                            + parsed.firstArgument()
            );
        }

        return ValidationResult.success();
    }

    /** Validates an alias before it is substituted into a command. */
    private static ValidationResult validateAlias(String alias) {

        if (alias.length() > MAX_ALIAS_LENGTH) {
            return ValidationResult.failure(
                    "Alias too long (max " + MAX_ALIAS_LENGTH + " chars)"
            );
        }

        // Check for dangerous characters
        if (ALIAS_DANGEROUS_CHARS.matcher(alias).find()) {
            return ValidationResult.failure(
                    "Alias contains forbidden characters (; & | ` $ \\ < > /): "
                            + alias
            );
        }

        // No newlines or control chars
        if (alias.chars().anyMatch(c -> c < 32 && c != '\t')) {
            return ValidationResult.failure(
                    "Alias contains control characters"
            );
        }

        return ValidationResult.success();
    }

    /**
     * Verifies that the nick in the resolved command matches the target player.
     * This prevents executing a command on the wrong player due to detection errors.
     *
     * Strategy: Extract player names from the command and verify at least one
     * matches the target (case-insensitive, exact or prefix match).
     */
    private static ValidationResult verifyNickMatch(
            List<String> commands,
            PlayerContext target
    ) {

        if (target == null ||
                target.name() == null ||
                target.name().isBlank()) {

            return ValidationResult.success();
        }

        String targetName = target.name().toLowerCase(Locale.ROOT);
        boolean checkRequired = false;

        for (String command : commands) {

            if (command == null || command.isBlank()) {
                continue;
            }

            ParsedCommand parsed = parseCommand(command.trim());

            if (parsed == null ||
                    !TARGET_CHECK_COMMANDS.contains(parsed.root())) {

                continue;
            }

            checkRequired = true;

            // Extract potential player names from command arguments
            String[] parts = command.trim().split("\\s+");

            // Check each argument (skip the command itself)
            for (int i = 1; i < parts.length; i++) {
                String arg = parts[i].toLowerCase(Locale.ROOT);

                // Skip if it looks like a flag, number, or time duration
                if (arg.startsWith("-") ||
                        arg.matches("^\\d+[smhd]?$") ||  // 10m, 30s, 1h, 2d
                        arg.matches("^\\d+$") ||          // pure number
                        arg.equals("true") || arg.equals("false")) {
                    continue;
                }

                // Check if this argument matches our target
                if (arg.equals(targetName) ||
                        arg.startsWith(targetName) ||
                        targetName.startsWith(arg)) {
                    return ValidationResult.success();
                }
            }
        }

        if (!checkRequired) {
            return ValidationResult.success();
        }

        // No matching nick found in any command - this is suspicious
        return ValidationResult.failure(
                "Nick verification failed: command does not appear to target '" +
                        target.name() + "'. Detected nick mismatch - possible detection error."
        );
    }

    private static ParsedCommand parseCommand(String command) {

        if (command.isEmpty() || !command.startsWith("/")) {
            return null;
        }

        String body = command.substring(1).stripLeading();

        if (body.isEmpty() || body.startsWith("/")) {
            return null;
        }

        int rootEnd = 0;

        while (rootEnd < body.length() &&
                !Character.isWhitespace(body.charAt(rootEnd))) {

            rootEnd++;
        }

        String label = body.substring(0, rootEnd);
        int namespaceSeparator = label.lastIndexOf(':');
        String root = namespaceSeparator < 0
                ? label
                : label.substring(namespaceSeparator + 1);

        if (root.isEmpty()) {
            return null;
        }

        String arguments = body.substring(rootEnd).stripLeading();
        int argumentEnd = 0;

        while (argumentEnd < arguments.length() &&
                !Character.isWhitespace(arguments.charAt(argumentEnd))) {

            argumentEnd++;
        }

        return new ParsedCommand(
                root.toLowerCase(Locale.ROOT),
                arguments.substring(0, argumentEnd)
                        .toLowerCase(Locale.ROOT)
        );
    }

    private record ParsedCommand(
            String root,
            String firstArgument
    ) {
    }

    /**
     * Sanitizes an alias by removing dangerous characters.
     * Returns the sanitized alias, or empty string if it becomes empty.
     */
    public static String sanitizeAlias(String alias) {
        if (alias == null) return "";

        String sanitized = ALIAS_DANGEROUS_CHARS.matcher(alias).replaceAll("");
        sanitized = sanitized.replaceAll("[\\x00-\\x1F\\x7F]", ""); // control chars
        sanitized = sanitized.trim();

        if (sanitized.length() > MAX_ALIAS_LENGTH) {
            sanitized = sanitized.substring(0, MAX_ALIAS_LENGTH);
        }

        return sanitized;
    }

    /**
     * Result of a validation check.
     */
    public static final class ValidationResult {
        private final boolean success;
        private final String message;

        private ValidationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}