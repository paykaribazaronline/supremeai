package org.example.model;

import java.time.LocalDateTime;
import java.util.*;

/**
 * System Configuration and Mode Settings
 * Stores current operating mode and related configurations
 */
public class SystemConfiguration {
    private String id;
    private SystemMode currentMode;
    private LocalDateTime modeChangedAt;
    private String changedByAdmin;
    
    // FULLY_AUTOMATIC mode settings
    private boolean autoLearnEnabled = true;
    private boolean autoGenerateAPIs = true;
    private boolean autoImproveCode = true;
    private int autonomyLevel = 80; // 0-100: how aggressive is autonomous decision-making
    
    // PRESET_RULES mode settings
    private List<String> allowedOperations = new ArrayList<>(); // What system can do
    private List<String> blockedOperations = new ArrayList<>();  // What system cannot do
    private int maxAutoActionsPerDay = -1;  // -1 = unlimited (MASTER RULE: no hardcoded caps)
    private int maxAutoActionsPerHour = -1;  // -1 = unlimited (MASTER RULE: no hardcoded caps)
    private int dailyActionsUsed = 0;
    private int hourlyActionsUsed = 0;
    
    // MANUAL_ONLY mode settings
    private boolean requireApprovalForAllChanges = true;
    private List<String> pendingApprovals = new ArrayList<>();
    
    // General settings
    private boolean loggingEnabled = true;
    private boolean auditTrailEnabled = true;
    private String lastUpdatedBy;
    private LocalDateTime lastUpdatedAt;
    private int confidenceThreshold = 85; // 0-100: minimum confidence for auto-execution

    public SystemConfiguration() {
        this.id = UUID.randomUUID().toString();
        this.currentMode = SystemMode.PRESET_RULES; // Safe default
        this.modeChangedAt = LocalDateTime.now();
        this.lastUpdatedAt = LocalDateTime.now();
        initializeDefaultAllowedOperations();
    }

    private void initializeDefaultAllowedOperations() {
        allowedOperations.addAll(Arrays.asList(
            "LEARN_FROM_ERRORS",
            "UPDATE_TECHNICAL_KNOWLEDGE",
            "GENERATE_API",
            "OPTIMIZE_CODE",
            "FIX_BUGS",
            "GENERATE_DOCUMENTATION",
            "ANALYZE_PERFORMANCE"
        ));
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public SystemMode getCurrentMode() { return currentMode; }
    public void setCurrentMode(SystemMode currentMode) { 
        this.currentMode = currentMode;
        this.modeChangedAt = LocalDateTime.now();
    }

    public LocalDateTime getModeChangedAt() { return modeChangedAt; }
    
    public String getChangedByAdmin() { return changedByAdmin; }
    public void setChangedByAdmin(String changedByAdmin) { this.changedByAdmin = changedByAdmin; }

    public boolean isAutoLearnEnabled() { return autoLearnEnabled; }
    public void setAutoLearnEnabled(boolean autoLearnEnabled) { this.autoLearnEnabled = autoLearnEnabled; }

    public boolean isAutoGenerateAPIs() { return autoGenerateAPIs; }
    public void setAutoGenerateAPIs(boolean autoGenerateAPIs) { this.autoGenerateAPIs = autoGenerateAPIs; }

    public boolean isAutoImproveCode() { return autoImproveCode; }
    public void setAutoImproveCode(boolean autoImproveCode) { this.autoImproveCode = autoImproveCode; }

    public int getAutonomyLevel() { return autonomyLevel; }
    public void setAutonomyLevel(int autonomyLevel) { 
        this.autonomyLevel = Math.max(0, Math.min(100, autonomyLevel));
    }

    public List<String> getAllowedOperations() { return allowedOperations; }
    public void setAllowedOperations(List<String> allowedOperations) { this.allowedOperations = allowedOperations; }

    public List<String> getBlockedOperations() { return blockedOperations; }
    public void setBlockedOperations(List<String> blockedOperations) { this.blockedOperations = blockedOperations; }

    public int getMaxAutoActionsPerDay() { return maxAutoActionsPerDay; }
    public void setMaxAutoActionsPerDay(int maxAutoActionsPerDay) { this.maxAutoActionsPerDay = maxAutoActionsPerDay; }

    public int getMaxAutoActionsPerHour() { return maxAutoActionsPerHour; }
    public void setMaxAutoActionsPerHour(int maxAutoActionsPerHour) { this.maxAutoActionsPerHour = maxAutoActionsPerHour; }

    public int getDailyActionsUsed() { return dailyActionsUsed; }
    public void setDailyActionsUsed(int dailyActionsUsed) { this.dailyActionsUsed = dailyActionsUsed; }

    public int getHourlyActionsUsed() { return hourlyActionsUsed; }
    public void setHourlyActionsUsed(int hourlyActionsUsed) { this.hourlyActionsUsed = hourlyActionsUsed; }

    public boolean isRequireApprovalForAllChanges() { return requireApprovalForAllChanges; }
    public void setRequireApprovalForAllChanges(boolean requireApprovalForAllChanges) { 
        this.requireApprovalForAllChanges = requireApprovalForAllChanges;
    }

    public List<String> getPendingApprovals() { return pendingApprovals; }
    public void setPendingApprovals(List<String> pendingApprovals) { this.pendingApprovals = pendingApprovals; }

    public boolean isLoggingEnabled() { return loggingEnabled; }
    public void setLoggingEnabled(boolean loggingEnabled) { this.loggingEnabled = loggingEnabled; }

    public boolean isAuditTrailEnabled() { return auditTrailEnabled; }
    public void setAuditTrailEnabled(boolean auditTrailEnabled) { this.auditTrailEnabled = auditTrailEnabled; }

    public String getLastUpdatedBy() { return lastUpdatedBy; }
    public void setLastUpdatedBy(String lastUpdatedBy) { this.lastUpdatedBy = lastUpdatedBy; }

    public LocalDateTime getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }

