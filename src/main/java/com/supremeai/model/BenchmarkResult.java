package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;

import java.time.LocalDateTime;

/**
 * Benchmark result document for Firestore benchmark_results collection.
 */
@Document(collectionName = "benchmark_results")
public class BenchmarkResult {
    @DocumentId
    private String id;
    private String providerName;
    private Double accuracy;
    private Double supremeAiAccuracy;
    private String topProviderName;
    private Double topProviderAccuracy;
    private LocalDateTime timestamp;

    public BenchmarkResult() {}

    public BenchmarkResult(String providerName, Double accuracy) {
        this.providerName = providerName;
        this.accuracy = accuracy;
        this.timestamp = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public Double getAccuracy() { return accuracy; }
    public void setAccuracy(Double accuracy) { this.accuracy = accuracy; }

    public Double getSupremeAiAccuracy() { return supremeAiAccuracy; }
    public void setSupremeAiAccuracy(Double supremeAiAccuracy) { this.supremeAiAccuracy = supremeAiAccuracy; }

    public String getTopProviderName() { return topProviderName; }
    public void setTopProviderName(String topProviderName) { this.topProviderName = topProviderName; }

    public Double getTopProviderAccuracy() { return topProviderAccuracy; }
    public void setTopProviderAccuracy(Double topProviderAccuracy) { this.topProviderAccuracy = topProviderAccuracy; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}