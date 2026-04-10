package org.example.service;

import org.example.model.SystemMode;
import org.example.model.SystemConfiguration;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

/**
 * System Mode Service
 * Controls SupremeAI's operating mode and enforces mode-specific rules
 */
@Service
public class SystemModeService {
    
    private SystemConfiguration config = new SystemConfiguration();
    private LocalDateTime hourlyResetTime = LocalDateTime.now();
    private LocalDateTime dailyResetTime = LocalDateTime.now();

    /**
     * Get current system mode
     */
    public SystemMode getCurrentMode() {
        return config.getCurrentMode();
    }

    /**
     * Get current system configuration
     */
    public SystemConfiguration getConfiguration() {
        return config;
    }

    /**
     * Change system mode (admin only)
     */
    public void setSystemMode(SystemMode newMode, String adminName) {
        SystemMode oldMode = config.getCurrentMode();
        config.setCurrentMode(newMode);
        config.setChangedByAdmin(adminName);
        config.setLastUpdatedBy(adminName);
        config.setLastUpdatedAt(LocalDateTime.now());
        
        System.out.println("🔄 System mode changed: " + oldMode.name() + " → " + newMode.name() + 
                          " (by " + adminName + ")");
        logModeChange(oldMode, newMode, adminName);
    }

    /**
     * Check if an operation can be executed in current mode
     */
    public OperationDecision canExecuteOperation(String operationName, int confidenceScore) {
        resetLimitsIfNeeded();
        
        SystemMode mode = config.getCurrentMode();
        OperationDecision decision = new OperationDecision();
        decision.setOperationName(operationName);
        decision.setMode(mode);
        decision.setConfidence(confidenceScore);
        
        // Check confidence threshold
        if (confidenceScore < config.getConfidenceThreshold()) {
            decision.setAllowed(false);
            decision.setReason("Confidence " + confidenceScore + " below threshold " + config.getConfidenceThreshold());
            return decision;
        }

        switch (mode) {
            case FULLY_AUTOMATIC:
                // Everything allowed with high confidence
                decision.setAllowed(true);
                decision.setReason("FULLY_AUTOMATIC mode: autonomous execution enabled");
                break;

            case PRESET_RULES:
                // Check against allowed operations and limits
                if (!config.isOperationAllowed(operationName)) {
                    decision.setAllowed(false);
                    decision.setReason("Operation '" + operationName + "' not in allowed operations list");
                } else if (config.isDailyLimitExceeded()) {
                    decision.setAllowed(false);
                    decision.setReason("Daily action limit exceeded: " + config.getDailyActionsUsed() + 
                                      " / " + config.getMaxAutoActionsPerDay());
                } else if (config.isHourlyLimitExceeded()) {
                    decision.setAllowed(false);
                    decision.setReason("Hourly action limit exceeded: " + config.getHourlyActionsUsed() + 
                                      " / " + config.getMaxAutoActionsPerHour());
                } else {
                    decision.setAllowed(true);
                    decision.setReason("PRESET_RULES mode: operation matches allowed list");
                    incrementActionCounters();
                }
                break;

            case MANUAL_ONLY:
                // Never allowed without explicit admin command
                decision.setAllowed(false);
                decision.setReason("MANUAL_ONLY mode: waiting for explicit admin command");
                decision.setRequiresApproval(true);
                break;
        }

        return decision;
    }

    /**
     * Request approval for an operation in MANUAL_ONLY mode
     */
    public void requestApproval(String operationId, String operationDescription) {
        if (config.getCurrentMode() == SystemMode.MANUAL_ONLY) {
            config.getPendingApprovals().add(operationId + ": " + operationDescription);
            System.out.println("⏳ Approval requested: " + operationDescription + 
                              " (mode: MANUAL_ONLY)");
        }
    }

    /**
     * Approve a manual operation
     */
    public void approveOperation(String operationId, String adminName) {
        String pendingEntry = findPendingEntry(operationId);
        if (pendingEntry != null) {
            config.getPendingApprovals().remove(pendingEntry);
        }
        config.setLastUpdatedBy(adminName);
        config.setLastUpdatedAt(LocalDateTime.now());
        System.out.println("✅ Operation approved by " + adminName + ": " + operationId);
    }

    /**
     * Reject a manual operation
     */
    public void rejectOperation(String operationId, String adminName) {
        String pendingEntry = findPendingEntry(operationId);
        if (pendingEntry != null) {
            config.getPendingApprovals().remove(pendingEntry);
        }
        config.setLastUpdatedBy(adminName);
        config.setLastUpdatedAt(LocalDateTime.now());
        System.out.println("❌ Operation rejected by " + adminName + ": " + operationId);
    }

    /**
     * Get all pending approvals
     */
    public List<String> getPendingApprovals() {
        return new ArrayList<>(config.getPendingApprovals());
    }

    /**
     * Configure allowed operations for PRESET_RULES mode
     */
    public void setAllowedOperations(List<String> operations, String adminName) {
        config.setAllowedOperations(operations);
        config.setLastUpdatedBy(adminName);
        config.setLastUpdatedAt(LocalDateTime.now());
        System.out.println("📝 Allowed operations updated: " + operations.size() + " operations");
    }

