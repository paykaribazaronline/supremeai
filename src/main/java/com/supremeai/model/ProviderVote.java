package com.supremeai.model;

import java.time.Instant;

public class ProviderVote {
    private String providerName;
    private String response;
    private double confidence; // 0.0-1.0
    private long timestamp;
    private long latencyMs;
    private boolean success;
    private String errorMessage;

    public ProviderVote() {}

    public ProviderVote(String providerName, String response, double confidence, long timestamp) {
        this.providerName = providerName;
        this.response = response;
        this.confidence = confidence;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    
    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }
    
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
