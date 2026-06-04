package com.supremeai.dto;

import java.util.List;

public class ApiKeyBulkTestResult {
  private List<ApiKeyTestResult> results;
  private int totalTested;
  private int validCount;
  private int invalidCount;

  public ApiKeyBulkTestResult() {}

  public ApiKeyBulkTestResult(
      List<ApiKeyTestResult> results, int totalTested, int validCount, int invalidCount) {
    this.results = results;
    this.totalTested = totalTested;
    this.validCount = validCount;
    this.invalidCount = invalidCount;
  }

  public List<ApiKeyTestResult> getResults() {
    return results;
  }

  public void setResults(List<ApiKeyTestResult> results) {
    this.results = results;
  }

  public int getTotalTested() {
    return totalTested;
  }

  public void setTotalTested(int totalTested) {
    this.totalTested = totalTested;
  }

  public int getValidCount() {
    return validCount;
  }

  public void setValidCount(int validCount) {
    this.validCount = validCount;
  }

  public int getInvalidCount() {
    return invalidCount;
  }

  public void setInvalidCount(int invalidCount) {
    this.invalidCount = invalidCount;
  }
}
