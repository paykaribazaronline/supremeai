package com.supremeai.model;

import java.time.LocalDateTime;
import java.util.Map;

public class ErrorResponse {
  private String error;
  private String message;
  private int status;
  private LocalDateTime timestamp;
  private Map<String, Object> details;

  public ErrorResponse() {}

  public ErrorResponse(
      String error,
      String message,
      int status,
      LocalDateTime timestamp,
      Map<String, Object> details) {
    this.error = error;
    this.message = message;
    this.status = status;
    this.timestamp = timestamp;
    this.details = details;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(LocalDateTime timestamp) {
    this.timestamp = timestamp;
  }

  public Map<String, Object> getDetails() {
    return details;
  }

  public void setDetails(Map<String, Object> details) {
    this.details = details;
  }

  public static ErrorResponseBuilder builder() {
    return new ErrorResponseBuilder();
  }

  public static class ErrorResponseBuilder {
    private String error;
    private String message;
    private int status;
    private LocalDateTime timestamp;
    private Map<String, Object> details;

    ErrorResponseBuilder() {}

    public ErrorResponseBuilder error(String error) {
      this.error = error;
      return this;
    }

    public ErrorResponseBuilder message(String message) {
      this.message = message;
      return this;
    }

    public ErrorResponseBuilder status(int status) {
      this.status = status;
      return this;
    }

    public ErrorResponseBuilder timestamp(LocalDateTime timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public ErrorResponseBuilder details(Map<String, Object> details) {
      this.details = details;
      return this;
    }

    public ErrorResponse build() {
      return new ErrorResponse(error, message, status, timestamp, details);
    }
  }
}
