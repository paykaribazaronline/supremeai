package com.supremeai.learning.knowledge;

import java.util.Map;

public class SolutionMemory {
    private String triggerError;
    private String resolvedCode;
    private String workingAIProvider;
    private long timestamp;
    private int successCount; // How many times this solution fixed the same issue later

    public SolutionMemory(String triggerError, String resolvedCode, String workingAIProvider) {
        this.triggerError = triggerError;
        this.resolvedCode = resolvedCode;
        this.workingAIProvider = workingAIProvider;
        this.timestamp = System.currentTimeMillis();
        this.successCount = 1;
    }

    public void incrementSuccess() {
        this.successCount++;
    }

    // Getters
    public String getTriggerError() { return triggerError; }
    public String getResolvedCode() { return resolvedCode; }
    public String getWorkingAIProvider() { return workingAIProvider; }
    public int getSuccessCount() { return successCount; }
}