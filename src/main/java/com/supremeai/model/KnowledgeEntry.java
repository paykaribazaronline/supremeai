package com.supremeai.model;

import java.time.LocalDateTime;

public class KnowledgeEntry {
    private String id;
    private String topic;
    private String pattern;
    private String solution;
    private String sourceProvider;
    private double confidenceScore;
    private LocalDateTime createdAt;

    public KnowledgeEntry() {}

    public KnowledgeEntry(String id, String topic, String pattern, String solution, String sourceProvider, double confidenceScore, LocalDateTime createdAt) {
        this.id = id;
        this.topic = topic;
        this.pattern = pattern;
        this.solution = solution;
        this.sourceProvider = sourceProvider;
        this.confidenceScore = confidenceScore;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }
    public String getSolution() { return solution; }
    public void setSolution(String solution) { this.solution = solution; }
    public String getSourceProvider() { return sourceProvider; }
    public void setSourceProvider(String sourceProvider) { this.sourceProvider = sourceProvider; }
    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String topic;
        private String pattern;
        private String solution;
        private String sourceProvider;
        private double confidenceScore;
        private LocalDateTime createdAt;

        public Builder id(String id) { this.id = id; return this; }
        public Builder topic(String topic) { this.topic = topic; return this; }
        public Builder pattern(String pattern) { this.pattern = pattern; return this; }
        public Builder solution(String solution) { this.solution = solution; return this; }
        public Builder sourceProvider(String sourceProvider) { this.sourceProvider = sourceProvider; return this; }
        public Builder confidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public KnowledgeEntry build() {
            return new KnowledgeEntry(id, topic, pattern, solution, sourceProvider, confidenceScore, createdAt);
        }
    }
}
