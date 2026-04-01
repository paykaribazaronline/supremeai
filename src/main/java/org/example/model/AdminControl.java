package org.example.model;

/**
 * Admin Control Model
 * Controls system behavior: AUTO (full autonomy), WAIT (needs approval), FORCE_STOP
 */
public class AdminControl {
    
    public enum PermissionMode {
        AUTO,           // Auto commit and push
        WAIT,           // Wait for admin approval
        FORCE_STOP      // Stop all operations
    }
    
    private String id;
    private PermissionMode permissionMode;
    private boolean isRunning;
    private boolean canCommit;
    private long createdAt;
    private long lastUpdatedAt;
    private String updatedBy;      // Username of admin who made change
    private String description;    // Why this mode was set
    
    public AdminControl() {
        this.permissionMode = PermissionMode.WAIT; // Default: safe mode
        this.isRunning = true;
        this.canCommit = true;
        this.createdAt = System.currentTimeMillis();
        this.lastUpdatedAt = System.currentTimeMillis();
    }
    
    // Getters & Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public PermissionMode getPermissionMode() {
        return permissionMode;
    }
    
    public void setPermissionMode(PermissionMode permissionMode) {
        this.permissionMode = permissionMode;
        this.lastUpdatedAt = System.currentTimeMillis();
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    public void setRunning(boolean running) {
        isRunning = running;
        this.lastUpdatedAt = System.currentTimeMillis();
    }
    
    public boolean isCanCommit() {
        return canCommit && isRunning;
    }
    
    public void setCanCommit(boolean canCommit) {
        this.canCommit = canCommit;
        this.lastUpdatedAt = System.currentTimeMillis();
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public long getLastUpdatedAt() {
        return lastUpdatedAt;
    }
    
    public void setLastUpdatedAt(long lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }
    
    public String getUpdatedBy() {
        return updatedBy;
    }
    
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        return "AdminControl{" +
                "id='" + id + '\'' +
                ", permissionMode=" + permissionMode +
                ", isRunning=" + isRunning +
                ", canCommit=" + canCommit +
                ", updatedBy='" + updatedBy + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
