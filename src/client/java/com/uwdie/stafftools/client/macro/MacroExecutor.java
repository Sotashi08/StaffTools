package com.uwdie.stafftools.client.macro;

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

    private MacroExecutor() {
    }

    public static void execute(
            Macro macro,
            PlayerContext target
    ) {

        if (macro == null ||
                !macro.isEnabled()) {

            return;
        }

        executeCommands(
                macro.getCommands(),
                target,
                macro.getName(),
                SourceKind.MACRO
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

        if (macro == null ||
                !macro.isEnabled()) {

            return;
        }

        executeCommands(
                substituteAlias(
                        macro.getCommands(),
                        alias
                ),
                target,
                macro.getName(),
                SourceKind.MACRO
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
                SourceKind.ACTION
        );
    }

    /** Runs a player action whose command carries {@code <alias>}. */
    public static void executeActionWithAlias(
            String command,
            PlayerContext target,
            String alias,
            String actionLabel
    ) {

        executeCommands(
                substituteAlias(
                        List.of(command == null ? "" : command),
                        alias
                ),
                target,
                actionLabel,
                SourceKind.ACTION
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
                SourceKind.MANUAL
        );
    }

    /**
     * {@code sourceName} / {@code sourceKind} are recorded on every
     * punishment the commands produce, so the history can tell macros
     * and actions apart.
     */
    public static void executeCommands(
            List<String> rawCommands,
            PlayerContext target,
            String sourceName,
            SourceKind sourceKind
    ) {

        MinecraftClient client =
                MinecraftClient.getInstance();

        if (client.player == null ||
                client.getNetworkHandler() == null) {

            return;
        }

        PunishmentHistory.get()
                .setPendingSource(sourceName, sourceKind);

        MacroContext context =
                MacroContext.create(target);

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

            if (command.startsWith("/")) {

                command =
                        command.substring(1);
            }

            client.getNetworkHandler()
                    .sendChatCommand(command);
        }
    }
}
