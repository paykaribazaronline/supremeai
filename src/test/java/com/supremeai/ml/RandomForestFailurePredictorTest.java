package com.supremeai.ml;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RandomForestFailurePredictorTest {

    @Test
    public void testFailurePrediction() {
        RandomForestFailurePredictor predictor = new RandomForestFailurePredictor();
        
        // Training data:
        // [0.1, 0.1] -> Label 0 (Normal)
        // [0.9, 0.9] -> Label 1 (Failure)
        int size = 100;
        double[][] trainingData = new double[size][2];
        int[] labels = new int[size];
        
        for (int i = 0; i < size / 2; i++) {
            trainingData[i][0] = Math.random() * 0.2;
            trainingData[i][1] = Math.random() * 0.2;
            labels[i] = 0;
        }
        for (int i = size / 2; i < size; i++) {
            trainingData[i][0] = 0.8 + Math.random() * 0.2;
            trainingData[i][1] = 0.8 + Math.random() * 0.2;
            labels[i] = 1;
        }
        
        predictor.train(trainingData, labels);
        
        // Test normal
        double[] normalTest = {0.1, 0.1};
        int normalPred = predictor.predict(normalTest);
        assertEquals(0, normalPred, "Should predict normal");
        
        // Test failure
        double[] failureTest = {0.9, 0.9};
        int failurePred = predictor.predict(failureTest);
        assertEquals(1, failurePred, "Should predict failure");
    }
}
