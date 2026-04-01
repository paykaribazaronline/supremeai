package org.example.model;

import java.util.List;

/**
 * Pending Action Model
 * Tracks actions waiting for admin approval
 */
public class PendingAction {
    
    public enum ActionType {
        CODE_GENERATION,
        COMMIT,
        PUSH,
        DEPLOYMENT,
        CONFIGURATION
    }
    
    public enum ActionStatus {
        PENDING,
        APPROVED,
        REJECTED,
        EXECUTED,
        FAILED
    }
    
    private String id;
    private ActionType actionType;
    private ActionStatus status;
    private String description;
    private String details;        // JSON details of the action
    private long createdAt;
    private long approvedAt;
    private String approvedBy;     // Username of admin who approved
    private String reason;         // Reason for approval/rejection
    private List<String> changes;  // What changed
    
    public PendingAction() {
        this.status = ActionStatus.PENDING;
        this.createdAt = System.currentTimeMillis();
    }
    
    public PendingAction(ActionType actionType, String description) {
        this();
        this.actionType = actionType;
        this.description = description;
    }
    
    // Getters & Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public ActionType getActionType() {
        return actionType;
    }
    
    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }
    
    public ActionStatus getStatus() {
        return status;
    }
    
    public void setStatus(ActionStatus status) {
        this.status = status;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public long getApprovedAt() {
        return approvedAt;
    }
    
    public void setApprovedAt(long approvedAt) {
        this.approvedAt = approvedAt;
    }
    
    public String getApprovedBy() {
        return approvedBy;
    }
    
    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public List<String> getChanges() {
        return changes;
    }
    
    public void setChanges(List<String> changes) {
        this.changes = changes;
    }
    
    @Override
    public String toString() {
        return "PendingAction{" +
                "id='" + id + '\'' +
                ", actionType=" + actionType +
                ", status=" + status +
                ", description='" + description + '\'' +
                ", approvedBy='" + approvedBy + '\'' +
                '}';
    }
}
