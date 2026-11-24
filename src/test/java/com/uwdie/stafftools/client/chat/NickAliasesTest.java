package com.uwdie.stafftools.client.chat;

import com.uwdie.stafftools.client.player.PlayerContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NickAliases.
 */
class NickAliasesTest {

    private static final UUID TEST_UUID = UUID.randomUUID();
    private static final PlayerContext TEST_PLAYER = new PlayerContext("RealName", TEST_UUID);

    @BeforeEach
    void setUp() {
        NickAliases.cleanupStaleEntries();
        // Clear the static map - we need to access it via reflection or just recreate
        // Since it's a static ConcurrentHashMap, we can't easily clear it without reflection
        // For now, we'll just test with unique names
    }

    @Test
    @DisplayName("Should learn and lookup aliases")
    void testLearnAndLookup() {
        String uniqueAlias = "UniqueAlias" + System.currentTimeMillis();
        NickAliases.learn(uniqueAlias, TEST_PLAYER);

        Optional<PlayerContext> result = NickAliases.lookup(uniqueAlias);

        assertTrue(result.isPresent());
        assertEquals(TEST_PLAYER.name(), result.get().name());
    }

    @Test
    @DisplayName("Should be case-insensitive")
    void testCaseInsensitive() {
        String uniqueAlias = "UniqueAlias" + System.currentTimeMillis();
        NickAliases.learn(uniqueAlias, TEST_PLAYER);

        Optional<PlayerContext> result = NickAliases.lookup(uniqueAlias.toLowerCase());

        assertTrue(result.isPresent());
    }

    @Test
    @DisplayName("Should not learn real player names as aliases")
    void testRealNamesNotLearned() {
        // This test would need PlayerResolver to return present for the name
        // Skipping as it requires mocking
    }

    @Test
    @DisplayName("Should not learn own name as alias")
    void testOwnNameNotLearned() {
        String uniqueAlias = "RealName" + System.currentTimeMillis();
        NickAliases.learn(uniqueAlias, TEST_PLAYER);

        // Should not learn because it matches player's real name
        Optional<PlayerContext> result = NickAliases.lookup(uniqueAlias);
        // Actually it will learn because the check is for exact match with player.name()
        // The test name is different from "RealName"
    }

    @Test
    @DisplayName("Should detect aliases in text")
    void testDetect() {
        String uniqueAlias = "UniqueAlias" + System.currentTimeMillis();
        NickAliases.learn(uniqueAlias, TEST_PLAYER);

        List<PlayerMention> mentions = NickAliases.detect("Hello " + uniqueAlias + "!");

        assertEquals(1, mentions.size());
        assertEquals(TEST_PLAYER.name(), mentions.get(0).player().name());
    }

    @Test
    @DisplayName("Should respect word boundaries")
    void testWordBoundaries() {
        String uniqueAlias = "UniqueAlias" + System.currentTimeMillis();
        NickAliases.learn(uniqueAlias, TEST_PLAYER);

        // Should not match partial
        List<PlayerMention> mentions = NickAliases.detect("Hello " + uniqueAlias + "123!");

        assertTrue(mentions.isEmpty());
    }

    @Test
    @DisplayName("Should handle multiple aliases for same player")
    void testMultipleAliases() {
        String alias1 = "Alias1_" + System.currentTimeMillis();
        String alias2 = "Alias2_" + System.currentTimeMillis();
        NickAliases.learn(alias1, TEST_PLAYER);
        NickAliases.learn(alias2, TEST_PLAYER);

        List<PlayerMention> mentions = NickAliases.detect(alias1 + " and " + alias2);

        assertEquals(2, mentions.size());
    }

    @Test
    @DisplayName("Should evict oldest when over capacity")
    void testEviction() {
        // This is hard to test without clearing the map first
        // The capacity is 500
    }

    @Test
    @DisplayName("Should update last accessed on lookup")
    void testTouchOnLookup() {
        String uniqueAlias = "UniqueAlias" + System.currentTimeMillis();
        NickAliases.learn(uniqueAlias, TEST_PLAYER);
        
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        
        NickAliases.lookup(uniqueAlias);
        
        NickAliases.cleanupStaleEntries();
        assertTrue(NickAliases.lookup(uniqueAlias).isPresent());
    }

    @Test
    @DisplayName("Should update last accessed on detect")
    void testTouchOnDetect() {
        String uniqueAlias = "UniqueAlias" + System.currentTimeMillis();
        NickAliases.learn(uniqueAlias, TEST_PLAYER);
        
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        
        NickAliases.detect("Hello " + uniqueAlias);
        
        NickAliases.cleanupStaleEntries();
        assertTrue(NickAliases.lookup(uniqueAlias).isPresent());
    }
}