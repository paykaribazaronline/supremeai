package org.example.model;

import java.util.*;

/**
 * Research Topic - A subject SupremeAI investigates during idle time
 */
public class ResearchTopic {
    private String id = UUID.randomUUID().toString();
    private String domain;        // ARCHITECTURE, SECURITY, PERFORMANCE, DEVOPS, AI_ML, TESTING, etc.
    private String topic;         // Specific topic name
    private String question;      // Research question to investigate
    private String status;        // QUEUED, RESEARCHING, COMPLETED, FAILED
    private String summary;       // Research summary / findings
    private List<String> findings = new ArrayList<>();
    private List<String> actionableInsights = new ArrayList<>();
    private Double confidenceScore = 0.0;
    private int researchDepth = 0;       // How many AI providers contributed
    private long createdAt = System.currentTimeMillis();
    private long completedAt = 0;
    private String source;        // What triggered this research: IDLE_SCAN, GAP_ANALYSIS, ERROR_PATTERN, TREND

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<String> getFindings() { return findings; }
    public void setFindings(List<String> findings) { this.findings = findings; }
    public void addFinding(String finding) { this.findings.add(finding); }

    public List<String> getActionableInsights() { return actionableInsights; }
    public void setActionableInsights(List<String> insights) { this.actionableInsights = insights; }
    public void addInsight(String insight) { this.actionableInsights.add(insight); }

    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double score) { this.confidenceScore = score; }

    public int getResearchDepth() { return researchDepth; }
    public void setResearchDepth(int depth) { this.researchDepth = depth; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long ts) { this.createdAt = ts; }

    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long ts) { this.completedAt = ts; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
