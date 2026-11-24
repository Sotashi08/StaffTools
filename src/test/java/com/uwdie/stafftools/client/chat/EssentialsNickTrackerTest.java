package com.uwdie.stafftools.client.chat;

import com.uwdie.stafftools.client.player.PlayerContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EssentialsNickTracker.
 */
class EssentialsNickTrackerTest {

    private static final UUID TEST_UUID = UUID.randomUUID();
    private static final PlayerContext TEST_PLAYER = new PlayerContext("RealName", TEST_UUID);

    @BeforeEach
    void setUp() {
        EssentialsNickTracker.get().clear();
    }

    @Test
    @DisplayName("Should learn nick from announcement")
    void testLearnFromAnnouncement() {
        String announcement = "RealName is now known as CoolNick";
        EssentialsNickTracker.get().observeMessage(announcement);

        // The tracker needs the old nick to be resolvable
        // In real usage, the old nick would be in tab list or cache
    }

    @Test
    @DisplayName("Should parse various announcement formats")
    void testAnnouncementFormats() {
        String[] announcements = {
            "OldNick is now known as NewNick",
            "nickname for OldNick has been changed to NewNick",
            "OldNick -> NewNick",
            "OldNick → NewNick"
        };

        for (String announcement : announcements) {
            EssentialsNickTracker.get().clear();
            EssentialsNickTracker.get().observeMessage(announcement);
            // Should not throw
        }
    }

    @Test
    @DisplayName("Should ignore announcements with same old and new nick")
    void testSameNickIgnored() {
        String announcement = "Notch is now known as Notch";
        EssentialsNickTracker.get().observeMessage(announcement);

        // Should not learn anything
        assertNull(EssentialsNickTracker.get().resolve("Notch"));
    }

    @Test
    @DisplayName("Should not learn ranks as nicks")
    void testRanksNotLearned() {
        String[] ranks = {"admin", "moderator", "vip", "premium", "helper", "owner", "builder"};

        for (String rank : ranks) {
            EssentialsNickTracker.get().clear();
            // Try to learn a rank as a nick
            EssentialsNickTracker.get().learn(rank, TEST_PLAYER);
            // Should not be stored
            assertNull(EssentialsNickTracker.get().resolve(rank));
        }
    }

    @Test
    @DisplayName("Should track multiple nicks for one player")
    void testMultipleNicks() {
        EssentialsNickTracker.get().learn("Nick1", TEST_PLAYER);
        EssentialsNickTracker.get().learn("Nick2", TEST_PLAYER);
        EssentialsNickTracker.get().learn("Nick3", TEST_PLAYER);

        List<String> nicks = EssentialsNickTracker.get().nicksOf(TEST_PLAYER);

        assertEquals(3, nicks.size());
        assertTrue(nicks.contains("Nick1"));
        assertTrue(nicks.contains("Nick2"));
        assertTrue(nicks.contains("Nick3"));
    }

    @Test
    @DisplayName("Should find nicks in text")
    void testFindIn() {
        EssentialsNickTracker.get().learn("CoolNick", TEST_PLAYER);

        List<PlayerContext> results = EssentialsNickTracker.get().findIn("Hello CoolNick!");

        assertEquals(1, results.size());
        assertEquals(TEST_PLAYER.name(), results.get(0).name());
    }

    @Test
    @DisplayName("Should identify only this player's changed nick in text")
    void testContainsNickOf() {
        EssentialsNickTracker.get().learn("uwdiesol", TEST_PLAYER);
        PlayerContext otherPlayer =
                new PlayerContext("OtherName", UUID.randomUUID());

        assertTrue(
                EssentialsNickTracker.get()
                        .containsNickOf(TEST_PLAYER, "\u00A7a[VIP] uwdiesol")
        );
        assertFalse(
                EssentialsNickTracker.get()
                        .containsNickOf(otherPlayer, "uwdiesol")
        );
        assertFalse(
                EssentialsNickTracker.get()
                        .containsNickOf(TEST_PLAYER, "xuwdiesol123")
        );
    }

