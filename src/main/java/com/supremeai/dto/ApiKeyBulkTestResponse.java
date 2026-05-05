package com.supremeai.dto;

import java.util.List;
import java.util.Map;

public class ApiKeyBulkTestResponse {
    private boolean success;
    private String message;
    private List<Map<String, Object>> results;

    public ApiKeyBulkTestResponse() {}

    public ApiKeyBulkTestResponse(boolean success, String message, List<Map<String, Object>> results) {
        this.success = success;
        this.message = message;
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

    public List<Map<String, Object>> getResults() {
        return results;
    }

    public void setResults(List<Map<String, Object>> results) {
        this.results = results;
    }
}