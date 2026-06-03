package com.supremeai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ApiKeyUpdateRequestBody {
    @Size(min = 1, max = 50, message = "Provider must be between 1 and 50 characters")
    private String provider;

    @Size(min = 1, max = 100, message = "Label must be between 1 and 100 characters")
    private String label;

    private String apiKey;

    @Size(min = 1, max = 500, message = "Base URL must be between 1 and 500 characters")
    private String baseUrl;

    private String status;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}