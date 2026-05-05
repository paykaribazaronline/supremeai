package com.supremeai.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standardized API response wrapper for consistent REST API responses.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    private boolean success;
    private T data;
    private String message;
    private String error;
    private int status;
    private String timestamp;
    private Map<String, String> errors;
    
    private ApiResponse() {}
    
    private ApiResponse(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.timestamp = LocalDateTime.now().toString();
    }
    
    private ApiResponse(boolean success, String error, int status) {
        this.success = success;
        this.error = error;
        this.status = status;
        this.timestamp = LocalDateTime.now().toString();
    }
    
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }
    
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message);
    }
    
    public static <T> ApiResponse<T> successWithMessage(String message) {
        return new ApiResponse<>(true, null, message);
    }
    
    public static <T> ApiResponse<T> error(String error, int status) {
        return new ApiResponse<>(false, error, status);
    }
    
    public static <T> ApiResponse<T> validationError(Map<String, String> errors) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.errors = errors;
        response.status = 400;
        response.error = "Validation Failed";
        response.message = "Invalid request parameters";
        response.timestamp = LocalDateTime.now().toString();
        return response;
    }
    
    // Getters
    public boolean isSuccess() { return success; }
    public T getData() { return data; }
    public String getMessage() { return message; }
    public String getError() { return error; }
    public int getStatus() { return status; }
    public String getTimestamp() { return timestamp; }
    public Map<String, String> getErrors() { return errors; }
    
    // Setters for flexibility
    public void setSuccess(boolean success) { this.success = success; }
    public void setData(T data) { this.data = data; }
    public void setMessage(String message) { this.message = message; }
    public void setError(String error) { this.error = error; }
    public void setStatus(int status) { this.status = status; }
    public void setErrors(Map<String, String> errors) { this.errors = errors; }
}