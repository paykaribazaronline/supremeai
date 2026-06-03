package com.supremeai.intelligence.profiling;

public class TaskPerformanceProfile {
    private int totalAttempts = 0;
    private int successCount = 0;
    private long totalExecutionTimeMs = 0;

    public synchronized void update(boolean success, long executionTimeMs) {
        this.totalAttempts++;
        if (success) {
            this.successCount++;
        }
        this.totalExecutionTimeMs += executionTimeMs;
    }

    public double getSuccessRate() {
        if (totalAttempts == 0) return 0.0;
        return (double) successCount / totalAttempts;
    }

    public long getAverageSpeedMs() {
        if (totalAttempts == 0) return 0;
        return totalExecutionTimeMs / totalAttempts;
    }

    /**
     * Score combines high success rate and low latency (speed).
     */
    public double calculateOverallScore() {
        if (totalAttempts == 0 || successCount == 0) return 0.0;
        
        double successRate = getSuccessRate();
        long avgSpeed = getAverageSpeedMs();
        
        if (avgSpeed <= 0) avgSpeed = 1;

        return (successRate * 0.70) + ((1000.0 / avgSpeed) * 0.30);
    }
}