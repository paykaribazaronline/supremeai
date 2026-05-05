package com.supremeai.ml;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class IsolationForestTest {

    @Test
    public void testAnomalyDetection() {
        IsolationForest forest = new IsolationForest();
        
        // Training data: points clustered around (1.0, 1.0)
        double[][] trainingData = new double[100][2];
        for (int i = 0; i < 100; i++) {
            trainingData[i][0] = 1.0 + Math.random() * 0.1;
            trainingData[i][1] = 1.0 + Math.random() * 0.1;
        }
        
        forest.train(trainingData);
        
        // Normal point
        double[] normalPoint = {1.05, 1.05};
        double normalScore = forest.computeAnomalyScore(normalPoint);
        
        // Anomaly point
        double[] anomalyPoint = {5.0, 5.0};
        double anomalyScore = forest.computeAnomalyScore(anomalyPoint);
        
        System.out.println("Normal score: " + normalScore);
        System.out.println("Anomaly score: " + anomalyScore);
        
        assertTrue(anomalyScore > normalScore, "Anomaly score should be higher than normal score");
        assertTrue(normalScore < 0.6, "Normal score should be low");
        assertTrue(anomalyScore > 0.5, "Anomaly score should be relatively high");
    }
}
