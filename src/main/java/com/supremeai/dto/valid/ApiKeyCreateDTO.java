package com.supremeai.dto.valid;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** DTO for API key creation with validation. */
public class ApiKeyCreateDTO {

  @NotBlank(message = "Provider is required")
  @Size(max = 50, message = "Provider name must not exceed 50 characters")
  private String provider;

  @NotBlank(message = "Label is required")
  @Size(max = 100, message = "Label must not exceed 100 characters")
  private String label;

  @NotBlank(message = "API key is required")
  @Size(min = 10, max = 500, message = "API key must be between 10 and 500 characters")
  private String apiKey;

  @Size(max = 500, message = "Base URL must not exceed 500 characters")
  @Pattern(
      regexp = "^(https?://)?[a-zA-Z0-9.-]+(:[0-9]+)?(/.*)?$",
      message = "Invalid base URL format")
  private String baseUrl;

  private List<@Size(max = 100, message = "Model name must not exceed 100 characters") String>
      models;

  public ApiKeyCreateDTO() {}

  public ApiKeyCreateDTO(
      String provider, String label, String apiKey, String baseUrl, List<String> models) {
    this.provider = provider;
    this.label = label;
    this.apiKey = apiKey;
    this.baseUrl = baseUrl;
    this.models = models;
  }

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
