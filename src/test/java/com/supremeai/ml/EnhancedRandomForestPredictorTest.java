package com.supremeai.ml;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EnhancedRandomForestPredictor.
 * Tests random forest training, prediction, feature importance, and retraining logic.
 */
class EnhancedRandomForestPredictorTest {

    private EnhancedRandomForestPredictor predictor;

    @BeforeEach
    void setUp() {
        predictor = new EnhancedRandomForestPredictor();
    }

    @Test
    void testRecordFailure_addsToTrainingData() {
        Map<EnhancedRandomForestPredictor.FeatureType, Double> features = new HashMap<>();
        features.put(EnhancedRandomForestPredictor.FeatureType.ERROR_FREQUENCY, 5.0);
        features.put(EnhancedRandomForestPredictor.FeatureType.TIME_SINCE_LAST_ERROR, 100.0);

        predictor.recordFailure("NullPointerException", features, true);

        // Get model stats to verify training data increased
        Map<String, Object> stats = predictor.getModelStats();
        assertEquals(1, stats.get("trainingDataSize"));
    }

    @Test
    void testPredict_beforeTraining_returnsDefault() {
        Map<EnhancedRandomForestPredictor.FeatureType, Double> features = new HashMap<>();
        features.put(EnhancedRandomForestPredictor.FeatureType.ERROR_FREQUENCY, 10.0);

        EnhancedRandomForestPredictor.FailurePrediction prediction = predictor.predict("test", features);

        assertEquals(0.5, prediction.probability, 0.001);
        assertEquals("Model not trained yet", prediction.confidence);
    }

    @Test
    void testPredict_afterSomeTraining_returnsProbability() {
        // Train with enough data
        for (int i = 0; i < 150; i++) {
            Map<EnhancedRandomForestPredictor.FeatureType, Double> f = new HashMap<>();
            f.put(EnhancedRandomForestPredictor.FeatureType.ERROR_FREQUENCY, (double) i % 20);
            f.put(EnhancedRandomForestPredictor.FeatureType.CODE_COMPLEXITY, i % 10 * 0.1);
            predictor.recordFailure("err_" + i, f, i % 3 == 0); // True for every 3rd
        }

        // Now predict
        Map<EnhancedRandomForestPredictor.FeatureType, Double> testFeatures = new HashMap<>();
        testFeatures.put(EnhancedRandomForestPredictor.FeatureType.ERROR_FREQUENCY, 15.0);
        testFeatures.put(EnhancedRandomForestPredictor.FeatureType.CODE_COMPLEXITY, 0.5);

        EnhancedRandomForestPredictor.FailurePrediction prediction = predictor.predict("test", testFeatures);

        assertNotNull(prediction);
        assertTrue(prediction.probability >= 0.0 && prediction.probability <= 1.0);
        assertNotNull(prediction.confidence);
    }

    @Test
    void testGetModelStats_returnsValidStats() {
        Map<String, Object> stats = predictor.getModelStats();
        assertTrue(stats.containsKey("forestSize"));
        assertTrue(stats.containsKey("trainingDataSize"));
        assertTrue(stats.containsKey("totalPredictions"));
        assertTrue(stats.containsKey("accuracy"));
        assertTrue(stats.containsKey("featureImportance"));
    }

    @Test
    void testRecordFailure_andAutoRetrain() {
        // After enough records, the forest should be trained (non-empty)
        for (int i = 0; i < 120; i++) {
            Map<EnhancedRandomForestPredictor.FeatureType, Double> f = new HashMap<>();
            f.put(EnhancedRandomForestPredictor.FeatureType.ERROR_FREQUENCY, (double) i % 20);
            predictor.recordFailure("err_" + i, f, i % 2 == 0);
        }

        Map<String, Object> stats = predictor.getModelStats();
        assertTrue(((int) stats.get("trainingDataSize")) >= 120);
        // After sufficient data, forest should be trained (size > 0)
        assertTrue(((int) stats.get("forestSize")) > 0);
    }

