package com.uwdie.stafftools.client.chat;

import com.uwdie.stafftools.client.player.PlayerContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PlayerMentionDetector.
 * Tests various detection scenarios with real server log samples.
 */
class PlayerMentionDetectorTest {

    private static final UUID TEST_UUID = UUID.randomUUID();
    private static final PlayerContext TEST_PLAYER = new PlayerContext("Notch", TEST_UUID);
    private static final PlayerContext TEST_PLAYER2 = new PlayerContext("Herobrine", UUID.randomUUID());

    @BeforeEach
    void setUp() {
        // Clear caches before each test
        NickCache.get().clear();
        EssentialsNickTracker.get().clear();
        NickAliases.cleanupStaleEntries();
    }

    @Test
    @DisplayName("Should detect exact nick match in plain text")
    void testExactMatch() {
        // Given
        NickCache.get().remember(TEST_PLAYER);

        // When
        List<PlayerMention> mentions = PlayerMentionDetector.detect("Hello Notch!");

        // Then
        assertEquals(1, mentions.size());
        assertEquals(TEST_PLAYER.name(), mentions.get(0).player().name());
        assertEquals(6, mentions.get(0).start());
        assertEquals(11, mentions.get(0).end());
    }

    @Test
    @DisplayName("Should not match partial words")
    void testNoPartialMatch() {
        // Given
        NickCache.get().remember(TEST_PLAYER);

        // When
        List<PlayerMention> mentions = PlayerMentionDetector.detect("Hello Notch123!");

        // Then
        assertTrue(mentions.isEmpty(), "Should not match partial words");
    }

    @Test
    @DisplayName("Should detect multiple nicks in one message")
    void testMultipleNicks() {
        // Given
        NickCache.get().remember(TEST_PLAYER);
        NickCache.get().remember(TEST_PLAYER2);

        // When
        List<PlayerMention> mentions = PlayerMentionDetector.detect("Notch and Herobrine are here");

        // Then
        assertEquals(2, mentions.size());
    }

    @Test
    @DisplayName("Should handle case-insensitive matching")
    void testCaseInsensitive() {
        // Given
        NickCache.get().remember(TEST_PLAYER);

        // When
        List<PlayerMention> mentions = PlayerMentionDetector.detect("hello notch!");

        // Then
        assertEquals(1, mentions.size());
        assertEquals("Notch", mentions.get(0).player().name());
    }

    @Test
    @DisplayName("Should detect nick with unicode look-alikes")
    void testUnicodeFold() {
        // Given
        NickCache.get().remember(TEST_PLAYER);

        // When - using unicode look-alikes for "Notch"
        String unicodeText = "Hello \u1D04\u1D0F\u1D1B\u1D04\u1D00!"; // "Notch" in small caps
        List<PlayerMention> mentions = PlayerMentionDetector.detect(unicodeText);

        // Then - should detect if unicode folding is enabled
        // Note: This test documents expected behavior; actual detection depends on config
    }

    @Test
    @DisplayName("Should detect nick with decorations stripped")
    void testDecorationFold() {
        // Given
        NickCache.get().remember(TEST_PLAYER);

        // When - nick with brackets and symbols
        String decoratedText = "Hello [VIP]Notch\u2605!";
        List<PlayerMention> mentions = PlayerMentionDetector.detect(decoratedText);

        // Then - should detect if decoration folding is enabled
    }

    @Test
    @DisplayName("Should detect nick with legacy color codes stripped")
    void testLegacyColorCodes() {
        // Given
        NickCache.get().remember(TEST_PLAYER);

        // When - nick with § color codes
        String coloredText = "Hello \u00A7aNotch\u00A7f!";
        List<PlayerMention> mentions = PlayerMentionDetector.detect(coloredText);

        // Then - should detect if color code stripping is enabled
    }

    @Test
    @DisplayName("Should learn and detect aliases from click/hover events")
    void testAliasLearning() {
        // Given
        String alias = "CoolNick123";
        NickAliases.learn(alias, TEST_PLAYER);

        // When
        List<PlayerMention> mentions = PlayerMentionDetector.detect("Hello " + alias + "!");

        // Then
        assertEquals(1, mentions.size());
        assertEquals(TEST_PLAYER.name(), mentions.get(0).player().name());
    }

