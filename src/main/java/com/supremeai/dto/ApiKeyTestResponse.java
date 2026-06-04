package com.supremeai.dto;

public class ApiKeyTestResponse {
  private boolean success;
  private String message;
  private String response;

  public ApiKeyTestResponse() {}

  public ApiKeyTestResponse(boolean success, String message, String response) {
    this.success = success;
    this.message = message;
    this.response = response;
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

  public String getResponse() {
    return response;
  }

  public void setResponse(String response) {
    this.response = response;
  }
}
