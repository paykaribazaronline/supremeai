package org.example.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.*;

/**
 * Phase 5: Global REST Exception Handler
 */
@RestControllerAdvice
public class RestExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(RestExceptionHandler.class);

    /**
     * Handle resource not found (404)
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<?> handleNotFound(NoHandlerFoundException e, WebRequest request) {
        String path = e.getRequestURL();
        logger.debug("❌ Endpoint not found: {} {}", e.getHttpMethod(), path);

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error", path);
        error.put("status", "error");
        error.put("code", "NOT_FOUND");
        error.put("message", "Endpoint not found: " + path);
        error.put("path", path);
        error.put("timestamp", String.valueOf(System.currentTimeMillis()));

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handle HTTP method not allowed (405)
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e, WebRequest request) {
        logger.debug("⚠️ Method not allowed: {}", e.getMessage());

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error", "METHOD_NOT_ALLOWED");
        error.put("status", "error");
        error.put("code", "METHOD_NOT_ALLOWED");
        error.put("message", e.getMessage());
        error.put("timestamp", String.valueOf(System.currentTimeMillis()));

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    /**
     * Handle unsupported media type (415)
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<?> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException e, WebRequest request) {
        logger.debug("⚠️ Unsupported media type: {}", e.getMessage());

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error", "UNSUPPORTED_MEDIA_TYPE");
        error.put("status", "error");
        error.put("code", "UNSUPPORTED_MEDIA_TYPE");
        error.put("message", e.getMessage());
        error.put("timestamp", String.valueOf(System.currentTimeMillis()));

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(error);
    }

    /**
     * Handle illegal argument exceptions (400)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e, WebRequest request) {
        logger.debug("⚠️ Invalid argument: {}", e.getMessage());

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error", "INVALID_ARGUMENT");
        error.put("status", "error");
        error.put("code", "INVALID_ARGUMENT");
        error.put("message", e.getMessage());
        error.put("timestamp", String.valueOf(System.currentTimeMillis()));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handle runtime exceptions (500)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException e, WebRequest request) {
        logger.error("❌ Runtime exception: {}", e.getMessage(), e);

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error", "INTERNAL_SERVER_ERROR");
        error.put("status", "error");
        error.put("code", "INTERNAL_SERVER_ERROR");
        error.put("message", e.getMessage() != null ? e.getMessage() : "Internal server error");
        error.put("timestamp", String.valueOf(System.currentTimeMillis()));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Handle all other exceptions (500)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception e, WebRequest request) {
        logger.error("❌ Unhandled exception: {}", e.getMessage(), e);

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error", "INTERNAL_SERVER_ERROR");
        error.put("status", "error");
        error.put("code", "INTERNAL_SERVER_ERROR");
        error.put("message", "An unexpected error occurred");
        error.put("timestamp", String.valueOf(System.currentTimeMillis()));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // ========== Helpers ==========

    @SuppressWarnings("unused")
    private List<String> getStackTrace(Throwable e) {
        List<String> trace = new ArrayList<>();
        for (StackTraceElement element : e.getStackTrace()) {
            trace.add(element.toString());
        }
        return trace;
    }
}
