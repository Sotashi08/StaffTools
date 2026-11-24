package com.uwdie.stafftools.client.macro;

import com.uwdie.stafftools.client.player.PlayerContext;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Resolves a command template against example values, so the editor can
 * show what the command will actually look like before it is run.
 *
 * <p>The preview never touches the live game state: it uses a fixed
 * sample player and sample coordinates, which keeps the preview stable
 * while the user types and makes it obvious which parts are dynamic.
 */
public final class CommandPreview {

    /** Sample nick used in the preview. */
    public static final String SAMPLE_PLAYER = "Notch";

    private CommandPreview() {
    }

    /**
     * Resolves {@code template} with example values.
     *
     * @param template raw command, may contain placeholders
     * @param alias    alias value to substitute, or null
     * @return the command as it would be sent, or an empty string
     */
    public static String resolve(
            String template,
            String alias
    ) {

        if (template == null || template.isBlank()) {
            return "";
        }

        String withAlias = alias == null || alias.isBlank()
                ? template
                : template.replace(
                        MacroExecutor.ALIAS_TOKEN,
                        alias.trim()
                );

        return PlaceholderEngine.resolve(
                withAlias,
                sampleContext()
        ).replaceAll(" {2,}", " ").trim();
    }

    /** Example context: a sample player, staff and coordinates. */
    private static MacroContext sampleContext() {

        Map<String, String> variables = new LinkedHashMap<>();

        variables.put("player", SAMPLE_PLAYER);
        variables.put("staff", "Staff");
        variables.put("x", "128");
        variables.put("y", "64");
        variables.put("z", "-256");
        variables.put("ping", "42");
        variables.put("server", "play.example.net");

        return MacroContext.of(
                new PlayerContext(
                        SAMPLE_PLAYER,
                        java.util.UUID.nameUUIDFromBytes(
                                ("OfflinePlayer:" + SAMPLE_PLAYER)
                                        .getBytes(
                                                java.nio.charset.StandardCharsets.UTF_8
                                        )
                        )
                ),
                variables
        );
    }
}
