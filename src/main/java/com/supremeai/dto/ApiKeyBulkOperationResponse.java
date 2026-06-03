package com.supremeai.dto;

import java.util.List;
import java.util.Map;

public class ApiKeyBulkOperationResponse {
    private boolean success;
    private String message;
    private int processedCount;
    private int successCount;
    private int failureCount;
    private List<Map<String, Object>> results;

    public ApiKeyBulkOperationResponse() {}

    public ApiKeyBulkOperationResponse(boolean success, String message, int processedCount, int successCount, int failureCount, List<Map<String, Object>> results) {
        this.success = success;
        this.message = message;
        this.processedCount = processedCount;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.results = results;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getProcessedCount() {
        return processedCount;
    }

    public void setProcessedCount(int processedCount) {
        this.processedCount = processedCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public List<Map<String, Object>> getResults() {
        return results;
    }

    public void setResults(List<Map<String, Object>> results) {
        this.results = results;
    }
}