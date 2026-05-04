package com.supremeai.dto;

import jakarta.validation.constraints.NotBlank;

public class ApiKeyTestRequestBody {
    @NotBlank(message = "Provider name is required")
    private String name;

    @NotBlank(message = "API key is required")
    private String apiKey;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}