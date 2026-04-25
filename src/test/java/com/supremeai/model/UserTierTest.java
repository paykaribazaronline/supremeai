package com.supremeai.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserTierTest {

    @Test
    public void testGetDescription() {
        assertEquals("Anonymous guest access with minimal quota", UserTier.GUEST.getDescription());
        assertEquals("Registered free tier", UserTier.FREE.getDescription());
        assertEquals("Basic paid tier", UserTier.BASIC.getDescription());
        assertEquals("Professional tier with high limits", UserTier.PRO.getDescription());
        assertEquals("Enterprise tier with maximum limits", UserTier.ENTERPRISE.getDescription());
        assertEquals("Administrator with unlimited access", UserTier.ADMIN.getDescription());
    }

    @Test
    public void testIsPremium() {
        assertFalse(UserTier.GUEST.isPremium());
        assertFalse(UserTier.FREE.isPremium());
        assertTrue(UserTier.BASIC.isPremium());
        assertTrue(UserTier.PRO.isPremium());
        assertTrue(UserTier.ENTERPRISE.isPremium());
        assertFalse(UserTier.ADMIN.isPremium()); // ADMIN is unlimited, not premium
    }

    @Test
    public void testIsUnlimited() {
        assertFalse(UserTier.GUEST.isUnlimited());
        assertFalse(UserTier.FREE.isUnlimited());
        assertFalse(UserTier.BASIC.isUnlimited());
        assertFalse(UserTier.PRO.isUnlimited());
        assertFalse(UserTier.ENTERPRISE.isUnlimited());
        assertTrue(UserTier.ADMIN.isUnlimited());
    }

    @Test
    public void testGetDefaultMonthlyQuota() {
        assertEquals(10L, UserTier.GUEST.getDefaultMonthlyQuota());
        assertEquals(100L, UserTier.FREE.getDefaultMonthlyQuota());
        assertEquals(1_000L, UserTier.BASIC.getDefaultMonthlyQuota());
        assertEquals(5_000L, UserTier.PRO.getDefaultMonthlyQuota());
        assertEquals(50_000L, UserTier.ENTERPRISE.getDefaultMonthlyQuota());
        assertEquals(Long.MAX_VALUE, UserTier.ADMIN.getDefaultMonthlyQuota());
    }

    @Test
    public void testHasUnlimitedQuota() {
        assertFalse(UserTier.GUEST.hasUnlimitedQuota());
        assertFalse(UserTier.FREE.hasUnlimitedQuota());
        assertFalse(UserTier.BASIC.hasUnlimitedQuota());
        assertFalse(UserTier.PRO.hasUnlimitedQuota());
        assertFalse(UserTier.ENTERPRISE.hasUnlimitedQuota());
        assertTrue(UserTier.ADMIN.hasUnlimitedQuota());
    }

    @Test
    public void testValues() {
        UserTier[] values = UserTier.values();
        assertEquals(6, values.length);
    }
}