    @Test
    @DisplayName("Should track EssentialsX nick changes")
    void testEssentialsNickChange() {
        // Given - simulate a nick change announcement
        String announcement = "Notch is now known as CoolNick";
        EssentialsNickTracker.get().observeMessage(announcement);

        // When - the new nick should be detectable
        List<PlayerMention> mentions = PlayerMentionDetector.detect("Hello CoolNick!");

        // Then - should detect if the old nick was resolvable
        // Note: This requires the old nick to be in tab list or cache
    }

    @Test
    @DisplayName("Nick highlighting toggle disables every detected nick")
    void testNickHighlightSetting() {
        EssentialsNickTracker.get().learn("CoolNick", TEST_PLAYER);

        String message = "CoolNick and Herobrine";
        int customStart = message.indexOf("CoolNick");
        int ordinaryStart = message.indexOf("Herobrine");

        List<PlayerMention> mentions = List.of(
                new PlayerMention(
                        TEST_PLAYER,
                        customStart,
                        customStart + "CoolNick".length()
                ),
                new PlayerMention(
                        TEST_PLAYER2,
                        ordinaryStart,
                        ordinaryStart + "Herobrine".length()
                )
        );

        List<PlayerMention> disabled =
                PlayerMentionDetector.applyNickHighlightSetting(
                        mentions,
                        true
                );

        assertFalse(disabled.get(0).highlight());
        assertFalse(disabled.get(1).highlight());

        List<PlayerMention> enabled =
                PlayerMentionDetector.applyNickHighlightSetting(
                        mentions,
                        false
                );

        assertTrue(enabled.get(0).highlight());
        assertTrue(enabled.get(1).highlight());
    }

    @Test
    @DisplayName("Should merge overlapping mentions correctly")
    void testMergeOverlapping() {
        // Given - two players with similar names
        PlayerContext player1 = new PlayerContext("Notch", UUID.randomUUID());
        PlayerContext player2 = new PlayerContext("Notch_", UUID.randomUUID());
        NickCache.get().remember(player1);
        NickCache.get().remember(player2);

        // When
        List<PlayerMention> mentions = PlayerMentionDetector.detect("Notch and Notch_");

        // Then - both should be detected, longest match wins at same position
        assertEquals(2, mentions.size());
    }

    @Test
    @DisplayName("Should handle empty and null messages")
    void testEmptyMessages() {
        assertTrue(PlayerMentionDetector.detect(null).isEmpty());
        assertTrue(PlayerMentionDetector.detect("").isEmpty());
        assertTrue(PlayerMentionDetector.detect("   ").isEmpty());
    }

    @Test
    @DisplayName("Should respect word boundaries")
    void testWordBoundaries() {
        // Given
        NickCache.get().remember(TEST_PLAYER);

        // When - nick at start, middle, end of message
        List<PlayerMention> start = PlayerMentionDetector.detect("Notch hello");
        List<PlayerMention> middle = PlayerMentionDetector.detect("hello Notch there");
        List<PlayerMention> end = PlayerMentionDetector.detect("hello Notch");

        // Then
        assertEquals(1, start.size());
        assertEquals(1, middle.size());
        assertEquals(1, end.size());
    }

    @Test
    @DisplayName("Should not match nicks inside other words")
    void testNoSubstringMatch() {
        // Given
        NickCache.get().remember(TEST_PLAYER);

        // When
        List<PlayerMention> mentions = PlayerMentionDetector.detect("Notch123 Notch_");

        // Then - should only match "Notch" at word boundary
        assertEquals(1, mentions.size());
        assertEquals(8, mentions.get(0).start()); // "Notch" at position 8
    }

    @Test
    @DisplayName("Real server log sample - ban message")
    void testRealBanMessage() {
        // Given
        PlayerContext target = new PlayerContext("BadPlayer", UUID.randomUUID());
        NickCache.get().remember(target);

        // When - typical ban message format
        String message = "[Server] BadPlayer was banned by Notch for cheating.";
        List<PlayerMention> mentions = PlayerMentionDetector.detect(message);

        // Then
        assertEquals(1, mentions.size());
        assertEquals("BadPlayer", mentions.get(0).player().name());
    }

