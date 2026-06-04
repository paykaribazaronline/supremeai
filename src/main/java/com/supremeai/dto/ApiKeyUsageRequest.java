package com.supremeai.dto;

import jakarta.validation.constraints.NotBlank;

public class ApiKeyUsageRequest {
  @NotBlank(message = "Start date is required")
  private String startDate;

  @NotBlank(message = "End date is required")
  private String endDate;

  @NotBlank(message = "Provider is required")
  private String provider;

  // Getters and Setters
  public String getStartDate() {
    return startDate;
  }

  public void setStartDate(String startDate) {
    this.startDate = startDate;
  }

  public String getEndDate() {
    return endDate;
  }

  public void setEndDate(String endDate) {
    this.endDate = endDate;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }
}
