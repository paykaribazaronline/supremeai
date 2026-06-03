package com.supremeai.dto;

public class ApiKeyBulkUpdateResult {
    private String keyId;
    private boolean updated;
    private String message;

    public ApiKeyBulkUpdateResult() {}

    public ApiKeyBulkUpdateResult(String keyId, boolean updated, String message) {
        this.keyId = keyId;
        this.updated = updated;
        this.message = message;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}