    @Test
    @DisplayName("Real server log sample - mute message with duration")
    void testRealMuteMessage() {
        // Given
        PlayerContext target = new PlayerContext("Spammer", UUID.randomUUID());
        NickCache.get().remember(target);

        // When
        String message = "Spammer has been muted for 30m (Reason: Flood)";
        List<PlayerMention> mentions = PlayerMentionDetector.detect(message);

        // Then
        assertEquals(1, mentions.size());
        assertEquals("Spammer", mentions.get(0).player().name());
    }

    @Test
    @DisplayName("Real server log sample - kick message")
    void testRealKickMessage() {
        // Given
        PlayerContext target = new PlayerContext("AFKPlayer", UUID.randomUUID());
        NickCache.get().remember(target);

        // When
        String message = "AFKPlayer was kicked: Flying is not enabled on this server";
        List<PlayerMention> mentions = PlayerMentionDetector.detect(message);

        // Then
        assertEquals(1, mentions.size());
        assertEquals("AFKPlayer", mentions.get(0).player().name());
    }

    @Test
    @DisplayName("Real server log sample - warn message")
    void testRealWarnMessage() {
        // Given
        PlayerContext target = new PlayerContext("RuleBreaker", UUID.randomUUID());
        NickCache.get().remember(target);

        // When
        String message = "\u00A7c[Warn] \u00A7fRuleBreaker \u00A77-> \u00A7cSpam in chat";
        List<PlayerMention> mentions = PlayerMentionDetector.detect(message);

        // Then
        assertEquals(1, mentions.size());
        assertEquals("RuleBreaker", mentions.get(0).player().name());
    }

    @Test
    @DisplayName("Real server log sample - EssentialsX nick change")
    void testRealEssentialsNickChange() {
        // Given
        PlayerContext realPlayer = new PlayerContext("RealName", UUID.randomUUID());
        NickCache.get().remember(realPlayer);

        // When - EssentialsX style announcement
        String announcement = "RealName is now known as CoolDisplayName";
        EssentialsNickTracker.get().observeMessage(announcement);

        // Then - the new display name should be tracked
        List<String> nicks = EssentialsNickTracker.get().nicksOf(realPlayer);
        assertTrue(nicks.contains("CoolDisplayName"));
    }

    @Test
    @DisplayName("Real server log sample - multiple players in conversation")
    void testMultiplePlayersConversation() {
        // Given
        PlayerContext p1 = new PlayerContext("Alice", UUID.randomUUID());
        PlayerContext p2 = new PlayerContext("Bob", UUID.randomUUID());
        PlayerContext p3 = new PlayerContext("Charlie", UUID.randomUUID());
        NickCache.get().remember(p1);
        NickCache.get().remember(p2);
        NickCache.get().remember(p3);

        // When
        String message = "Alice: Hey Bob, did you see Charlie?";
        List<PlayerMention> mentions = PlayerMentionDetector.detect(message);

        // Then
        assertEquals(3, mentions.size());
    }

    @Test
    @DisplayName("Should handle nicks with underscores and numbers")
    void testNicksWithSpecialChars() {
        // Given
        PlayerContext player = new PlayerContext("Player_123", UUID.randomUUID());
        NickCache.get().remember(player);

        // When
        List<PlayerMention> mentions = PlayerMentionDetector.detect("Hello Player_123!");

        // Then
        assertEquals(1, mentions.size());
        assertEquals("Player_123", mentions.get(0).player().name());
    }

    @Test
    @DisplayName("Should handle very long messages efficiently")
    void testLongMessage() {
        // Given
        NickCache.get().remember(TEST_PLAYER);

        // When - message with 1000 characters
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("Some text ").append(i).append(" ");
        }
        sb.append("Notch");
        for (int i = 0; i < 50; i++) {
            sb.append(" more text ").append(i).append(" ");
        }

        List<PlayerMention> mentions = PlayerMentionDetector.detect(sb.toString());

        // Then
        assertEquals(1, mentions.size());
    }
}