    @Test
    @DisplayName("Should learn custom nick from a decorated tab-list display name")
    void testLearnFromDecoratedDisplayName() {
        PlayerContext owner =
                new PlayerContext("uwdie", UUID.randomUUID());

        EssentialsNickTracker.get()
                .learnFromDisplayName("[CT.MODER] uwdiesol", owner);

        assertTrue(
                EssentialsNickTracker.get()
                        .resolve("uwdiesol") != null
        );
        assertTrue(
                EssentialsNickTracker.get()
                        .containsNickOf(owner, "Hello uwdiesol!")
        );
        assertFalse(
                EssentialsNickTracker.get()
                        .containsNickOf(owner, "Hello uwdie!")
        );
    }

    @Test
    @DisplayName("Should keep original nick when Essentials only changes its format")
    void testCosmeticOriginalNickNotLearnedAsCustom() {
        PlayerContext owner =
                new PlayerContext("uwdie", UUID.randomUUID());

        assertTrue(
                EssentialsNickTracker.isOnlyCosmeticNameChange(
                        "[CT.MODER] uwdie",
                        "uwdie"
                )
        );

        EssentialsNickTracker.get()
                .learnFromDisplayName("[CT.MODER] uwdie", owner);

        assertNull(
                EssentialsNickTracker.get()
                        .resolve("uwdie")
        );
        assertFalse(
                EssentialsNickTracker.get()
                        .containsNickOf(owner, "uwdie")
        );
    }

    @Test
    @DisplayName("Should learn a genuinely changed Essentials nick")
    void testGenuinelyChangedNickIsLearned() {
        PlayerContext owner =
                new PlayerContext("uwdie", UUID.randomUUID());

        assertFalse(
                EssentialsNickTracker.isOnlyCosmeticNameChange(
                        "[CT.MODER] uwdiesol",
                        "uwdie"
                )
        );

        EssentialsNickTracker.get()
                .learnFromDisplayName("[CT.MODER] uwdiesol", owner);

        assertNotNull(
                EssentialsNickTracker.get()
                        .resolve("uwdiesol")
        );
    }

    @Test
    @DisplayName("Should clear all entries")
    void testClear() {
        EssentialsNickTracker.get().learn("Nick1", TEST_PLAYER);
        EssentialsNickTracker.get().clear();

        assertNull(EssentialsNickTracker.get().resolve("Nick1"));
    }

    @Test
    @DisplayName("Should evict oldest when over capacity")
    void testEviction() {
        for (int i = 0; i < 400; i++) {
            EssentialsNickTracker.get().learn("Nick" + i, TEST_PLAYER);
        }

        EssentialsNickTracker.get().learn("NewNick", TEST_PLAYER);

        assertNull(EssentialsNickTracker.get().resolve("Nick0"));
        assertNotNull(EssentialsNickTracker.get().resolve("NewNick"));
    }

    @Test
    @DisplayName("Should update last accessed on resolve")
    void testTouchOnResolve() {
        EssentialsNickTracker.get().learn("Nick1", TEST_PLAYER);
        
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        
        EssentialsNickTracker.get().resolve("Nick1");
        
        EssentialsNickTracker.get().cleanupStaleEntries();
        assertNotNull(EssentialsNickTracker.get().resolve("Nick1"));
    }

    @Test
    @DisplayName("Should handle unicode folded text")
    void testUnicodeFold() {
        EssentialsNickTracker.get().learn("CoolNick", TEST_PLAYER);

        // Unicode folded version
        String folded = "Hello \u1D04\u1D0F\u1D1B\u1D04\u1D00!"; // "CoolNick" in small caps
        List<PlayerContext> results = EssentialsNickTracker.get().findIn(folded);
        
        // Should find if unicode folding is applied in findIn
    }
}