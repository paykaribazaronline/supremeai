package com.supremeai.exception;

/**
 * Base exception for simulator-related errors
 */
public class SimulatorException extends RuntimeException {
    public SimulatorException(String message) { super(message); }
    public SimulatorException(String message, Throwable cause) { super(message, cause); }
}

/**
 * Thrown when user attempts to install more apps than their quota allows
 */
public class SimulatorQuotaExceededException extends SimulatorException {
    private final int used;
    private final int limit;

    public SimulatorQuotaExceededException(int used, int limit) {
        super(String.format("Quota exceeded: %d/%d apps installed", used, limit));
        this.used = used;
        this.limit = limit;
    }

    public SimulatorQuotaExceededException(String message) {
        super(message);
        this.used = -1;
        this.limit = -1;
    }

    public int getUsed() { return used; }
    public int getLimit() { return limit; }
}

/**
 * Thrown when app deployment to simulator fails
 */
public class SimulatorDeploymentException extends SimulatorException {
    public SimulatorDeploymentException(String message) { super(message); }
    public SimulatorDeploymentException(String message, Throwable cause) { super(message, cause); }
}

/**
 * Thrown when requested app is not found or not owned by user
 */
public class SimulatorResourceNotFoundException extends SimulatorException {
    public SimulatorResourceNotFoundException(String message) { super(message); }
}

/**
 * Thrown when operation conflicts with current state (e.g. app already installed)
 */
public class SimulatorConflictException extends SimulatorException {
    public SimulatorConflictException(String message) { super(message); }
}

/**
 * Thrown when session operation is invalid (e.g. stop when none running)
 */
public class SimulatorSessionException extends SimulatorException {
    public SimulatorSessionException(String message) { super(message); }
}
