package com.supremeai.dto;

public class ApiKeyBulkDeleteResult {
    private String keyId;
    private boolean deleted;
    private String message;

    public ApiKeyBulkDeleteResult() {}

    public ApiKeyBulkDeleteResult(String keyId, boolean deleted, String message) {
        this.keyId = keyId;
        this.deleted = deleted;
        this.message = message;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}