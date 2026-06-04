package com.supremeai.dto;

import java.util.List;
import java.util.Map;

public class ApiKeyBulkDeleteResponse {
  private boolean success;
  private String message;
  private int deletedCount;
  private List<String> deletedIds;
  private List<Map<String, Object>> results;

  public ApiKeyBulkDeleteResponse() {}

  public ApiKeyBulkDeleteResponse(
      boolean success,
      String message,
      int deletedCount,
      List<String> deletedIds,
      List<Map<String, Object>> results) {
    this.success = success;
    this.message = message;
    this.deletedCount = deletedCount;
    this.deletedIds = deletedIds;
    this.results = results;
  }

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public int getDeletedCount() {
    return deletedCount;
  }

  public void setDeletedCount(int deletedCount) {
    this.deletedCount = deletedCount;
  }

  public List<String> getDeletedIds() {
    return deletedIds;
  }

  public void setDeletedIds(List<String> deletedIds) {
    this.deletedIds = deletedIds;
  }

  public List<Map<String, Object>> getResults() {
    return results;
  }

  public void setResults(List<Map<String, Object>> results) {
    this.results = results;
  }
}
