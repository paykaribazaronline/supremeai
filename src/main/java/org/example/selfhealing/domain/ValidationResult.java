package org.example.selfhealing.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

/**
 * Validation result for code fixes
 * 
 * Tracks whether a generated fix passed all validation stages:
 * - Static analysis (no syntax errors, no security issues)  
 * - Tests (unit tests pass)
 * - Regression tests (no new failures)
 * - Security scan (no vulnerabilities introduced)
 */
public class ValidationResult {
    
    @JsonProperty("passed")
    private boolean passed;
    
    @JsonProperty("stages")
    private List<ValidationStage> stages = new ArrayList<>();
    
    @JsonProperty("securityVulnerabilities")
    private List<String> securityVulnerabilities = new ArrayList<>();
    
    @JsonProperty("staticAnalysisErrors")
    private List<String> staticAnalysisErrors = new ArrayList<>();
    
    @JsonProperty("failedTests")
    private List<String> failedTests = new ArrayList<>();
    
    @JsonProperty("overallScore")
    private double overallScore; // 0.0 to 1.0
    
    @JsonProperty("notes")
    private String notes;
    
    // Constructors
    public ValidationResult() {
        this.passed = true;
        this.overallScore = 0.0;
    }
    
    // Getters & Setters
    public boolean isPassed() {
        return passed;
    }
    
    public void setPassed(boolean passed) {
        this.passed = passed;
    }
    
    public List<ValidationStage> getStages() {
        return stages;
    }
    
    public void setStages(List<ValidationStage> stages) {
        this.stages = stages;
    }
    
    public void addStage(ValidationStage stage) {
        this.stages.add(stage);
        recalculateOverallScore();
    }
    
    public List<String> getSecurityVulnerabilities() {
        return securityVulnerabilities;
    }
    
    public void addSecurityVulnerability(String vulnerability) {
        this.securityVulnerabilities.add(vulnerability);
        this.passed = false;
    }
    
    public List<String> getStaticAnalysisErrors() {
        return staticAnalysisErrors;
    }
    
    public void addStaticAnalysisError(String error) {
        this.staticAnalysisErrors.add(error);
        this.passed = false;
    }
    
    public List<String> getFailedTests() {
        return failedTests;
    }
    
    public void addFailedTest(String test) {
        this.failedTests.add(test);
        this.passed = false;
    }
    
    public double getOverallScore() {
        return overallScore;
    }
    
    public void setOverallScore(double overallScore) {
        this.overallScore = overallScore;
    }
    
    private void recalculateOverallScore() {
        if (stages.isEmpty()) {
            return;
        }
        
        double sum = stages.stream()
                .mapToDouble(ValidationStage::getScore)
                .sum();
        this.overallScore = sum / stages.size();
        this.passed = this.overallScore >= 0.85; // Require 85% pass rate
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    @Override
    public String toString() {
        return "ValidationResult{" +
                "passed=" + passed +
                ", overallScore=" + overallScore +
                ", stages=" + stages.size() +
                ", vulnerabilities=" + securityVulnerabilities.size() +
                '}';
    }
}
