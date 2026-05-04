package com.supremeai.dto;

public class ApiKeyTestResult {
    private String keyId;
    private String provider;
    private boolean valid;
    private String message;
    private String response;

    public ApiKeyTestResult() {}

    public ApiKeyTestResult(String keyId, String provider, boolean valid, String message, String response) {
        this.keyId = keyId;
        this.provider = provider;
        this.valid = valid;
        this.message = message;
        this.response = response;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
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

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}