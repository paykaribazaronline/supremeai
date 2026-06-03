package com.supremeai.dto;

import java.util.List;
import java.util.Map;

public class ApiKeyBulkRegenerateResponse {
    private boolean success;
    private String message;
    private int regeneratedCount;
    private List<String> regeneratedIds;
    private List<Map<String, Object>> results;

    public ApiKeyBulkRegenerateResponse() {}

    public ApiKeyBulkRegenerateResponse(boolean success, String message, int regeneratedCount, List<String> regeneratedIds, List<Map<String, Object>> results) {
        this.success = success;
        this.message = message;
        this.regeneratedCount = regeneratedCount;
        this.regeneratedIds = regeneratedIds;
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

    public int getRegeneratedCount() {
        return regeneratedCount;
    }

    public void setRegeneratedCount(int regeneratedCount) {
        this.regeneratedCount = regeneratedCount;
    }

    public List<String> getRegeneratedIds() {
        return regeneratedIds;
    }

    public void setRegeneratedIds(List<String> regeneratedIds) {
        this.regeneratedIds = regeneratedIds;
    }

    public List<Map<String, Object>> getResults() {
        return results;
    }

    public void setResults(List<Map<String, Object>> results) {
        this.results = results;
    }
}