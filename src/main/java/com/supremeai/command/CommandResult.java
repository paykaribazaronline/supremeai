package com.supremeai.command;

import java.time.LocalDateTime;

/**
 * Result of command execution
 * 
 * Contains:
 * - Status (success, error, pending)
 * - Result data
 * - Error details if failed
 * - Execution metadata
 */
public class CommandResult {
    public enum Status {
        SUCCESS,        // Command completed successfully
        PENDING,        // Async command queued
        RUNNING,        // Async command running
        FAILED,         // Command failed
        TIMEOUT,        // Command timed out
        CANCELLED       // Command cancelled
    }
    
    private String commandName;
    private Status status;
    private Object data;              // Result data from command
    private String errorMessage;      // If failed
    private String errorCode;         // If failed
    private long executionTimeMs;     // How long it took
    private LocalDateTime executedAt;
    private String executedBy;        // User who ran it
    private String jobId;             // For async commands
    
    // Constructor
    public CommandResult(String commandName) {
        this.commandName = commandName;
        this.status = Status.PENDING;
        this.executedAt = LocalDateTime.now();
        this.executionTimeMs = 0;
    }
    
    // Builders
    public static CommandResult success(String commandName, Object data) {
        CommandResult result = new CommandResult(commandName);
        result.status = Status.SUCCESS;
        result.data = data;
        return result;
    }
    
    public static CommandResult error(String commandName, String errorCode, String errorMessage) {
        CommandResult result = new CommandResult(commandName);
        result.status = Status.FAILED;
        result.errorCode = errorCode;
        result.errorMessage = errorMessage;
        return result;
    }
    
    public static CommandResult pending(String commandName, String jobId) {
        CommandResult result = new CommandResult(commandName);
        result.status = Status.PENDING;
        result.jobId = jobId;
        return result;
    }
    
    public static CommandResult running(String commandName, String jobId) {
        CommandResult result = new CommandResult(commandName);
        result.status = Status.RUNNING;
        result.jobId = jobId;
        return result;
    }
    
    // Getters & Setters
    public String getCommandName() { return commandName; }
    public Status getStatus() { return status; }
    public Object getData() { return data; }
    public String getErrorMessage() { return errorMessage; }
    public String getErrorCode() { return errorCode; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    public LocalDateTime getExecutedAt() { return executedAt; }
    public String getExecutedBy() { return executedBy; }
    public String getJobId() { return jobId; }
    
    public void setExecutionTime(long timeMs) { this.executionTimeMs = timeMs; }
    public void setExecutedBy(String user) { this.executedBy = user; }
    
    public boolean isSuccess() { return status == Status.SUCCESS; }
    public boolean isFailed() { return status == Status.FAILED; }
    public boolean isRunning() { return status == Status.RUNNING || status == Status.PENDING; }
    
    @Override
    public String toString() {
        return String.format("[%s] %s - Status: %s (%.0fms)", 
            commandName, executedBy, status, executionTimeMs);
    }
}