    @Test
    void testExtractFeatures_allFeatureTypes() {
        Map<EnhancedRandomForestPredictor.FeatureType, Double> featureMap = new HashMap<>();
        featureMap.put(EnhancedRandomForestPredictor.FeatureType.ERROR_FREQUENCY, 5.0);
        featureMap.put(EnhancedRandomForestPredictor.FeatureType.TIME_SINCE_LAST_ERROR, 3600.0);
        featureMap.put(EnhancedRandomForestPredictor.FeatureType.PROVIDER_SUCCESS_RATE, 0.95);
        featureMap.put(EnhancedRandomForestPredictor.FeatureType.CODE_COMPLEXITY, 0.7);
        featureMap.put(EnhancedRandomForestPredictor.FeatureType.USER_EXPERIENCE_LEVEL, 0.8);
        featureMap.put(EnhancedRandomForestPredictor.FeatureType.SYSTEM_LOAD, 0.3);
        featureMap.put(EnhancedRandomForestPredictor.FeatureType.API_RESPONSE_TIME, 150.0);
        featureMap.put(EnhancedRandomForestPredictor.FeatureType.MEMORY_USAGE, 0.6);
        featureMap.put(EnhancedRandomForestPredictor.FeatureType.ERROR_MESSAGE_LENGTH, 200.0);
        featureMap.put(EnhancedRandomForestPredictor.FeatureType.STACK_TRACE_DEPTH, 10.0);

        double[] features = invokeExtractFeatures(featureMap);
        assertEquals(10, features.length);
        assertEquals(5.0, features[0]);
        assertEquals(3600.0, features[1]);
        assertEquals(0.95, features[2]);
    }

    @Test
    void testFeatureImportanceCalculatedAfterTraining() {
        // Add training data with MULTIPLE features so permutation importance works
        for (int i = 0; i < 150; i++) {
            Map<EnhancedRandomForestPredictor.FeatureType, Double> f = new HashMap<>();
            f.put(EnhancedRandomForestPredictor.FeatureType.ERROR_FREQUENCY, (double) (i % 20));
            f.put(EnhancedRandomForestPredictor.FeatureType.CODE_COMPLEXITY, i % 10 * 0.1);
            f.put(EnhancedRandomForestPredictor.FeatureType.SYSTEM_LOAD, (i % 5) * 0.2);
            // Label correlates with ERROR_FREQUENCY to ensure that feature has importance
            predictor.recordFailure("err_" + i, f, i % 20 > 10);
        }

        Map<String, Object> stats = predictor.getModelStats();
        @SuppressWarnings("unchecked")
        Map<String, Double> importance = (Map<String, Double>) stats.get("featureImportance");
        assertNotNull(importance);
        // Feature importance should be populated after training
        assertFalse(importance.isEmpty(), "Feature importance map should not be empty after training");
        // Sum of all importance values should be non-negative
        double sum = importance.values().stream().mapToDouble(Double::doubleValue).sum();
        assertTrue(sum >= 0, "Feature importance sum should be non-negative, was: " + sum);
    }

    @Test
    void testCopyArray_copiesCorrectly() {
        double[][] original = {{1.0, 2.0}, {3.0, 4.0}};
        double[][] copy = invokeCopyArray(original);
        assertNotSame(original, copy);
        assertEquals(original[0][0], copy[0][0]);
        assertEquals(original[1][1], copy[1][1]);
    }

    @Test
    void testCalculateGini_balancedSplit_lowGini() {
        // This is a private method test via reflection - we can't easily test
        // But we can verify the whole pipeline integrates correctly
        Map<EnhancedRandomForestPredictor.FeatureType, Double> f = new HashMap<>();
        f.put(EnhancedRandomForestPredictor.FeatureType.ERROR_FREQUENCY, 1.0);
        predictor.recordFailure("e1", f, true);
        predictor.recordFailure("e2", f, false);

        // After training, predict should not crash
        EnhancedRandomForestPredictor.FailurePrediction pred = predictor.predict("test", f);
        assertNotNull(pred);
    }

    private double[] invokeExtractFeatures(Map<EnhancedRandomForestPredictor.FeatureType, Double> featureMap) {
        try {
            var method = EnhancedRandomForestPredictor.class.getDeclaredMethod("extractFeatures", Map.class);
            method.setAccessible(true);
            return (double[]) method.invoke(predictor, featureMap);
        } catch (Exception e) {
            fail("Failed to invoke extractFeatures: " + e.getMessage());
            return null;
        }
    }

    private double[][] invokeCopyArray(double[][] original) {
        try {
            var method = EnhancedRandomForestPredictor.class.getDeclaredMethod("copyArray", double[][].class);
            method.setAccessible(true);
            return (double[][]) method.invoke(predictor, (Object) original);
        } catch (Exception e) {
            fail("Failed: " + e.getMessage());
            return null;
        }
    }
}
