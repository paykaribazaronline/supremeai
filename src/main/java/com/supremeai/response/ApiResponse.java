package com.supremeai.response;

import java.time.Instant;

/**
 * Standardized API Response format for all controllers.
 * @param <T> The type of the data being returned.
 */
public record ApiResponse<T>(
    boolean success,
    T data,
    String error,
    long timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, Instant.now().toEpochMilli());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message, Instant.now().toEpochMilli());
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(false, data, message, Instant.now().toEpochMilli());
    }

    public static <T> ApiResponse<Object> validationError(Object errors) {
        return new ApiResponse<>(false, errors, "Validation failed", Instant.now().toEpochMilli());
    }
}