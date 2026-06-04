package com.supremeai.learning;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LearningQuotaService.
 * Tests quota enforcement, daily reset logic, and emergency thresholds.
 */
class LearningQuotaServiceTest {

    private LearningQuotaService quotaService;

    @BeforeEach
    void setUp() {
        quotaService = new LearningQuotaService();
        // Override defaults for testing
        ReflectionTestUtils.setField(quotaService, "globalDailyMax", 100);
        ReflectionTestUtils.setField(quotaService, "perUserDailyMax", 10);
        ReflectionTestUtils.setField(quotaService, "siteVisitMaxPerUser", 5);
        ReflectionTestUtils.setField(quotaService, "emergencyThreshold", 0.9);
    }

    @Test
    void testCheckQuota_allowsWithinLimit() {
        boolean result = quotaService.checkQuota("user1", "SCRAPE", 3);
        assertTrue(result, "Quota should be allowed within limit");
    }

    @Test
    void testCheckQuota_deniesExceedingPerUserLimit() {
        // Exhaust user quota first
        quotaService.checkQuota("user1", "SCRAPE", 10);

        boolean result = quotaService.checkQuota("user1", "SCRAPE", 1);
        assertFalse(result, "Should deny when per-user limit exceeded");
    }

    @Test
    void testCheckQuota_deniesExceedingGlobalLimit() {
        // Increase per-user limit so we can hit global limit
        ReflectionTestUtils.setField(quotaService, "perUserDailyMax", 200);
        // Exhaust global quota
        quotaService.checkQuota("user1", "SCRAPE", 100);

        boolean result = quotaService.checkQuota("user2", "SCRAPE", 1);
        assertFalse(result, "Should deny when global limit exceeded");
    }

    @Test
    void testCheckQuota_nullUserId_treatedAsAnonymous() {
        boolean result = quotaService.checkQuota(null, "SCRAPE", 5);
        assertTrue(result, "Null user ID should be treated as anonymous");
    }

    @Test
    void testCheckQuota_differentUsersHaveSeparateCounters() {
        boolean user1Result = quotaService.checkQuota("user1", "SCRAPE", 8);
        boolean user2Result = quotaService.checkQuota("user2", "SCRAPE", 8);
        assertTrue(user1Result && user2Result, "Both users should have separate quotas");

        // user1 should be almost at limit (8/10), user2 at 8/10
        assertFalse(quotaService.checkQuota("user1", "SCRAPE", 3));
        assertFalse(quotaService.checkQuota("user2", "SCRAPE", 3));
    }

    @Test
    void testCanVisitSite_withinLimit_returnsTrue() {
        assertTrue(quotaService.canVisitSite("user1"));
        quotaService.recordSiteVisit("user1");
        assertTrue(quotaService.canVisitSite("user1"));
    }

    @Test
    void testCanVisitSite_exceedsLimit_returnsFalse() {
        for (int i = 0; i < 5; i++) {
            quotaService.recordSiteVisit("user1");
        }
        assertFalse(quotaService.canVisitSite("user1"), "Should deny after 5 visits");
    }

    @Test
    void testRecordSiteVisit_consumesQuota() {
        // Exhaust site visit quota
        for (int i = 0; i < 5; i++) {
            quotaService.recordSiteVisit("user1");
        }
        assertFalse(quotaService.canVisitSite("user1"), "Quota should be exhausted after 5 visits");
    }

    @Test
    void testGetQuotaStats_returnsValidStats() {
        // Increase per-user limit for this check
        ReflectionTestUtils.setField(quotaService, "perUserDailyMax", 100);
        quotaService.checkQuota("user1", "SCRAPE", 5);

        Map<String, Object> stats = quotaService.getQuotaStats();
        assertEquals(5, (Integer) stats.get("globalDailyUsed"));
        assertEquals(100, (Integer) stats.get("globalDailyMax"));
        assertEquals(100, (Integer) stats.get("perUserDailyMax"));
        assertNotNull(stats.get("lastReset"));
    }

    @Test
    void testIsEmergencyThresholdExceeded_underThreshold() {
        // Increase per-user limit
        ReflectionTestUtils.setField(quotaService, "perUserDailyMax", 100);
        quotaService.checkQuota("user1", "SCRAPE", 80);
        assertFalse(quotaService.isEmergencyThresholdExceeded(), "Should not exceed at 80%");
    }

    @Test
    void testIsEmergencyThresholdExceeded_overThreshold() {
        // Increase per-user limit
        ReflectionTestUtils.setField(quotaService, "perUserDailyMax", 100);
        quotaService.checkQuota("user1", "SCRAPE", 91);
        assertTrue(quotaService.isEmergencyThresholdExceeded(), "Should exceed at 91% (threshold 90%)");
    }

    @Test
    void testCheckQuota_emergencyThresholdTriggered() {
        // Increase per-user limit
        ReflectionTestUtils.setField(quotaService, "perUserDailyMax", 100);
        // At 91% global usage, next check should log emergency but still allow until 100%
        quotaService.checkQuota("user1", "SCRAPE", 91);
        boolean result = quotaService.checkQuota("user2", "SCRAPE", 1);
        assertTrue(result, "Should still allow until global limit is reached");
    }

    @Test
    void testSetPerUserDailyMax_adminOverride() throws Exception {
        quotaService.setPerUserDailyMax(20);
        // Use reflection to verify field changed
        java.lang.reflect.Field field = LearningQuotaService.class.getDeclaredField("perUserDailyMax");
        field.setAccessible(true);
        int newValue = (int) field.get(quotaService);
        assertEquals(20, newValue);
    }

    @Test
    void testSetGlobalDailyMax_adminOverride() throws Exception {
        quotaService.setGlobalDailyMax(200);
        java.lang.reflect.Field field = LearningQuotaService.class.getDeclaredField("globalDailyMax");
        field.setAccessible(true);
        int newValue = (int) field.get(quotaService);
        assertEquals(200, newValue);
    }
}
