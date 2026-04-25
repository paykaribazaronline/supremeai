package com.supremeai.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class UserTest {

    private User user;

    @BeforeEach
    public void setUp() {
        user = new User("firebase-uid-123", "test@example.com", "Test User");
    }

    @Test
    public void testDefaultConstructor() {
        User emptyUser = new User();

        assertNull(emptyUser.getFirebaseUid());
        assertNull(emptyUser.getEmail());
        assertNull(emptyUser.getDisplayName());
        assertEquals(UserTier.FREE, emptyUser.getTier()); // Default is FREE
        assertNull(emptyUser.getIsAdmin());
        assertNull(emptyUser.getRole());
        assertEquals(0L, emptyUser.getCurrentUsage());
        assertNotNull(emptyUser.getCreatedAt());
        assertNotNull(emptyUser.getUpdatedAt());
        assertNull(emptyUser.getLastUsedAt());
        assertTrue(emptyUser.getIsActive()); // Default is true
    }

    @Test
    public void testConstructorWithFirebaseUidEmailDisplayName() {
        assertEquals("firebase-uid-123", user.getFirebaseUid());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("Test User", user.getDisplayName());
        assertEquals(UserTier.FREE, user.getTier());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    public void testSettersAndGetters() {
        user.setFirebaseUid("new-uid");
        assertEquals("new-uid", user.getFirebaseUid());

        user.setEmail("new@example.com");
        assertEquals("new@example.com", user.getEmail());

        user.setDisplayName("New Name");
        assertEquals("New Name", user.getDisplayName());

        user.setTier(UserTier.PRO);
        assertEquals(UserTier.PRO, user.getTier());

        user.setIsAdmin(true);
        assertTrue(user.getIsAdmin());

        user.setRole("ADMIN");
        assertEquals("ADMIN", user.getRole());

        user.setCurrentUsage(500L);
        assertEquals(500L, user.getCurrentUsage());

        user.setCreatedAt(LocalDateTime.now().minusDays(1));
        assertNotNull(user.getCreatedAt());

        LocalDateTime now = LocalDateTime.now();
        user.setUpdatedAt(now);
        assertEquals(now, user.getUpdatedAt());

        user.setLastUsedAt(now);
        assertEquals(now, user.getLastUsedAt());

        user.setLastLoginAt(now);
        assertEquals(now, user.getLastLoginAt());

        user.setIsActive(false);
        assertFalse(user.getIsActive());
    }

    @Test
    public void testIsAdmin() {
        user.setTier(UserTier.ADMIN);
        assertTrue(user.isAdmin());

        user.setTier(UserTier.FREE);
        assertFalse(user.isAdmin());

        // Test legacy isAdmin field
        user.setTier(UserTier.FREE);
        user.setIsAdmin(true);
        assertTrue(user.isAdmin());
    }

    @Test
    public void testGetMonthlyQuota() {
        user.setTier(UserTier.GUEST);
        assertEquals(10L, user.getMonthlyQuota());

        user.setTier(UserTier.FREE);
        assertEquals(100L, user.getMonthlyQuota());

        user.setTier(UserTier.BASIC);
        assertEquals(1_000L, user.getMonthlyQuota());

        user.setTier(UserTier.PRO);
        assertEquals(5_000L, user.getMonthlyQuota());

        user.setTier(UserTier.ENTERPRISE);
        assertEquals(50_000L, user.getMonthlyQuota());

        user.setTier(UserTier.ADMIN);
        assertEquals(Long.MAX_VALUE, user.getMonthlyQuota());
    }

    @Test
    public void testResetMonthlyUsage() {
        user.setCurrentUsage(500L);
        user.setLastUsedAt(LocalDateTime.now());

        user.resetMonthlyUsage();

        assertEquals(0L, user.getCurrentUsage());
        assertNotNull(user.getUpdatedAt());
    }

    @Test
    public void testHasQuotaRemaining() {
        user.setTier(UserTier.FREE);
        user.setCurrentUsage(50L);
        assertTrue(user.hasQuotaRemaining());

        user.setCurrentUsage(100L);
        assertFalse(user.hasQuotaRemaining());

        // Admin always has quota
        user.setTier(UserTier.ADMIN);
        user.setCurrentUsage(Long.MAX_VALUE);
        assertTrue(user.hasQuotaRemaining());
    }
}
