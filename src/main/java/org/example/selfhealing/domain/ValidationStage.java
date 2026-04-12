package org.example.selfhealing.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Single validation stage result
 * 
 * Represents one step in the validation pipeline:
 * - Static Analysis
 * - Unit Tests
 * - Security Scan
 * - Regression Tests
 */
public class ValidationStage {
    
    @JsonProperty("stageName")
    private String stageName;
    
    @JsonProperty("passed")
    private boolean passed;
    
    @JsonProperty("score")
    private double score; // 0.0 to 1.0
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("startedAt")
    private Instant startedAt;
    
    @JsonProperty("completedAt")
    private Instant completedAt;
    
    @JsonProperty("durationMs")
    private long durationMs;
    
    // Constructors
    public ValidationStage() {
    }
    
    public ValidationStage(String stageName) {
        this.stageName = stageName;
        this.startedAt = Instant.now();
    }
    
    public void complete(boolean passed, double score, String message) {
        this.passed = passed;
        this.score = score;
        this.message = message;
        this.completedAt = Instant.now();
        this.durationMs = completedAt.toEpochMilli() - startedAt.toEpochMilli();
    }
    
    // Getters & Setters
    public String getStageName() {
        return stageName;
    }
    
    public void setStageName(String stageName) {
        this.stageName = stageName;
    }
    
    public boolean isPassed() {
        return passed;
    }
    
    public void setPassed(boolean passed) {
        this.passed = passed;
    }
    
    public double getScore() {
        return score;
    }
    
    public void setScore(double score) {
        this.score = score;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Instant getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }
    
    public Instant getCompletedAt() {
        return completedAt;
    }
    
    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
    
    public long getDurationMs() {
        return durationMs;
    }
    
    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }
    
    @Override
    public String toString() {
        return "ValidationStage{" +
                "stageName='" + stageName + '\'' +
                ", passed=" + passed +
                ", score=" + score +
                ", durationMs=" + durationMs +
                '}';
    }
}
