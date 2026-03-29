package org.example.model;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents a task assignment to an AI agent
 */
public class TaskAssignment {
    private String id;
    private String agentId;
    private String agentName;
    private String taskName;
    private String taskDescription;
    private String priority; // "low", "medium", "high", "critical"
    private double progress;
    private String status; // "pending", "in-progress", "completed", "failed"
    private LocalDateTime createdAt;
    private LocalDateTime deadline;
    private LocalDateTime completedAt;

    public TaskAssignment() {
        this.id = UUID.randomUUID().toString();
        this.progress = 0.0;
        this.status = "pending";
        this.createdAt = LocalDateTime.now();
    }

    public TaskAssignment(String agentId, String taskName) {
        this();
        this.agentId = agentId;
        this.taskName = taskName;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public String getTaskDescription() { return taskDescription; }
    public void setTaskDescription(String taskDescription) { this.taskDescription = taskDescription; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public double getProgress() { return progress; }
    public void setProgress(double progress) { this.progress = progress; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
