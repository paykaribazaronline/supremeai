package com.supremeai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ApiKeyRequest {
  @NotBlank(message = "Provider is required")
  @Size(min = 1, max = 50, message = "Provider must be between 1 and 50 characters")
  private String provider;

  @NotBlank(message = "API key is required")
  private String apiKey;

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }
}
