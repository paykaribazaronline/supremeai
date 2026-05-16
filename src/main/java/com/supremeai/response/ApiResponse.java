package com.supremeai.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Standardized API Response format for all controllers.
 * @param <T> The type of the data being returned.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String error;
    private long timestamp;

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(Instant.now().toEpochMilli())
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        // We can reuse the error field for messages in success if needed, 
        // but traditionally we just put data.
        // For compatibility with calls using (message, data), we use this signature.
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(Instant.now().toEpochMilli())
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(message)
                .timestamp(Instant.now().toEpochMilli())
                .build();
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .data(data)
                .error(message)
                .timestamp(Instant.now().toEpochMilli())
                .build();
    }

    public static <T> ApiResponse<Object> validationError(Object errors) {
        return ApiResponse.<Object>builder()
                .success(false)
                .data(errors)
                .error("Validation failed")
                .timestamp(Instant.now().toEpochMilli())
                .build();
    }
}