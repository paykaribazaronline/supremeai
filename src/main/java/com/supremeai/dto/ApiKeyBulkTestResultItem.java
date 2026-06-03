package com.supremeai.dto;

public class ApiKeyBulkTestResultItem {
    private String keyId;
    private boolean valid;
    private String message;

    public ApiKeyBulkTestResultItem() {}

    public ApiKeyBulkTestResultItem(String keyId, boolean valid, String message) {
        this.keyId = keyId;
        this.valid = valid;
        this.message = message;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}