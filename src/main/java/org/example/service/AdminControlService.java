package org.example.service;

import org.example.model.AdminControl;
import org.example.model.PendingAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Admin Control Service
 * Manages system permissions: AUTO, WAIT, FORCE_STOP
 * Tracks pending actions awaiting approval
 * Persists settings to Firebase for durability across restarts
 */
@Service
public class AdminControlService {
    private static final Logger logger = LoggerFactory.getLogger(AdminControlService.class);
    private static final String FIREBASE_ADMIN_CONTROL_PATH = "admin/control";
    private static final String FIREBASE_PENDING_ACTIONS_PATH = "admin/pending-actions";
    
    // In-memory cache
    private final Map<String, AdminControl> adminControls = new ConcurrentHashMap<>();
    private final Map<String, PendingAction> pendingActions = new ConcurrentHashMap<>();
    
    @Autowired(required = false)
    private FirebaseService firebaseService;
    
    /**
     * Initialize admin control from Firebase on startup
     */
    private void loadFromFirebase() {
        try {
            if (firebaseService == null || !firebaseService.isInitialized()) {
                logger.warn("⚠️ FirebaseService not available, using in-memory storage only");
                return;
            }
            
            // Try to load existing admin control from Firebase using Realtime Database
            Map<String, Object> data = firebaseService.getSystemConfig("admin/control");
            if (data != null && !data.isEmpty()) {
                AdminControl control = new AdminControl();
                control.setId("default");
                if (data.containsKey("permissionMode")) {
                    control.setPermissionMode(AdminControl.PermissionMode.valueOf((String) data.get("permissionMode")));
                }
                if (data.containsKey("isRunning")) {
                    control.setRunning((Boolean) data.getOrDefault("isRunning", true));
                }
                if (data.containsKey("canCommit")) {
                    control.setCanCommit((Boolean) data.getOrDefault("canCommit", false));
                }
                if (data.containsKey("description")) {
                    control.setDescription((String) data.get("description"));
                }
                if (data.containsKey("updatedBy")) {
                    control.setUpdatedBy((String) data.get("updatedBy"));
                }
                adminControls.put("default", control);
                logger.info("✅ AdminControl loaded from Firebase: {}", control.getPermissionMode());
            }
        } catch (Exception e) {
            logger.warn("⚠️ Failed to load admin control from Firebase, using defaults: {}", e.getMessage());
        }
    }

    /**
     * Get or create admin control (singleton pattern)
     */
    public AdminControl getAdminControl() {
        return adminControls.computeIfAbsent("default", k -> {
            // Try to load from Firebase first
            loadFromFirebase();
            
            // If not in cache yet, create new one
            if (!adminControls.containsKey("default")) {
                AdminControl control = new AdminControl();
                control.setId("default");
                logger.info("✅ AdminControl initialized with WAIT mode (safe default)");
                adminControls.put("default", control);
                saveToFirebase(control);
            }
            
            return adminControls.get("default");
        });
    }
    
    /**
     * Save admin control to Firebase with error verification
     */
    private void saveToFirebase(AdminControl control) {
        try {
            if (firebaseService == null || !firebaseService.isInitialized()) {
                return;
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("id", control.getId());
            data.put("permissionMode", control.getPermissionMode().toString());
            data.put("isRunning", control.isRunning());
            data.put("canCommit", control.isCanCommit());
            data.put("description", control.getDescription());
            data.put("updatedBy", control.getUpdatedBy());
            data.put("lastUpdatedAt", System.currentTimeMillis());
            
            // Call saveSystemConfig which now has callbacks built-in
            firebaseService.saveSystemConfig("admin/control", data);
            logger.info("✅ AdminControl persistence initiated for mode: {}", control.getPermissionMode());
        } catch (Exception e) {
            logger.error("❌ Failed to save AdminControl to Firebase: {}", e.getMessage(), e);
        }
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
        
        // Persist to Firebase
        saveToFirebase(control);
        
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
        
        // Save to Firebase
        savePendingActionToFirebase(action);
        
        logger.info("📋 Pending action created: {} - {}", type, description);
        return action;
    }
    
    /**
     * Save pending action to Firebase with error verification
     */
    private void savePendingActionToFirebase(PendingAction action) {
        try {
            if (firebaseService == null || !firebaseService.isInitialized()) {
                return;
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("id", action.getId());
            data.put("actionType", action.getActionType().toString());
            data.put("description", action.getDescription());
            data.put("status", action.getStatus().toString());
            data.put("details", action.getDetails());
            data.put("createdAt", action.getCreatedAt());
            data.put("approvedAt", action.getApprovedAt());
            data.put("approvedBy", action.getApprovedBy());
            data.put("reason", action.getReason());
            
            // Call saveSystemConfig which now has callbacks built-in
            firebaseService.saveSystemConfig("admin/pending-actions/" + action.getId(), data);
            logger.info("✅ Pending action persistence initiated: {}", action.getId());
        } catch (Exception e) {
            logger.error("❌ Failed to save pending action to Firebase: {}", e.getMessage(), e);
        }
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
        
        // Save to Firebase
        savePendingActionToFirebase(action);
        
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
        
        // Save to Firebase
        savePendingActionToFirebase(action);
        
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
        
        // Save to Firebase
        savePendingActionToFirebase(action);
        
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
        
        // Save to Firebase
        savePendingActionToFirebase(action);
        
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
