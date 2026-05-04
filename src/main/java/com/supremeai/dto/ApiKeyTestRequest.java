package com.supremeai.dto;

import jakarta.validation.constraints.NotBlank;

public class ApiKeyTestRequest {
    @NotBlank(message = "API key ID is required")
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}