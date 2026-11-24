package com.uwdie.stafftools.client.punishment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CommandAnalyzer.
 */
class CommandAnalyzerTest {

    @Test
    @DisplayName("Should analyze mute command")
    void testAnalyzeMute() {
        PunishmentRecord record = CommandAnalyzer.analyze("/mute Notch 10m");

        assertNotNull(record);
        assertEquals(PunishmentType.MUTE, record.getType());
        assertEquals("Notch", record.getPlayerName());
    }

    @Test
    @DisplayName("Should analyze ban command")
    void testAnalyzeBan() {
        PunishmentRecord record = CommandAnalyzer.analyze("/ban Hacker Cheating");

        assertNotNull(record);
        assertEquals(PunishmentType.BAN, record.getType());
        assertEquals("Hacker", record.getPlayerName());
    }

    @Test
    @DisplayName("Should analyze kick command")
    void testAnalyzeKick() {
        PunishmentRecord record = CommandAnalyzer.analyze("/kick AFKPlayer Flying");

        assertNotNull(record);
        assertEquals(PunishmentType.KICK, record.getType());
        assertEquals("AFKPlayer", record.getPlayerName());
    }

    @Test
    @DisplayName("Should analyze warn command")
    void testAnalyzeWarn() {
        PunishmentRecord record = CommandAnalyzer.analyze("/warn Spammer Flood");

        assertNotNull(record);
        assertEquals(PunishmentType.WARN, record.getType());
        assertEquals("Spammer", record.getPlayerName());
    }

    @Test
    @DisplayName("Should analyze freeze command")
    void testAnalyzeFreeze() {
        PunishmentRecord record = CommandAnalyzer.analyze("/freeze Suspect");

        assertNotNull(record);
        assertEquals(PunishmentType.FREEZE, record.getType());
        assertEquals("Suspect", record.getPlayerName());
    }

    @Test
    @DisplayName("Should return null for non-punishment commands")
    void testNonPunishment() {
        PunishmentRecord record = CommandAnalyzer.analyze("/msg Hello");
        assertNull(record);

        record = CommandAnalyzer.analyze("/tp Notch 100 64 200");
        assertNull(record);

        record = CommandAnalyzer.analyze("/home");
        assertNull(record);
    }

    @Test
    @DisplayName("Should detect failure messages")
    void testIsFailure() {
        assertTrue(CommandAnalyzer.isFailure("недостаточно прав"));
        assertTrue(CommandAnalyzer.isFailure("permission denied"));
        assertTrue(CommandAnalyzer.isFailure("unknown command"));
        assertTrue(CommandAnalyzer.isFailure("player not found"));
        assertTrue(CommandAnalyzer.isFailure("cannot mute yourself"));

        assertFalse(CommandAnalyzer.isFailure("player muted successfully"));
        assertFalse(CommandAnalyzer.isFailure("done"));
    }

    @Test
    @DisplayName("Should extract issuer from broadcast")
    void testExtractIssuer() {
        String msg1 = "Notch muted Herobrine for 10m.";
        String issuer1 = CommandAnalyzer.extractIssuer(msg1);
        assertEquals("notch", issuer1);

        String msg2 = "Herobrine was banned by Admin123";
        String issuer2 = CommandAnalyzer.extractIssuer(msg2);
        assertEquals("admin123", issuer2);
    }

    @Test
    @DisplayName("Should normalize text for comparison")
    void testNormalize() {
        String normalized = CommandAnalyzer.normalize("\u00A7c[Server] \u00A7fNotch \u00A77muted");
        assertFalse(normalized.contains("\u00A7"));
        assertEquals(normalized.toLowerCase(), normalized);
    }

    @Test
    @DisplayName("Should check if name contained in text")
    void testContainsName() {
        assertTrue(CommandAnalyzer.containsName("hello notch there", "notch"));
        assertTrue(CommandAnalyzer.containsName("notch", "notch"));
        assertTrue(CommandAnalyzer.containsName("hey notch!", "notch"));

        assertFalse(CommandAnalyzer.containsName("notch123", "notch"));
        assertFalse(CommandAnalyzer.containsName("anotch", "notch"));
    }

    @Test
    @DisplayName("Should resolve outcome from success message")
    void testResolveOutcome() {
        PunishmentStatus outcome = CommandAnalyzer.resolveOutcome(
            "notch has been muted for 10m", PunishmentType.MUTE);
        assertEquals(PunishmentStatus.DONE, outcome);

        outcome = CommandAnalyzer.resolveOutcome(
            "hacker was banned permanently", PunishmentType.BAN);
        assertEquals(PunishmentStatus.DONE, outcome);
    }

    @Test
    @DisplayName("Should return null for ambiguous messages")
    void testResolveOutcomeAmbiguous() {
        PunishmentStatus outcome = CommandAnalyzer.resolveOutcome(
            "notch is online", PunishmentType.MUTE);
        assertNull(outcome);
    }

    @Test
    @DisplayName("Should check if type mentions outcome")
    void testMentionsOutcome() {
        assertTrue(PunishmentType.MUTE.mentionsOutcome("muted for 10m"));
        assertTrue(PunishmentType.BAN.mentionsOutcome("banned permanently"));
        assertTrue(PunishmentType.KICK.mentionsOutcome("was kicked"));
        assertTrue(PunishmentType.WARN.mentionsOutcome("warned for spam"));

        assertFalse(PunishmentType.MUTE.mentionsOutcome("banned"));
        assertFalse(PunishmentType.BAN.mentionsOutcome("muted"));
    }

    @Test
    @DisplayName("Should handle tempban/tempmute aliases")
    void testTempCommands() {
        PunishmentRecord record = CommandAnalyzer.analyze("/tempban Hacker 1d Cheating");
        assertNotNull(record);
        assertEquals(PunishmentType.BAN, record.getType());

        record = CommandAnalyzer.analyze("/tempmute Spammer 30m Flood");
        assertNotNull(record);
        assertEquals(PunishmentType.MUTE, record.getType());
    }

    @Test
    @DisplayName("Should handle unmute/unban")
    void testUnCommands() {
        PunishmentRecord record = CommandAnalyzer.analyze("/unmute Notch");
        assertNotNull(record);
        assertEquals(PunishmentType.UNMUTE, record.getType());

        record = CommandAnalyzer.analyze("/unban Hacker");
        assertNotNull(record);
        assertEquals(PunishmentType.UNBAN, record.getType());
    }
}