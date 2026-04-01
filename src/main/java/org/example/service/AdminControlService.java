package org.example.service;

import org.example.model.AdminControl;
import org.example.model.PendingAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Admin Control Service
 * Manages system permissions: AUTO, WAIT, FORCE_STOP
 * Tracks pending actions awaiting approval
 */
@Service
public class AdminControlService {
    private static final Logger logger = LoggerFactory.getLogger(AdminControlService.class);
    
    // In-memory storage (can be switched to Firebase)
    private final Map<String, AdminControl> adminControls = new ConcurrentHashMap<>();
    private final Map<String, PendingAction> pendingActions = new ConcurrentHashMap<>();
    
    /**
     * Get or create admin control (singleton pattern)
     */
    public AdminControl getAdminControl() {
        return adminControls.computeIfAbsent("default", k -> {
            AdminControl control = new AdminControl();
            control.setId("default");
            logger.info("✅ AdminControl initialized with WAIT mode (safe default)");
            return control;
        });
    }
    
    /**
     * Get current permission mode
     */
    public AdminControl.PermissionMode getPermissionMode() {
        return getAdminControl().getPermissionMode();
    }
    
    /**
     * Get current system status
     */
    public Map<String, Object> getStatus() {
        AdminControl control = getAdminControl();
        Map<String, Object> status = new HashMap<>();
        status.put("permissionMode", control.getPermissionMode());
        status.put("isRunning", control.isRunning());
        status.put("canCommit", control.isCanCommit());
        status.put("pendingActions", getPendingActionsCount());
        status.put("lastUpdatedAt", control.getLastUpdatedAt());
        status.put("lastUpdatedBy", control.getUpdatedBy());
        return status;
    }
    
    /**
     * Change permission mode (AUTO/WAIT/FORCE_STOP)
     */
    public AdminControl setPermissionMode(AdminControl.PermissionMode mode, String adminUsername, String description) {
        AdminControl control = getAdminControl();
        AdminControl.PermissionMode oldMode = control.getPermissionMode();
        
        control.setPermissionMode(mode);
        control.setUpdatedBy(adminUsername);
        control.setDescription(description);
        
        if (mode == AdminControl.PermissionMode.FORCE_STOP) {
            control.setRunning(false);
            logger.warn("🛑 FORCE_STOP activated by {}: {}", adminUsername, description);
        } else {
            control.setRunning(true);
            logger.info("✅ Permission mode changed from {} to {} by {}", oldMode, mode, adminUsername);
        }
        
        return control;
    }
    
    /**
     * Check if system can auto-commit
     */
    public boolean canAutoCommit() {
        AdminControl control = getAdminControl();
        return control.isCanCommit() && 
               control.getPermissionMode() == AdminControl.PermissionMode.AUTO &&
               control.isRunning();
    }
    
    /**
     * Check if action needs approval
     */
    public boolean requiresApproval() {
        AdminControl control = getAdminControl();
        return control.getPermissionMode() == AdminControl.PermissionMode.WAIT ||
               !control.isRunning();
    }
    
    /**
     * Create pending action (for WAIT mode)
     */
    public PendingAction createPendingAction(PendingAction.ActionType type, String description, String details) {
        PendingAction action = new PendingAction(type, description);
        action.setId(UUID.randomUUID().toString());
        action.setDetails(details);
        pendingActions.put(action.getId(), action);
        
        logger.info("📋 Pending action created: {} - {}", type, description);
        return action;
    }
    
    /**
     * Get pending actions count
     */
    public int getPendingActionsCount() {
        return (int) pendingActions.values().stream()
            .filter(a -> a.getStatus() == PendingAction.ActionStatus.PENDING)
            .count();
    }
    
    /**
     * Get all pending actions
     */
    public List<PendingAction> getPendingActions() {
        return pendingActions.values().stream()
            .filter(a -> a.getStatus() == PendingAction.ActionStatus.PENDING)
            .sorted(Comparator.comparingLong(PendingAction::getCreatedAt).reversed())
            .toList();
    }
    
    /**
     * Approve pending action
     */
    public PendingAction approvePendingAction(String actionId, String adminUsername, String reason) {
        PendingAction action = pendingActions.get(actionId);
        if (action == null) {
            logger.warn("❌ Pending action not found: {}", actionId);
            return null;
        }
        
        action.setStatus(PendingAction.ActionStatus.APPROVED);
        action.setApprovedAt(System.currentTimeMillis());
        action.setApprovedBy(adminUsername);
        action.setReason(reason);
        
        logger.info("✅ Action approved by {}: {}", adminUsername, actionId);
        return action;
    }
    
    /**
     * Reject pending action
     */
    public PendingAction rejectPendingAction(String actionId, String adminUsername, String reason) {
        PendingAction action = pendingActions.get(actionId);
        if (action == null) {
            logger.warn("❌ Pending action not found: {}", actionId);
            return null;
        }
        
        action.setStatus(PendingAction.ActionStatus.REJECTED);
        action.setApprovedBy(adminUsername);
        action.setReason(reason);
        
        logger.warn("❌ Action rejected by {}: {}", adminUsername, actionId);
        return action;
    }
    
    /**
     * Mark action as executed
     */
    public PendingAction markAsExecuted(String actionId, String result) {
        PendingAction action = pendingActions.get(actionId);
        if (action == null) {
            return null;
        }
        
        action.setStatus(PendingAction.ActionStatus.EXECUTED);
        action.setDetails(action.getDetails() + "\n\nRESULT: " + result);
        
        logger.info("✅ Action executed: {}", actionId);
        return action;
    }
    
    /**
     * Mark action as failed
     */
    public PendingAction markAsFailed(String actionId, String error) {
        PendingAction action = pendingActions.get(actionId);
        if (action == null) {
            return null;
        }
        
        action.setStatus(PendingAction.ActionStatus.FAILED);
        action.setDetails(action.getDetails() + "\n\nERROR: " + error);
        
        logger.error("❌ Action failed: {}", actionId);
        return action;
    }
    
    /**
     * Force stop all operations
     */
    public AdminControl forceStop(String adminUsername, String reason) {
        return setPermissionMode(AdminControl.PermissionMode.FORCE_STOP, adminUsername, reason);
    }
    
    /**
     * Resume operations
     */
    public AdminControl resume(String adminUsername, AdminControl.PermissionMode mode) {
        return setPermissionMode(mode, adminUsername, "Resumed by " + adminUsername);
    }
    
    /**
     * Get action history
     */
    public List<PendingAction> getActionHistory(int limit) {
        return pendingActions.values().stream()
            .filter(a -> a.getStatus() != PendingAction.ActionStatus.PENDING)
            .sorted(Comparator.comparingLong(PendingAction::getApprovedAt).reversed())
            .limit(limit)
            .toList();
    }
}
