package com.supremeai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public class ApiKeyCreateRequest {
  @NotBlank(message = "Provider is required")
  @Size(min = 1, max = 50, message = "Provider must be between 1 and 50 characters")
  private String provider;

  @NotBlank(message = "Label is required")
  @Size(min = 1, max = 100, message = "Label must be between 1 and 100 characters")
  private String label;

  @NotBlank(message = "API key is required")
  private String apiKey;

  @Size(min = 1, max = 500, message = "Base URL must be between 1 and 500 characters")
  private String baseUrl;

  private List<String> models;

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

  public List<String> getModels() {
    return models;
  }

  public void setModels(List<String> models) {
    this.models = models;
  }
}
