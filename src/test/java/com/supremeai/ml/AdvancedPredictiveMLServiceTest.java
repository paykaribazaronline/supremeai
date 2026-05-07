package com.supremeai.ml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AdvancedPredictiveMLService.
 * Tests ARIMA forecasting, quota prediction, and time-series analysis.
 */
class AdvancedPredictiveMLServiceTest {

    private AdvancedPredictiveMLService mlService;

    @BeforeEach
    void setUp() {
        mlService = new AdvancedPredictiveMLService();
    }

    @Test
    void testRecordQuotaUsage_storesData() {
        mlService.recordQuotaUsage("user1", 10, 100);
        // No exception thrown
    }

    @Test
    void testRecordSystemLoad_storesData() {
        mlService.recordSystemLoad("api-service", 0.75);
        // No exception thrown
    }

    @Test
    void testRecordUserActivity_storesData() {
        mlService.recordUserActivity("user1", "SCRAPE");
        // No exception thrown
    }

    @Test
    void testPredictQuotaExhaustion_insufficientData_returnsDefault() {
        // Record fewer than 30 data points
        for (int i = 0; i < 10; i++) {
            mlService.recordQuotaUsage("user1", 10, 100);
        }

        AdvancedPredictiveMLService.QuotaPrediction prediction =
            mlService.predictQuotaExhaustion("user1", 10, 100);

        assertEquals("user1", prediction.userId);
        assertEquals(-1, prediction.daysUntilExhaustion);
        assertEquals(0.0, prediction.confidence, 0.001);
        assertTrue(prediction.message.contains("Insufficient data"));
    }

    @Test
    void testPredictQuotaExhaustion_sufficientData_returnsPrediction() {
        // Record 35 data points (enough for model)
        for (int i = 0; i < 35; i++) {
            mlService.recordQuotaUsage("user2", (long)(i * 2), 100);
        }

        AdvancedPredictiveMLService.QuotaPrediction prediction =
            mlService.predictQuotaExhaustion("user2", 70, 100);

        assertEquals("user2", prediction.userId);
        assertNotNull(prediction.metadata);
        assertTrue(prediction.metadata.containsKey("dailyUsageRate"));
        assertTrue(prediction.metadata.containsKey("trend"));
    }

    @Test
    void testGetStatistics_returnsNonZeroStats() {
        mlService.recordQuotaUsage("u1", 10, 100);
        mlService.recordSystemLoad("svc1", 0.5);
        mlService.recordUserActivity("u2", "LOGIN");

        Map<String, Object> stats = mlService.getStatistics();
        assertEquals(1, stats.get("quotaHistoryUsers"));
        assertEquals(1, stats.get("systemLoadServices"));
        assertEquals(1, stats.get("activityHistoryUsers"));
    }

    @Test
    void testPredictNextActivity_insufficientData_returnsUnknown() {
        for (int i = 0; i < 5; i++) {
            mlService.recordUserActivity("user1", "SCRAPE");
        }

        AdvancedPredictiveMLService.UserActivityPrediction prediction =
            mlService.predictNextActivity("user1");

        assertEquals("user1", prediction.userId);
        assertEquals("unknown", prediction.predictedActivity);
    }

    @Test
    void testApplyDifferencing_firstOrder() {
        List<Double> values = List.of(1.0, 2.0, 3.0, 4.0, 5.0);
        List<Double> differenced = invokeApplyDifferencing(values, 1);

        assertEquals(4, differenced.size());
        assertEquals(1.0, differenced.get(0), 0.001);
        assertEquals(1.0, differenced.get(1), 0.001);
        assertEquals(1.0, differenced.get(2), 0.001);
        assertEquals(1.0, differenced.get(3), 0.001);
    }

    @Test
    void testApplyDifferencing_secondOrder() {
        List<Double> values = List.of(1.0, 2.0, 4.0, 7.0, 11.0);
        List<Double> first = invokeApplyDifferencing(values, 1);
        List<Double> second = invokeApplyDifferencing(first, 1);

        // Second order differences
        assertEquals(3, second.size());
    }

    @Test
    void testCalculateVariance_nonZeroValues() {
        List<Double> values = List.of(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0);
        double variance = invokeCalculateVariance(values);
        assertTrue(variance > 0);
    }

    @Test
    void testCalculateVariance_constantValues_returnsZero() {
        List<Double> values = List.of(5.0, 5.0, 5.0, 5.0);
        double variance = invokeCalculateVariance(values);
        assertEquals(0.0, variance, 0.0001);
    }

    @Test
    void testAnalyzeTrend_increasing() {
        List<Double> values = List.of(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0);
        String trend = invokeAnalyzeTrend(values);
        assertEquals("increasing", trend);
    }

    @Test
    void testAnalyzeTrend_decreasing() {
        List<Double> values = List.of(1.0, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1);
        String trend = invokeAnalyzeTrend(values);
        assertEquals("decreasing", trend);
    }

    @Test
    void testAnalyzeTrend_stable() {
        List<Double> values = List.of(0.5, 0.51, 0.49, 0.5, 0.5, 0.49, 0.51);
        String trend = invokeAnalyzeTrend(values);
        assertEquals("stable", trend);
    }

    // Reflection helpers for private methods
    private List<Double> invokeApplyDifferencing(List<Double> values, int order) {
        try {
            var method = AdvancedPredictiveMLService.class.getDeclaredMethod("applyDifferencing", List.class, int.class);
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Double> result = (List<Double>) method.invoke(mlService, values, order);
            return result;
        } catch (Exception e) {
            fail("Failed to invoke applyDifferencing: " + e.getMessage());
            return null;
        }
    }

    private double invokeCalculateVariance(List<Double> values) {
        try {
            var method = AdvancedPredictiveMLService.class.getDeclaredMethod("calculateVariance", List.class);
            method.setAccessible(true);
            return (double) method.invoke(mlService, values);
        } catch (Exception e) {
            fail("Failed to invoke calculateVariance: " + e.getMessage());
            return 0.0;
        }
    }

    private String invokeAnalyzeTrend(List<Double> values) {
        try {
            var method = AdvancedPredictiveMLService.class.getDeclaredMethod("analyzeTrend", List.class);
            method.setAccessible(true);
            return (String) method.invoke(mlService, values);
        } catch (Exception e) {
            fail("Failed to invoke analyzeTrend: " + e.getMessage());
            return null;
        }
    }
}
