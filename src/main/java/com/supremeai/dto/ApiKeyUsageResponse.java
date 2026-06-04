package com.supremeai.dto;

import java.util.Map;

public class ApiKeyUsageResponse {
  private long totalRequests;
  private long activeKeys;
  private double totalCost;
  private long providers;
  private long totalKeys;
  private Map<String, Map<String, Object>> byProvider;

  public ApiKeyUsageResponse() {}

  public ApiKeyUsageResponse(
      long totalRequests,
      long activeKeys,
      double totalCost,
      long providers,
      long totalKeys,
      Map<String, Map<String, Object>> byProvider) {
    this.totalRequests = totalRequests;
    this.activeKeys = activeKeys;
    this.totalCost = totalCost;
    this.providers = providers;
    this.totalKeys = totalKeys;
    this.byProvider = byProvider;
  }

  public long getTotalRequests() {
    return totalRequests;
  }

  public void setTotalRequests(long totalRequests) {
    this.totalRequests = totalRequests;
  }

  public long getActiveKeys() {
    return activeKeys;
  }

  public void setActiveKeys(long activeKeys) {
    this.activeKeys = activeKeys;
  }

  public double getTotalCost() {
    return totalCost;
  }

  public void setTotalCost(double totalCost) {
    this.totalCost = totalCost;
  }

  public long getProviders() {
    return providers;
  }

  public void setProviders(long providers) {
    this.providers = providers;
  }

  public long getTotalKeys() {
    return totalKeys;
  }

  public void setTotalKeys(long totalKeys) {
    this.totalKeys = totalKeys;
  }

  public Map<String, Map<String, Object>> getByProvider() {
    return byProvider;
  }

  public void setByProvider(Map<String, Map<String, Object>> byProvider) {
    this.byProvider = byProvider;
  }
}
