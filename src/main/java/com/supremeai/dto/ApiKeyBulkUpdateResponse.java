package com.supremeai.dto;

import java.util.List;
import java.util.Map;

public class ApiKeyBulkUpdateResponse {
    private boolean success;
    private String message;
    private int updatedCount;
    private List<String> updatedIds;
    private List<Map<String, Object>> results;

    public ApiKeyBulkUpdateResponse() {}

    public ApiKeyBulkUpdateResponse(boolean success, String message, int updatedCount, List<String> updatedIds, List<Map<String, Object>> results) {
        this.success = success;
        this.message = message;
        this.updatedCount = updatedCount;
        this.updatedIds = updatedIds;
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

    public int getUpdatedCount() {
        return updatedCount;
    }

    public void setUpdatedCount(int updatedCount) {
        this.updatedCount = updatedCount;
    }

    public List<String> getUpdatedIds() {
        return updatedIds;
    }

    public void setUpdatedIds(List<String> updatedIds) {
        this.updatedIds = updatedIds;
    }

    public List<Map<String, Object>> getResults() {
        return results;
    }

    public void setResults(List<Map<String, Object>> results) {
        this.results = results;
    }
}