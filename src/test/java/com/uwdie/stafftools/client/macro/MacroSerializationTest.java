package com.uwdie.stafftools.client.macro;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MacroSerializationTest {

    private final Gson gson = new Gson();

    @Test
    void missingLegacyAliasFieldsAreSafe() {

        Macro macro = gson.fromJson(
                """
                        {
                          "id": "00000000-0000-0000-0000-000000000001",
                          "name": "Mute",
                          "commands": ["/mute <player> <alias>"]
                        }
                        """,
                Macro.class
        );

        assertEquals(
                List.of("/mute <player> <alias>"),
                macro.getCommands()
        );
        assertEquals(List.of(), macro.getAliases());
    }

    @Test
    void aliasPickerVariantsSurviveDeserialization() {

        Macro macro = gson.fromJson(
                """
                        {
                          "id": "00000000-0000-0000-0000-000000000001",
                          "name": "Mute",
                          "commands": ["/mute <player> <alias>"],
                          "aliases": ["10m", "1h"]
                        }
                        """,
                Macro.class
        );

        assertEquals(
                List.of("/mute <player> <alias>"),
                macro.getCommands()
        );
        assertEquals(List.of("10m", "1h"), macro.getAliases());
    }
}
