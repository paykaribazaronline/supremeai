package org.example.model;

import java.util.*;

/**
 * System Learning - SupremeAI's Memory
 * Tracks errors, patterns, requirements, improvements
 */
public class SystemLearning {
    private String id = UUID.randomUUID().toString();
    private String type; // ERROR, PATTERN, REQUIREMENT, IMPROVEMENT
    private String category; // GIT, AUTH, VALIDATION, SECURITY, etc
    private String content;
    private Integer errorCount = 0;
    private List<String> solutions = new ArrayList<>();
    private Map<String, Object> context = new HashMap<>();
    private long timestamp = System.currentTimeMillis();
    private String severity; // CRITICAL, HIGH, MEDIUM, LOW
    private Boolean resolved = false;
    private String resolution;
    private Integer timesApplied = 0; // How many times this learning prevented error
    private Double confidenceScore = 0.0; // 0-1: How confident about this solution
    
    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public Integer getErrorCount() { return errorCount; }
    public void setErrorCount(Integer count) { this.errorCount = count; }
    public void incrementErrorCount() { this.errorCount++; }
    
    public List<String> getSolutions() { return solutions; }
    public void setSolutions(List<String> solutions) { this.solutions = solutions; }
    public void addSolution(String solution) { this.solutions.add(solution); }
    
    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public Boolean getResolved() { return resolved; }
    public void setResolved(Boolean resolved) { this.resolved = resolved; }
    
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
    
    public Integer getTimesApplied() { return timesApplied; }
    public void incrementTimesApplied() { this.timesApplied++; }
    
    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double score) { this.confidenceScore = score; }
}
