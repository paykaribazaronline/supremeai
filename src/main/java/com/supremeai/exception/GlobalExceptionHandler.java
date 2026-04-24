package com.supremeai.exception;

import com.supremeai.model.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SimulatorQuotaExceededException.class)
    public ResponseEntity<ErrorResponse> handleQuotaExceeded(SimulatorQuotaExceededException e) {
        ErrorResponse error = ErrorResponse.builder()
                .error("QUOTA_EXCEEDED")
                .message(e.getMessage())
                .status(HttpStatus.CONFLICT.value())
                .timestamp(LocalDateTime.now())
                .details(Map.of("used", e.getUsed(), "total", e.getLimit()))
                .build();
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(SimulatorResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(SimulatorResourceNotFoundException e) {
        ErrorResponse error = ErrorResponse.builder()
                .error("NOT_FOUND")
                .message(e.getMessage())
                .status(HttpStatus.NOT_FOUND.value())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(SimulatorDeploymentException.class)
    public ResponseEntity<ErrorResponse> handleDeploymentError(SimulatorDeploymentException e) {
        log.error("Deployment error: {}", e.getMessage(), e);
        ErrorResponse error = ErrorResponse.builder()
                .error("DEPLOYMENT_FAILED")
                .message(e.getMessage())
                .status(HttpStatus.BAD_GATEWAY.value())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(SimulatorConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(SimulatorConflictException e) {
        ErrorResponse error = ErrorResponse.builder()
                .error("CONFLICT")
                .message(e.getMessage())
                .status(HttpStatus.CONFLICT.value())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        ErrorResponse error = ErrorResponse.builder()
                .error("ACCESS_DENIED")
                .message("You do not have permission to access this resource")
                .status(HttpStatus.FORBIDDEN.value())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        ErrorResponse error = ErrorResponse.builder()
                .error("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
