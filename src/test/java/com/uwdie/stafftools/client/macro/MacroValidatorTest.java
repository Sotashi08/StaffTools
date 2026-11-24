package com.uwdie.stafftools.client.macro;

import com.uwdie.stafftools.client.player.PlayerContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MacroValidator.
 */
class MacroValidatorTest {

    private static final UUID TEST_UUID = UUID.randomUUID();
    private static final PlayerContext TEST_PLAYER = new PlayerContext("Notch", TEST_UUID);

    @Test
    @DisplayName("Should allow valid mute command")
    void testValidMute() {
        List<String> commands = List.of("/mute Notch 10m");
        var result = MacroValidator.validateCommands(commands, TEST_PLAYER, null);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Should allow valid ban command")
    void testValidBan() {
        List<String> commands = List.of("/ban Notch Cheating");
        var result = MacroValidator.validateCommands(commands, TEST_PLAYER, null);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Should allow valid warn command")
    void testValidWarn() {
        List<String> commands = List.of("/warn Notch Spam");
        var result = MacroValidator.validateCommands(commands, TEST_PLAYER, null);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Should allow valid kick command")
    void testValidKick() {
        List<String> commands = List.of("/kick Notch AFK");
        var result = MacroValidator.validateCommands(commands, TEST_PLAYER, null);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Should allow valid tp command")
    void testValidTp() {
        List<String> commands = List.of("/tp Notch 100 64 200");
        var result = MacroValidator.validateCommands(commands, TEST_PLAYER, null);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Should block op command")
    void testBlockOp() {
        List<String> commands = List.of("/op Notch");
        var result = MacroValidator.validateCommands(commands, TEST_PLAYER, null);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("denylist"));
    }

    @Test
    @DisplayName("Should block give command")
    void testBlockGive() {
        List<String> commands = List.of("/give Notch diamond 64");
        var result = MacroValidator.validateCommands(commands, TEST_PLAYER, null);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("denylist"));
    }

    @Test
    @DisplayName("Should block namespaced privileged commands")
    void testBlockNamespacedOp() {
        List<String> commands = List.of("/minecraft:op Notch");
        var result = MacroValidator.validateCommands(commands, TEST_PLAYER, null);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("denylist"));
    }

    @Test
    @DisplayName("Should allow a safe command with a privileged-looking prefix")
    void testAllowOpPrefix() {
        List<String> commands = List.of("/opinion Notch");
        var result = MacroValidator.validateCommands(commands, TEST_PLAYER, null);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Should block gamemode creative")
    void testBlockGamemodeCreative() {
        List<String> commands = List.of("/gamemode creative Notch");
        var result = MacroValidator.validateCommands(commands, TEST_PLAYER, null);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("denylist"));
    }

    @Test
    @DisplayName("Should block numeric creative gamemode")
    void testBlockNumericCreativeGamemode() {
        List<String> commands = List.of("/gamemode 1 Notch");
        var result = MacroValidator.validateCommands(commands, TEST_PLAYER, null);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("denylist"));
    }

    @Test
    @DisplayName("Should allow a non-creative gamemode")
    void testAllowSurvivalGamemode() {
        List<String> commands = List.of("/gamemode survival");
        var result = MacroValidator.validateCommands(commands, TEST_PLAYER, null);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Should allow unknown server plugin commands")
    void testAllowServerPluginCommand() {
        List<String> commands = List.of("/staffaccess mtempmute");
        var result = MacroValidator.validateCommands(commands, TEST_PLAYER, null);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Should allow ordinary punctuation in plugin command arguments")
    void testAllowPluginCommandPunctuation() {
        List<String> commands = List.of(
                "/staffaccess mtempmute Notch 10m reason; review && follow up"
        );
        var result = MacroValidator.validateCommands(commands, TEST_PLAYER, null);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Should validate alias - reject dangerous chars")
    void testAliasDangerousChars() {
        List<String> commands = List.of("/mute Notch <alias>");
        var result = MacroValidator.validateCommands(commands, TEST_PLAYER, "10m; rm -rf");

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("forbidden characters"));
    }

    @Test
    @DisplayName("Should validate alias - reject too long")
    void testAliasTooLong() {
        String longAlias = "a".repeat(200);
        List<String> commands = List.of("/mute Notch <alias>");
        var result = MacroValidator.validateCommands(commands, TEST_PLAYER, longAlias);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("too long"));
    }

    @Test
    @DisplayName("Should sanitize alias")
    void testSanitizeAlias() {
        String dangerous = "10m; rm -rf /";
        String sanitized = MacroValidator.sanitizeAlias(dangerous);

        assertEquals("10m rm -rf", sanitized);
    }

    @Test
    @DisplayName("Should verify nick matches target")
    void testNickVerification() {
        List<String> commands = List.of("/mute Notch 10m");
        var result = MacroValidator.validateCommands(commands, TEST_PLAYER, null);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Should fail nick verification when target doesn't match")
    void testNickVerificationFail() {
        PlayerContext wrongPlayer = new PlayerContext("Herobrine", UUID.randomUUID());
        List<String> commands = List.of("/mute Notch 10m");
        var result = MacroValidator.validateCommands(commands, wrongPlayer, null);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Nick verification failed"));
    }

    @Test
    @DisplayName("Should handle empty commands")
    void testEmptyCommands() {
        List<String> commands = List.of();
        var result = MacroValidator.validateCommands(commands, TEST_PLAYER, null);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("No commands"));
    }

    @Test
    @DisplayName("Should reject command without slash")
    void testNoSlash() {
        List<String> commands = List.of("mute Notch 10m");
        var result = MacroValidator.validateCommands(commands, TEST_PLAYER, null);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("must start with"));
    }

    @Test
    @DisplayName("Should reject multiline commands")
    void testRejectControlCharacters() {
        List<String> commands = List.of("/staffaccess\nmtempmute");
        var result = MacroValidator.validateCommands(commands, TEST_PLAYER, null);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("control characters"));
    }

    @Test
    @DisplayName("Should reject too long command")
    void testTooLongCommand() {
        String longCmd = "/mute " + "a".repeat(600);
        List<String> commands = List.of(longCmd);
        var result = MacroValidator.validateCommands(commands, TEST_PLAYER, null);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("too long"));
    }

    @Test
    @DisplayName("Should allow msg/tell commands")
    void testMsgCommand() {
        List<String> commands = List.of("/msg Notch Hello");
        var result = MacroValidator.validateCommands(commands, TEST_PLAYER, null);

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Should allow multiple valid commands")
    void testMultipleCommands() {
        List<String> commands = List.of(
            "/mute Notch 10m",
            "/warn Notch Spam",
            "/msg Notch Please stop"
        );
        var result = MacroValidator.validateCommands(commands, TEST_PLAYER, null);

        assertTrue(result.isSuccess());
    }
}