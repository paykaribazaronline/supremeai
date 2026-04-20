package com.supremeai.exception;

/**
 * Thrown when session operation is invalid (e.g. stop when none running)
 */
public class SimulatorSessionException extends SimulatorException {
    public SimulatorSessionException(String message) { super(message); }
}
