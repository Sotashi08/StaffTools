package com.uwdie.stafftools.client.macro;

import com.uwdie.stafftools.client.audit.AuditCategory;
import com.uwdie.stafftools.client.audit.AuditLog;
import com.uwdie.stafftools.client.audit.Profiler;
import com.uwdie.stafftools.client.player.PlayerContext;
import com.uwdie.stafftools.client.punishment.PunishmentHistory;
import com.uwdie.stafftools.client.punishment.PunishmentRecord;
import com.uwdie.stafftools.client.punishment.PunishmentRecord.SourceKind;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public final class MacroExecutor {

    /** Placeholder that turns a command into an alias picker flow. */
    public static final String ALIAS_TOKEN = "<alias>";

    /** Placeholder replaced by a reason the user types in a modal. */
    public static final String REASON_TOKEN = "<reason>";

    private MacroExecutor() {
    }

    public static void execute(
            Macro macro,
            PlayerContext target
    ) {

        if (macro == null) {
            return;
        }

        if (!macro.isEnabled()) {

            AuditLog.get().warn(
                    AuditCategory.MACRO,
                    "Macro skipped (disabled): " + macro.getName()
            );

            return;
        }

        executeCommands(
                macro.getCommands(),
                target,
                macro.getName(),
                SourceKind.MACRO,
                null
        );
    }

    /**
     * True if any of the macro's commands contain the {@code <alias>}
     * placeholder and therefore need a user-picked variant.
     */
    public static boolean requiresAlias(Macro macro) {

        return macro != null &&
                requiresAlias(macro.getCommands());
    }

    /** Same check for a bare command list (player actions). */
    public static boolean requiresAlias(List<String> commands) {

        if (commands == null) {
            return false;
        }

        for (String command : commands) {

            if (command != null &&
                    command.contains(ALIAS_TOKEN)) {

                return true;
            }
        }

        return false;
    }

    /**
     * Executes the macro with {@code <alias>} replaced by the picked
     * variant BEFORE regular placeholder resolution. An empty alias is
     * allowed; leftover double spaces are collapsed.
     */
    public static void executeWithAlias(
            Macro macro,
            PlayerContext target,
            String alias
    ) {

        if (macro == null) {
            return;
        }

        if (!macro.isEnabled()) {

            AuditLog.get().warn(
                    AuditCategory.MACRO,
                    "Macro skipped (disabled): " + macro.getName()
            );

            return;
        }

        // Sanitize alias before use
        String safeAlias = MacroValidator.sanitizeAlias(alias);

        executeCommands(
                substituteAlias(
                        macro.getCommands(),
                        safeAlias
                ),
                target,
                macro.getName(),
                SourceKind.MACRO,
                safeAlias
        );
    }

    /** Runs a player action, tagging the history as an ACTION. */
    public static void executeAction(
            String command,
            PlayerContext target,
            String actionLabel
    ) {

        executeCommands(
                List.of(command == null ? "" : command),
                target,
                actionLabel,
                SourceKind.ACTION,
                null
        );
    }

    /**
     * True if any command contains the {@code <reason>} placeholder and
     * therefore needs a typed reason before execution.
     */
    public static boolean requiresReason(Macro macro) {

        return macro != null && requiresReason(macro.getCommands());
    }

    /** Same check for a bare command list (player actions). */
    public static boolean requiresReason(List<String> commands) {

        if (commands == null) {
            return false;
        }

        for (String command : commands) {

            if (command != null &&
                    command.contains(REASON_TOKEN)) {

                return true;
            }
        }

        return false;
    }

    /**
     * Executes a macro with {@code <reason>} replaced by the typed reason
     * BEFORE regular placeholder resolution. An empty reason is allowed;
     * leftover double spaces are collapsed.
     */
    public static void executeWithReason(
            Macro macro,
            PlayerContext target,
            String reason
    ) {

        if (macro == null) {
            return;
        }

        if (!macro.isEnabled()) {

            AuditLog.get().warn(
                    AuditCategory.MACRO,
                    "Macro skipped (disabled): " + macro.getName()
            );

            return;
        }

        executeCommands(
                substituteReason(macro.getCommands(), reason),
                target,
                macro.getName(),
                SourceKind.MACRO,
                null
        );
    }

    /** Runs a player action whose command carries {@code <reason>}. */
    public static void executeActionWithReason(
            String command,
            PlayerContext target,
            String reason,
            String actionLabel
    ) {

        executeCommands(
                substituteReason(
                        List.of(command == null ? "" : command),
                        reason
                ),
                target,
                actionLabel,
                SourceKind.ACTION,
                null
        );
    }

    /** Replaces {@code <reason>} in every command and tidies spaces. */
    public static List<String> substituteReason(
            List<String> commands,
            String reason
    ) {

        String safeReason = reason == null ? "" : reason.trim();

        List<String> out = new ArrayList<>();

        for (String command : commands) {

            if (command == null) {
                continue;
            }

            out.add(
                    command
                            .replace(REASON_TOKEN, safeReason)
                            .replaceAll(" {2,}", " ")
                            .trim()
            );
        }

        return out;
    }

    /** Runs a player action whose command carries {@code <alias>}. */
    public static void executeActionWithAlias(
            String command,
            PlayerContext target,
            String alias,
            String actionLabel
    ) {

        // Sanitize alias before use
        String safeAlias = MacroValidator.sanitizeAlias(alias);

        executeCommands(
                substituteAlias(
                        List.of(command == null ? "" : command),
                        safeAlias
                ),
                target,
                actionLabel,
                SourceKind.ACTION,
                safeAlias
        );
    }

    /** Replaces {@code <alias>} in every command and tidies spaces. */
    public static List<String> substituteAlias(
            List<String> commands,
            String alias
    ) {

        String safeAlias =
                alias == null ? "" : alias.trim();

        List<String> out = new ArrayList<>();

        for (String command : commands) {

            if (command == null) {
                continue;
            }

            out.add(
                    command
                            .replace(ALIAS_TOKEN, safeAlias)
                            .replaceAll(" {2,}", " ")
                            .trim()
            );
        }

        return out;
    }

    public static void executeCommands(
            List<String> rawCommands,
            PlayerContext target
    ) {

        executeCommands(
                rawCommands,
                target,
                null,
                SourceKind.MANUAL,
                null
        );
    }

    /**
     * {@code sourceName} / {@code sourceKind} are recorded on every
     * punishment the commands produce, so the history can tell macros
     * and actions apart.
     *
     * @param aliasUsed the alias that was substituted (if any), for validation
     */
    public static void executeCommands(
            List<String> rawCommands,
            PlayerContext target,
            String sourceName,
            SourceKind sourceKind,
            String aliasUsed
    ) {

        MinecraftClient client =
                MinecraftClient.getInstance();

        if (client.player == null ||
                client.getNetworkHandler() == null) {

            AuditLog.get().warn(
                    AuditCategory.MACRO,
                    "Execution aborted (not in a world): "
                            + describe(sourceName, sourceKind)
            );

            return;
        }

        long startedAt = System.nanoTime();

        PunishmentHistory.get()
                .setPendingSource(sourceName, sourceKind);

        MacroContext context =
                MacroContext.create(target);

        List<String> resolvedCommands = new ArrayList<>();

        for (String raw :
                rawCommands) {

            if (raw == null ||
                    raw.isBlank()) {

                continue;
            }

            String command =
                    PlaceholderEngine.resolve(
                            raw,
                            context
                    ).trim();

            if (command.isEmpty()) {
                continue;
            }

            resolvedCommands.add(command);
        }

        // Validate all resolved commands before execution (commands still have leading /)
        MacroValidator.ValidationResult validation =
                MacroValidator.validateCommands(
                        resolvedCommands,
                        target,
                        aliasUsed
                );

        if (!validation.isSuccess()) {

            AuditLog.get().error(
                    AuditCategory.MACRO,
                    "Validation failed for "
                            + describe(sourceName, sourceKind)
                            + ": " + validation.getMessage()
            );

            return;
        }

        // Execute validated commands (strip leading / for sendChatCommand)
        for (String command : resolvedCommands) {
            String toSend = command;
            if (toSend.startsWith("/")) {
                toSend = toSend.substring(1);
            }
            client.getNetworkHandler()
                    .sendChatCommand(toSend);
        }

        Profiler.get().record(
                Profiler.MACRO_EXEC,
                System.nanoTime() - startedAt
        );

        AuditLog.get().success(
                AuditCategory.MACRO,
                "Ran " + describe(sourceName, sourceKind)
                        + (target == null ? "" : " -> " + target.name())
                        + (aliasUsed == null || aliasUsed.isBlank()
                        ? ""
                        : " [" + aliasUsed + "]")
                        + ": " + String.join(" | ", resolvedCommands)
        );
    }

    /** Human readable source label for the audit journal. */
    private static String describe(
            String sourceName,
            SourceKind sourceKind
    ) {

        String kind = sourceKind == null
                ? "command"
                : sourceKind.name().toLowerCase();

        return sourceName == null || sourceName.isBlank()
                ? kind
                : kind + " '" + sourceName + "'";
    }
}
