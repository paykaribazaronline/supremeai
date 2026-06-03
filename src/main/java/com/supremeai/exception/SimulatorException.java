package com.supremeai.exception;

/**
 * Base exception for simulator-related errors
 */
public class SimulatorException extends RuntimeException {
    public SimulatorException(String message) { super(message); }
    public SimulatorException(String message, Throwable cause) { super(message, cause); }
}
