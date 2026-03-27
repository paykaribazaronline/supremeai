package org.example.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.*;

/**
 * Phase 5: Global REST Exception Handler
 * 
 * Handles all exceptions thrown from REST endpoints
 * Returns consistent error format to clients
 * 
 * Error Response Format:
 * {
 *   "status": "error",
 *   "code": "ERROR_CODE",
 *   "message": "Human readable message",
 *   "timestamp": 1234567890,
 *   "path": "/api/endpoint",
 *   "details": {...}
 * }
 */
@RestControllerAdvice
public class RestExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(RestExceptionHandler.class);
    
    /**
     * Handle resource not found (404)
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<?> handleNotFound(NoHandlerFoundException e, WebRequest request) {
        logger.debug("❌ Endpoint not found: {} {}", e.getHttpMethod(), e.getRequestURL());
        
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("status", "error");
        error.put("code", "NOT_FOUND");
        error.put("message", "Endpoint not found");
        error.put("path", e.getRequestURL());
        error.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    /**
     * Handle illegal argument exceptions (400)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e, WebRequest request) {
        logger.debug("⚠️ Invalid argument: {}", e.getMessage());
        
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("status", "error");
        error.put("code", "INVALID_ARGUMENT");
        error.put("message", e.getMessage());
        error.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    /**
     * Handle runtime exceptions (500)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException e, WebRequest request) {
        logger.error("❌ Runtime exception: {}", e.getMessage(), e);
        
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("status", "error");
        error.put("code", "INTERNAL_SERVER_ERROR");
        error.put("message", e.getMessage() != null ? e.getMessage() : "Internal server error");
        error.put("timestamp", System.currentTimeMillis());
        
        // Include stack trace in dev
        if (isDevelopmentMode()) {
            error.put("stackTrace", getStackTrace(e));
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    /**
     * Handle all other exceptions (500)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericException(Exception e, WebRequest request) {
        logger.error("❌ Unhandled exception: {}", e.getMessage(), e);
        
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("status", "error");
        error.put("code", "INTERNAL_SERVER_ERROR");
        error.put("message", "An unexpected error occurred");
        error.put("timestamp", System.currentTimeMillis());
        
        // Include stack trace in dev
        if (isDevelopmentMode()) {
            error.put("stackTrace", getStackTrace(e));
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    // ========== Helpers ==========
    
    private boolean isDevelopmentMode() {
        String env = System.getenv("SPRING_ENV");
        return env == null || "dev".equalsIgnoreCase(env) || "development".equalsIgnoreCase(env);
    }
    
    private List<String> getStackTrace(Throwable e) {
        List<String> trace = new ArrayList<>();
        for (StackTraceElement element : e.getStackTrace()) {
            trace.add(element.toString());
        }
        return trace;
    }
}
