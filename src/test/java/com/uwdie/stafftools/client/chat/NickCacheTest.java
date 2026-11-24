package com.uwdie.stafftools.client.chat;

import com.uwdie.stafftools.client.player.PlayerContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NickCache.
 */
class NickCacheTest {

    private static final UUID TEST_UUID = UUID.randomUUID();
    private static final PlayerContext TEST_PLAYER = new PlayerContext("Notch", TEST_UUID);

    @BeforeEach
    void setUp() {
        NickCache.get().clear();
    }

    @Test
    @DisplayName("Should remember and resolve exact nick")
    void testRememberAndResolve() {
        NickCache.get().remember("Notch");

        Optional<PlayerContext> result = NickCache.get().resolve("Notch");

        assertTrue(result.isPresent());
        assertEquals("Notch", result.get().name());
    }

    @Test
    @DisplayName("Should be case-insensitive")
    void testCaseInsensitive() {
        NickCache.get().remember("Notch");

        Optional<PlayerContext> result = NickCache.get().resolve("notch");

        assertTrue(result.isPresent());
        assertEquals("Notch", result.get().name());
    }

    @Test
    @DisplayName("Should return empty for unknown nick")
    void testUnknownNick() {
        Optional<PlayerContext> result = NickCache.get().resolve("Unknown");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle null and empty input")
    void testNullAndEmpty() {
        assertTrue(NickCache.get().resolve(null).isEmpty());
        assertTrue(NickCache.get().resolve("").isEmpty());
        assertTrue(NickCache.get().resolve("   ").isEmpty());
    }

    @Test
    @DisplayName("Should not duplicate entries")
    void testNoDuplicates() {
        NickCache.get().remember("Notch");
        NickCache.get().remember("Notch");
        NickCache.get().remember("NOTCH");

        // Should only have one entry
        assertEquals(1, NickCache.get().findIn("Notch").size());
    }

    @Test
    @DisplayName("Should evict oldest when over capacity")
    void testEviction() {
        // Fill up to capacity (600)
        for (int i = 0; i < 600; i++) {
            NickCache.get().remember("Player" + i);
        }

        // Add one more - should evict oldest
        NickCache.get().remember("NewPlayer");

        // Oldest should be gone
        assertTrue(NickCache.get().resolve("Player0").isEmpty());
        assertTrue(NickCache.get().resolve("NewPlayer").isPresent());
    }

    @Test
    @DisplayName("Should find nicks in text")
    void testFindIn() {
        NickCache.get().remember("Notch");
        NickCache.get().remember("Herobrine");

        var results = NickCache.get().findIn("Notch and Herobrine are here");

        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("Should sort results by name length descending")
    void testSortByLength() {
        NickCache.get().remember("A");
        NickCache.get().remember("LongerName");
        NickCache.get().remember("Medium");

        var results = NickCache.get().findIn("A LongerName Medium");

        assertEquals("LongerName", results.get(0).name());
        assertEquals("Medium", results.get(1).name());
        assertEquals("A", results.get(2).name());
    }

    @Test
    @DisplayName("Should clear all entries")
    void testClear() {
        NickCache.get().remember("Notch");
        NickCache.get().clear();

        assertTrue(NickCache.get().resolve("Notch").isEmpty());
    }

    @Test
    @DisplayName("Fuzzy matching - should match with one edit distance")
    void testFuzzyMatch() {
        NickCache.get().remember("Notch");

        Optional<PlayerContext> result = NickCache.get().resolveFuzzy("Notc");

        assertTrue(result.isPresent());
        assertEquals("Notch", result.get().name());
    }

    @Test
    @DisplayName("Fuzzy matching - should not match with two edit distance")
    void testFuzzyNoMatch() {
        NickCache.get().remember("Notch");

        Optional<PlayerContext> result = NickCache.get().resolveFuzzy("Noth");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Fuzzy matching - should require minimum length")
    void testFuzzyMinLength() {
        NickCache.get().remember("Abc");

        Optional<PlayerContext> result = NickCache.get().resolveFuzzy("Ab");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should update last accessed on resolve")
    void testTouchOnResolve() {
        NickCache.get().remember("Notch");
        
        // Wait a bit
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        
        NickCache.get().resolve("Notch");
        
        // Should not be cleaned up as stale
        NickCache.get().cleanupStaleEntries();
        assertTrue(NickCache.get().resolve("Notch").isPresent());
    }
}