    public int getConfidenceThreshold() { return confidenceThreshold; }
    public void setConfidenceThreshold(int confidenceThreshold) { 
        this.confidenceThreshold = Math.max(0, Math.min(100, confidenceThreshold));
    }

    /**
     * Check if operation is allowed in current mode
     */
    public boolean isOperationAllowed(String operation) {
        if (blockedOperations.contains(operation)) {
            return false;
        }
        if (currentMode == SystemMode.MANUAL_ONLY) {
            return false; // Nothing allowed in manual mode without explicit request
        }
        if (currentMode == SystemMode.PRESET_RULES) {
            return allowedOperations.contains(operation);
        }
        return true; // FULLY_AUTOMATIC allows all
    }

    /**
     * Check if daily action limit exceeded
     * Returns false if maxAutoActionsPerDay is -1 (unlimited)
     */
    public boolean isDailyLimitExceeded() {
        if (maxAutoActionsPerDay == -1) return false;  // Unlimited
        return dailyActionsUsed >= maxAutoActionsPerDay;
    }

    /**
     * Check if hourly limit exceeded
     * Returns false if maxAutoActionsPerHour is -1 (unlimited)
     */
    public boolean isHourlyLimitExceeded() {
        if (maxAutoActionsPerHour == -1) return false;  // Unlimited
        return hourlyActionsUsed >= maxAutoActionsPerHour;
    }

    /**
     * Get mode status summary
     */
    public Map<String, Object> getModeStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("currentMode", currentMode.name());
        status.put("displayName", currentMode.getDisplayName());
        status.put("icon", currentMode.getIcon());
        status.put("modeChangedAt", modeChangedAt.toString());
        status.put("isAutonomous", currentMode.isAutonomous());
        status.put("isManualOnly", currentMode.isManualOnly());
        
        if (currentMode == SystemMode.FULLY_AUTOMATIC) {
            status.put("autoLearnEnabled", autoLearnEnabled);
            status.put("autoGenerateAPIs", autoGenerateAPIs);
            status.put("autoImproveCode", autoImproveCode);
            status.put("autonomyLevel", autonomyLevel + "%");
        } else if (currentMode == SystemMode.PRESET_RULES) {
            status.put("allowedOperations", allowedOperations.size());
            status.put("blockedOperations", blockedOperations.size());
            status.put("dailyActionsUsed", dailyActionsUsed + " / " + maxAutoActionsPerDay);
            status.put("hourlyActionsUsed", hourlyActionsUsed + " / " + maxAutoActionsPerHour);
        } else {
            status.put("pendingApprovals", pendingApprovals.size());
        }
        
        return status;
    }
}
