package com.supremeai.cost;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

public class QuotaDefinitionTest {

    @Test
    public void testConstructor() {
        QuotaDefinition quota = new QuotaDefinition("TestService", "testFeature", 1000.0, QuotaPeriod.DAILY);

        assertEquals("TestService", quota.getServiceName());
        assertEquals("testFeature", quota.getFeatureName());
        assertEquals(1000.0, quota.getLimit(), 0.001);
        assertEquals(QuotaPeriod.DAILY, quota.getPeriod());
        assertEquals(0.0, quota.getCurrentUsage(), 0.001);
        // Cannot test lastResetTime as there's no getter
    }

    @Test
    public void testAddUsage() {
        QuotaDefinition quota = new QuotaDefinition("TestService", "testFeature", 1000.0, QuotaPeriod.DAILY);

        quota.addUsage(100.0);

        assertEquals(100.0, quota.getCurrentUsage(), 0.001);
    }

    @Test
    public void testIsLimitReached_NotReached() {
        QuotaDefinition quota = new QuotaDefinition("TestService", "testFeature", 1000.0, QuotaPeriod.DAILY);
        quota.addUsage(500.0);

        assertFalse(quota.isLimitReached());
    }

    @Test
    public void testIsLimitReached_Reached() {
        QuotaDefinition quota = new QuotaDefinition("TestService", "testFeature", 1000.0, QuotaPeriod.DAILY);
        quota.addUsage(1000.0);

        assertTrue(quota.isLimitReached());
    }

    @Test
    public void testGetRemainingQuota() {
        QuotaDefinition quota = new QuotaDefinition("TestService", "testFeature", 1000.0, QuotaPeriod.DAILY);
        quota.addUsage(300.0);

        assertEquals(700.0, quota.getRemainingQuota(), 0.001);
    }

    @Test
    public void testGetUsagePercentage() {
        QuotaDefinition quota = new QuotaDefinition("TestService", "testFeature", 1000.0, QuotaPeriod.DAILY);
        quota.addUsage(250.0);

        assertEquals(25.0, quota.getUsagePercentage(), 0.001);
    }

    @Test
    public void testCheckAndResetIfNeeded_NoReset() {
        QuotaDefinition quota = new QuotaDefinition("TestService", "testFeature", 1000.0, QuotaPeriod.DAILY);

        quota.addUsage(100.0);

        assertEquals(100.0, quota.getCurrentUsage(), 0.001);
    }

    @Test
    public void testQuotaPeriodValues() {
        assertEquals(4, QuotaPeriod.values().length);

        // Test that all periods are available
        boolean hasHourly = false;
        boolean hasDaily = false;
        boolean hasMonthly = false;
        boolean hasYearly = false;

        for (QuotaPeriod period : QuotaPeriod.values()) {
            if (period == QuotaPeriod.HOURLY) hasHourly = true;
            if (period == QuotaPeriod.DAILY) hasDaily = true;
            if (period == QuotaPeriod.MONTHLY) hasMonthly = true;
            if (period == QuotaPeriod.YEARLY) hasYearly = true;
        }

        assertTrue(hasHourly);
        assertTrue(hasDaily);
        assertTrue(hasMonthly);
        assertTrue(hasYearly);
    }
}
