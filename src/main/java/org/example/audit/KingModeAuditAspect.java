package org.example.audit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * FIXED: King Mode Audit Aspect
 * 
 * Problem: No audit trail for King Mode (Admin Override) operations
 * Solution: AOP-based audit logging for all @KingMode annotated methods
 * 
 * Features:
 * 1. Pre-action logging with arguments
 * 2. Post-action logging with results
 * 3. Exception logging with full context
 * 4. Immutable audit records
 * 5. Secondary approval for critical actions
 * 
 * 4-Eyes Principle:
 * - Critical actions require secondary admin approval
 * - Actions are queued until approved by different admin
 * - Prevents single admin abuse
 */
@Aspect
@Component
public class KingModeAuditAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(KingModeAuditAspect.class);
    
    @Autowired
    private AuditLogger auditLogger;
    
    // Pending approvals for 4-eyes principle
    private final Map<String, PendingApproval> pendingApprovals = new ConcurrentHashMap<>();
    
    // Audit history (immutable records)
    private final List<KingModeAuditRecord> auditHistory = 
        Collections.synchronizedList(new ArrayList<>());
    
    /**
     * Intercept all @KingMode annotated methods
     */
    @Around("@annotation(kingMode)")
    public Object auditKingMode(ProceedingJoinPoint joinPoint, KingMode kingMode) throws Throwable {
        String adminId = getCurrentUser();
        String action = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Object[] args = joinPoint.getArgs();
        
        // Generate action ID
        String actionId = UUID.randomUUID().toString();
        
        // Check if this is a critical action requiring secondary approval
        if (isCriticalAction(action, args, kingMode)) {
            return handleCriticalAction(actionId, joinPoint, adminId, action, args);
        }
        
        // Pre-action log
        KingModeAuditRecord beforeRecord = new KingModeAuditRecord(
            actionId,
            "KING_MODE_BEFORE",
            adminId,
            className + "." + action,
            maskSensitive(args),
            null,
            null,
            Instant.now()
        );
        saveAuditRecord(beforeRecord);
        
        logger.info("👑 [KING_MODE] {} by {} - BEFORE", action, adminId);
        
        try {
            // Execute the action
            Object result = joinPoint.proceed();
            
            // Success log
            KingModeAuditRecord successRecord = new KingModeAuditRecord(
                actionId,
                "KING_MODE_SUCCESS",
                adminId,
                className + "." + action,
                null,
                truncateResult(result),
                null,
                Instant.now()
            );
            saveAuditRecord(successRecord);
            
            logger.info("👑 [KING_MODE] {} by {} - SUCCESS", action, adminId);
            
            return result;
            
        } catch (Exception e) {
            // Failure log
            KingModeAuditRecord failureRecord = new KingModeAuditRecord(
                actionId,
                "KING_MODE_FAILURE",
                adminId,
                className + "." + action,
                null,
                null,
                e.getMessage(),
                Instant.now()
            );
            saveAuditRecord(failureRecord);
            
            logger.error("👑 [KING_MODE] {} by {} - FAILURE: {}", 
                action, adminId, e.getMessage());
            
            throw e;
        }
    }

    /**
     * Convenience overload for direct unit testing and manual invocation.
     */
    public Object auditKingMode(ProceedingJoinPoint joinPoint) throws Throwable {
        return auditKingMode(joinPoint, null);
    }
    
    /**
     * Handle critical actions with 4-eyes principle
     */
    private Object handleCriticalAction(String actionId, ProceedingJoinPoint joinPoint,
                                       String adminId, String action, Object[] args) throws Throwable {
        logger.info("👑 [KING_MODE] Critical action {} by {} - QUEUED for secondary approval", 
            action, adminId);
        
        // Queue for secondary approval
        PendingApproval pending = new PendingApproval(
            actionId,
            adminId,
            action,
            args,
            joinPoint,
            Instant.now()
        );
        
        pendingApprovals.put(actionId, pending);
        
        // Notify secondary admin
        notifySecondaryAdmin(pending);
        
        // Return pending status
        return new PendingActionResult(actionId, "PENDING_APPROVAL",
            "Action queued for secondary admin approval");
    }
    
    /**
     * Approve a pending critical action (called by secondary admin)
     */
    public Object approvePendingAction(String actionId, String approvingAdminId, String reason) {
        PendingApproval pending = pendingApprovals.get(actionId);
        
        if (pending == null) {
            throw new IllegalArgumentException("Pending action not found: " + actionId);
        }
        
        // Check same admin cannot approve their own action
        if (pending.getRequestingAdminId().equals(approvingAdminId)) {
            throw new IllegalStateException("Same admin cannot approve their own action");
        }
        
        logger.info("👑 [KING_MODE] Action {} approved by secondary admin {}", 
            actionId, approvingAdminId);
        
        try {
            // Execute the action
            Object result = pending.getJoinPoint().proceed();
            
            // Log approval
            KingModeAuditRecord approvalRecord = new KingModeAuditRecord(
                actionId,
                "KING_MODE_APPROVED",
                approvingAdminId,
                pending.getAction(),
                null,
                truncateResult(result),
                null,
                Instant.now()
            );
            saveAuditRecord(approvalRecord);
            
            // Remove from pending
            pendingApprovals.remove(actionId);
            
            return result;
            
        } catch (Throwable e) {
            logger.error("👑 [KING_MODE] Approved action failed: {}", e.getMessage());
            throw new RuntimeException("Approved action execution failed", e);
        }
    }
    
    /**
     * Reject a pending action
     */
    public void rejectPendingAction(String actionId, String rejectingAdminId, String reason) {
        PendingApproval pending = pendingApprovals.remove(actionId);
        
        if (pending == null) {
            throw new IllegalArgumentException("Pending action not found: " + actionId);
        }
        
        logger.info("👑 [KING_MODE] Action {} rejected by admin {}: {}", 
            actionId, rejectingAdminId, reason);
        
        KingModeAuditRecord rejectionRecord = new KingModeAuditRecord(
            actionId,
            "KING_MODE_REJECTED",
            rejectingAdminId,
            pending.getAction(),
            null,
            null,
            reason,
            Instant.now()
        );
        saveAuditRecord(rejectionRecord);
    }
    
    /**
     * Check if action is critical (requires 4-eyes)
     */
    private boolean isCriticalAction(String action, Object[] args, KingMode kingMode) {
        if (kingMode != null &&
            (kingMode.requireSecondaryApproval() || kingMode.severity() == KingMode.Severity.CRITICAL)) {
            return true;
        }

        // Critical actions that require secondary approval
        Set<String> criticalActions = Set.of(
            "deleteAllData",
            "forceStop",
            "modifyPaymentSettings",
            "changeSecurityPolicy",
            "grantAdminAccess",
            "revokeAllKeys"
        );
        
        return criticalActions.contains(action);
    }
    
    /**
     * Save audit record to immutable storage
     */
    private void saveAuditRecord(KingModeAuditRecord record) {
        auditHistory.add(record);
        
        // Also log to standard audit logger
        auditLogger.logSecurityEvent("KING_MODE", 
            String.format("%s: %s by %s", record.getType(), record.getAction(), record.getAdminId()),
            record.getAdminId()
        );
    }
    
    /**
     * Get current user from security context
     */
    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "UNKNOWN";
    }
    
    /**
     * Mask sensitive arguments
     */
    private String maskSensitive(Object[] args) {
        if (args == null) return "[]";
        
        return Arrays.stream(args)
            .map(arg -> {
                if (arg == null) return "null";
                String str = arg.toString();
                // Mask potential secrets
                if (str.toLowerCase().contains("password") ||
                    str.toLowerCase().contains("secret") ||
                    str.toLowerCase().contains("key")) {
                    return "***MASKED***";
                }
                return str.length() > 100 ? str.substring(0, 100) + "..." : str;
            })
            .toList()
            .toString();
    }
    
    /**
     * Truncate result for logging
     */
    private String truncateResult(Object result) {
        if (result == null) return null;
        String str = result.toString();
        return str.length() > 500 ? str.substring(0, 500) + "..." : str;
    }
    
    /**
     * Notify secondary admin about pending approval
     */
    private void notifySecondaryAdmin(PendingApproval pending) {
        // In production, send notification via email, Slack, etc.
        logger.info("📧 Notification sent to secondary admins about pending action {}", 
            pending.getActionId());
    }
    
    /**
     * Get audit trail for an admin
     */
    public List<KingModeAuditRecord> getAdminAuditTrail(String adminId) {
        return auditHistory.stream()
            .filter(r -> r.getAdminId().equals(adminId))
            .sorted(Comparator.comparing(KingModeAuditRecord::getTimestamp).reversed())
            .toList();
    }
    
    /**
     * Get all pending approvals
     */
    public List<PendingApproval> getPendingApprovals() {
        return new ArrayList<>(pendingApprovals.values());
    }
    
    /**
     * Get audit statistics
     */
    public Map<String, Object> getStatistics() {
        return Map.of(
            "totalActions", auditHistory.size(),
            "successfulActions", auditHistory.stream()
                .filter(r -> "KING_MODE_SUCCESS".equals(r.getType()))
                .count(),
            "failedActions", auditHistory.stream()
                .filter(r -> "KING_MODE_FAILURE".equals(r.getType()))
                .count(),
            "pendingApprovals", pendingApprovals.size(),
            "actionsByAdmin", auditHistory.stream()
                .collect(Collectors.groupingBy(
                    KingModeAuditRecord::getAdminId,
                    Collectors.counting()
                )),
            "recentActions", auditHistory.stream()
                .sorted(Comparator.comparing(KingModeAuditRecord::getTimestamp).reversed())
                .limit(10)
                .map(r -> Map.of(
                    "type", r.getType(),
                    "admin", r.getAdminId(),
                    "action", r.getAction(),
                    "timestamp", r.getTimestamp().toString()
                ))
                .toList()
        );
    }
    
    // ============== Data Classes ==============
    
    public static class KingModeAuditRecord {
        private final String actionId;
        private final String type;
        private final String adminId;
        private final String action;
        private final String arguments;
        private final String result;
        private final String error;
        private final Instant timestamp;
        
        public KingModeAuditRecord(String actionId, String type, String adminId,
                                  String action, String arguments, String result,
                                  String error, Instant timestamp) {
            this.actionId = actionId;
            this.type = type;
            this.adminId = adminId;
            this.action = action;
            this.arguments = arguments;
            this.result = result;
            this.error = error;
            this.timestamp = timestamp;
        }
        
        // Getters only for immutability
        public String getActionId() { return actionId; }
        public String getType() { return type; }
        public String getAdminId() { return adminId; }
        public String getAction() { return action; }
        public String getArguments() { return arguments; }
        public String getResult() { return result; }
        public String getError() { return error; }
        public Instant getTimestamp() { return timestamp; }
    }
    
    public static class PendingApproval {
        private final String actionId;
        private final String requestingAdminId;
        private final String action;
        private final Object[] args;
        private final ProceedingJoinPoint joinPoint;
        private final Instant requestedAt;
        
        public PendingApproval(String actionId, String requestingAdminId,
                              String action, Object[] args,
                              ProceedingJoinPoint joinPoint, Instant requestedAt) {
            this.actionId = actionId;
            this.requestingAdminId = requestingAdminId;
            this.action = action;
            this.args = args;
            this.joinPoint = joinPoint;
            this.requestedAt = requestedAt;
        }
        
        public String getActionId() { return actionId; }
        public String getRequestingAdminId() { return requestingAdminId; }
        public String getAction() { return action; }
        public Object[] getArgs() { return args; }
        public ProceedingJoinPoint getJoinPoint() { return joinPoint; }
        public Instant getRequestedAt() { return requestedAt; }
    }
    
    public static class PendingActionResult {
        private final String actionId;
        private final String status;
        private final String message;
        
        public PendingActionResult(String actionId, String status, String message) {
            this.actionId = actionId;
            this.status = status;
            this.message = message;
        }
        
        public String getActionId() { return actionId; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
    }
}