    /**
     * Configure blocked operations list for PRESET_RULES mode
     */
    public void setBlockedOperations(List<String> operations, String adminName) {
        config.setBlockedOperations(operations);
        config.setLastUpdatedBy(adminName);
        config.setLastUpdatedAt(LocalDateTime.now());
        System.out.println("🚫 Blocked operations updated: " + operations.size() + " operations");
    }

    /**
     * Configure daily/hourly action limits for PRESET_RULES mode
     */
    public void setActionLimits(int maxPerDay, int maxPerHour, String adminName) {
        config.setMaxAutoActionsPerDay(Math.max(1, maxPerDay));
        config.setMaxAutoActionsPerHour(Math.max(1, maxPerHour));
        config.setLastUpdatedBy(adminName);
        config.setLastUpdatedAt(LocalDateTime.now());
        System.out.println("📊 Action limits updated: " + maxPerDay + "/day, " + maxPerHour + "/hour");
    }

    /**
     * Add operation to allowed list
     */
    public void allowOperation(String operation, String adminName) {
        if (!config.getAllowedOperations().contains(operation)) {
            config.getAllowedOperations().add(operation);
            config.setLastUpdatedBy(adminName);
            config.setLastUpdatedAt(LocalDateTime.now());
            System.out.println("✅ Operation allowed: " + operation);
        }
    }

    /**
     * Remove operation from allowed list
     */
    public void blockOperation(String operation, String adminName) {
        config.getAllowedOperations().remove(operation);
        config.getBlockedOperations().add(operation);
        config.setLastUpdatedBy(adminName);
        config.setLastUpdatedAt(LocalDateTime.now());
        System.out.println("🚫 Operation blocked: " + operation);
    }

    /**
     * Set autonomy level for FULLY_AUTOMATIC mode (0-100)
     */
    public void setAutonomyLevel(int level, String adminName) {
        config.setAutonomyLevel(level);
        config.setLastUpdatedBy(adminName);
        config.setLastUpdatedAt(LocalDateTime.now());
        System.out.println("⚡ Autonomy level set to: " + level + "%");
    }

    /**
     * Update FULLY_AUTOMATIC feature toggles and autonomy level
     */
    public void setAutomaticSettings(boolean autoLearn, boolean autoGenerateApis,
                                     boolean autoImproveCode, int autonomyLevel,
                                     String adminName) {
        config.setAutoLearnEnabled(autoLearn);
        config.setAutoGenerateAPIs(autoGenerateApis);
        config.setAutoImproveCode(autoImproveCode);
        config.setAutonomyLevel(autonomyLevel);
        config.setLastUpdatedBy(adminName);
        config.setLastUpdatedAt(LocalDateTime.now());
        System.out.println("⚙️ FULLY_AUTOMATIC settings updated by " + adminName);
    }

    /**
     * Get mode status for admin dashboard
     */
    public Map<String, Object> getModeStatus() {
        return config.getModeStatus();
    }

    /**
     * Get mode change history (would be stored in database)
     */
    public String getModeChangeHistory() {
        return "Mode: " + config.getCurrentMode().name() + 
               "\nChanged by: " + config.getChangedByAdmin() + 
               "\nChanged at: " + config.getModeChangedAt();
    }

    // Private helper methods

    private void resetLimitsIfNeeded() {
        LocalDateTime now = LocalDateTime.now();
        
        // Reset hourly
        if (now.isAfter(hourlyResetTime.plusHours(1))) {
            config.setHourlyActionsUsed(0);
            hourlyResetTime = now;
        }
        
        // Reset daily
        if (now.isAfter(dailyResetTime.plusDays(1))) {
            config.setDailyActionsUsed(0);
            dailyResetTime = now;
        }
    }

    private void incrementActionCounters() {
        config.setDailyActionsUsed(config.getDailyActionsUsed() + 1);
        config.setHourlyActionsUsed(config.getHourlyActionsUsed() + 1);
    }

    private void logModeChange(SystemMode oldMode, SystemMode newMode, String adminName) {
        if (config.isAuditTrailEnabled()) {
            System.out.println("[AUDIT] Mode change: " + oldMode.name() + " -> " + newMode.name() + 
                              " by " + adminName);
        }
    }

    private String findPendingEntry(String operationId) {
        if (operationId == null || operationId.isBlank()) {
            return null;
        }
        for (String pending : config.getPendingApprovals()) {
            if (pending.equals(operationId) || pending.startsWith(operationId + ":")) {
                return pending;
            }
        }
        return null;
    }

    /**
     * Inner class for operation decision
     */
    public static class OperationDecision {
        private String operationName;
        private SystemMode mode;
        private boolean allowed;
        private String reason;
        private int confidence;
        private boolean requiresApproval;

        // Getters and Setters
        public String getOperationName() { return operationName; }
        public void setOperationName(String operationName) { this.operationName = operationName; }

        public SystemMode getMode() { return mode; }
        public void setMode(SystemMode mode) { this.mode = mode; }

        public boolean isAllowed() { return allowed; }
        public void setAllowed(boolean allowed) { this.allowed = allowed; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }

        public int getConfidence() { return confidence; }
        public void setConfidence(int confidence) { this.confidence = confidence; }

        public boolean isRequiresApproval() { return requiresApproval; }
        public void setRequiresApproval(boolean requiresApproval) { this.requiresApproval = requiresApproval; }
    }
}
