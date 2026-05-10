package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collectionName = "system_learning/recommendations")
public class KnowledgeRecommendation {
    @DocumentId
    private String id;
    private String topic;
    private String reasoning;
    private Double confidence;
    private List<String> keywords;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private Map<String, Object> sourceMetrics;

    public enum Status {
        PENDING, APPROVED, DECLINED
    }

    public KnowledgeRecommendation() {}

    public KnowledgeRecommendation(String topic, String reasoning, Double confidence, List<String> keywords) {
        this.id = "rec_" + System.currentTimeMillis();
        this.topic = topic;
        this.reasoning = reasoning;
        this.confidence = confidence;
        this.keywords = keywords;
        this.status = Status.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    public Map<String, Object> getSourceMetrics() { return sourceMetrics; }
    public void setSourceMetrics(Map<String, Object> sourceMetrics) { this.sourceMetrics = sourceMetrics; }
}