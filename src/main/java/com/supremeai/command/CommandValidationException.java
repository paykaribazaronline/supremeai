package com.supremeai.command;

/**
 * Custom exception for command validation errors
 */
public class CommandValidationException extends RuntimeException {
    public CommandValidationException(String message) {
        super(message);
    }
    
    public CommandValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
