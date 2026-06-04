package com.supremeai.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class ApiKeyBulkUpdateRequest {
  @NotNull(message = "Key IDs list is required")
  @NotEmpty(message = "Key IDs list cannot be empty")
  @Size(min = 1, max = 100, message = "Key IDs list must contain between 1 and 100 items")
  private List<String> keyIds;

  @Size(min = 1, max = 50, message = "Provider must be between 1 and 50 characters")
  private String provider;

  @Size(min = 1, max = 100, message = "Label must be between 1 and 100 characters")
  private String label;

  private String status;

  public List<String> getKeyIds() {
    return keyIds;
  }

  public void setKeyIds(List<String> keyIds) {
    this.keyIds = keyIds;
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

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
