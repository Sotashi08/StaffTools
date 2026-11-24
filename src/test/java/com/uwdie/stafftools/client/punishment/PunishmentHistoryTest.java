package com.uwdie.stafftools.client.punishment;

import com.uwdie.stafftools.client.player.PlayerContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PunishmentHistory.
 */
class PunishmentHistoryTest {

    @TempDir
    Path tempDir;

    private PunishmentHistory history;

    @BeforeEach
    void setUp() {
        Path historyFile = tempDir.resolve("test_history.json");
        PunishmentHistory.init(historyFile);
        history = PunishmentHistory.get();
    }

    @Test
    @DisplayName("Should record punishment from command")
    void testRecordPunishment() {
        history.onCommandSent("/mute Notch 10m");

        List<PunishmentRecord> records = history.getRecords();
        assertEquals(1, records.size());
        assertEquals("Notch", records.get(0).getPlayerName());
        assertEquals(PunishmentType.MUTE, records.get(0).getType());
        assertEquals(PunishmentStatus.PENDING, records.get(0).getStatus());
    }

    @Test
    @DisplayName("Should resolve punishment on success response")
    void testResolveSuccess() {
        history.onCommandSent("/mute Notch 10m");

        // Simulate server response
        history.onMessageReceived("Notch has been muted for 10m.");

        List<PunishmentRecord> records = history.getRecords();
        assertEquals(PunishmentStatus.DONE, records.get(0).getEffectiveStatus());
    }

    @Test
    @DisplayName("Should mark as failed on error response")
    void testResolveFailure() {
        history.onCommandSent("/mute Notch 10m");

        // Simulate error response
        history.onMessageReceived("Недостаточно прав для выполнения команды");

        List<PunishmentRecord> records = history.getRecords();
        assertEquals(PunishmentStatus.FAILED, records.get(0).getEffectiveStatus());
    }

    @Test
    @DisplayName("Should mark as no response after timeout")
    void testTimeout() {
        history.onCommandSent("/mute Notch 10m");

        // Get the record and manually set timestamp to past
        PunishmentRecord record = history.getRecords().get(0);
        record.setTimestamp(System.currentTimeMillis() - 70_000); // 70 seconds ago

        // Effective status should be NO_RESPONSE (timeout is 60s now)
        assertEquals(PunishmentStatus.NO_RESPONSE, record.getEffectiveStatus());
    }

    @Test
    @DisplayName("Should not resolve other moderator's punishments")
    void testForeignIssuer() {
        history.onCommandSent("/mute Notch 10m");

        // Simulate broadcast from another moderator
        history.onMessageReceived("Herobrine muted Notch for 10m.");

        List<PunishmentRecord> records = history.getRecords();
        // Should still be pending because issuer is different
        assertEquals(PunishmentStatus.PENDING, records.get(0).getEffectiveStatus());
    }

    @Test
    @DisplayName("Should filter by server")
    void testServerFilter() {
        history.onCommandSent("/mute Notch 10m");

        List<PunishmentRecord> all = history.getRecordsOf();
        List<PunishmentRecord> server = history.getRecordsOf("some.server.address");

        assertEquals(1, all.size());
        assertEquals(0, server.size());
    }

    @Test
    @DisplayName("Should clear history")
    void testClear() {
        history.onCommandSent("/mute Notch 10m");
        history.clear();

        assertTrue(history.getRecords().isEmpty());
    }

    @Test
    @DisplayName("Should limit records to max")
    void testMaxRecords() {
        for (int i = 0; i < 350; i++) {
            history.onCommandSent("/mute Player" + i + " 10m");
        }

        assertEquals(300, history.getRecords().size());
    }

    @Test
    @DisplayName("Should track correlation ID")
    void testCorrelationId() {
        history.onCommandSent("/mute Notch 10m");

        PunishmentRecord record = history.getRecords().get(0);
        assertNotNull(record.getCorrelationId());
        assertFalse(record.getCorrelationId().isBlank());
    }

    @Test
    @DisplayName("Should handle ban command")
    void testBanCommand() {
        history.onCommandSent("/ban Hacker Cheating");

        PunishmentRecord record = history.getRecords().get(0);
        assertEquals(PunishmentType.BAN, record.getType());
        assertEquals("Hacker", record.getPlayerName());
    }

    @Test
    @DisplayName("Should handle kick command")
    void testKickCommand() {
        history.onCommandSent("/kick AFKPlayer Flying");

        PunishmentRecord record = history.getRecords().get(0);
        assertEquals(PunishmentType.KICK, record.getType());
    }

    @Test
    @DisplayName("Should handle warn command")
    void testWarnCommand() {
        history.onCommandSent("/warn Spammer Flood");

        PunishmentRecord record = history.getRecords().get(0);
        assertEquals(PunishmentType.WARN, record.getType());
    }

    @Test
    @DisplayName("Should handle freeze command")
    void testFreezeCommand() {
        history.onCommandSent("/freeze Suspect");

        PunishmentRecord record = history.getRecords().get(0);
        assertEquals(PunishmentType.FREEZE, record.getType());
    }

    @Test
    @DisplayName("Should persist to disk and reload")
    void testPersistence() {
        history.onCommandSent("/mute Notch 10m");
        history.onMessageReceived("Notch has been muted for 10m.");

        // Create new instance with same file
        Path historyFile = tempDir.resolve("test_history.json");
        PunishmentHistory.init(historyFile);
        PunishmentHistory newHistory = PunishmentHistory.get();

        List<PunishmentRecord> records = newHistory.getRecords();
        assertEquals(1, records.size());
        assertEquals(PunishmentStatus.DONE, records.get(0).getEffectiveStatus());
    }
}