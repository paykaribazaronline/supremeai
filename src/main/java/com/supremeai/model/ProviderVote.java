package com.supremeai.model;

import java.time.Instant;

public class ProviderVote {
    private String providerName;
    private String response;
    private double confidence; // 0.0-1.0
    private long timestamp;

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
}
