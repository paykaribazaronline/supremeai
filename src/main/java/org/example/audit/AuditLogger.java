package org.example.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import java.time.Instant;
import java.util.*;

/**
 * Audit logging for compliance and accountability
 * Tracks all significant actions: approvals, rejections, modifications
 */
public class AuditLogger {
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");
    private static final Logger logger = LoggerFactory.getLogger(AuditLogger.class);
    private final boolean enabled;
    
    public AuditLogger(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            logger.info("Audit logging enabled");
        }
    }
    
    /**
     * Log requirement classification
     */
    public void logRequirementClassified(String requirementId, String description, 
                                        String size, String userId) {
        if (!enabled) return;
        
        auditLog("REQUIREMENT_CLASSIFIED", Map.ofEntries(
            Map.entry("requirement_id", requirementId),
            Map.entry("description", truncate(description, 500)),
            Map.entry("size", size),
            Map.entry("user_id", userId)
        ));
    }
    
    /**
     * Log approval action
     */
    public void logApprovalAction(String requirementId, boolean approved, String userId, 
                                 String notes, String ipAddress) {
        if (!enabled) return;
        
        auditLog(approved ? "APPROVAL_GRANTED" : "APPROVAL_DENIED", Map.ofEntries(
            Map.entry("requirement_id", requirementId),
            Map.entry("user_id", userId),
            Map.entry("notes", truncate(notes, 500)),
            Map.entry("ip_address", maskIP(ipAddress))
        ));
    }
    
    /**
     * Log agent assignment
     */
    public void logAgentAssignment(String taskId, String taskType, String assignedAgent, 
                                  String userId) {
        if (!enabled) return;
        
        auditLog("AGENT_ASSIGNED", Map.ofEntries(
            Map.entry("task_id", taskId),
            Map.entry("task_type", taskType),
            Map.entry("assigned_agent", assignedAgent),
            Map.entry("user_id", userId)
        ));
    }
    
    /**
     * Log API call
     */
    public void logAPICall(String apiName, String method, String model, int statusCode,
                          long durationMs) {
        if (!enabled) return;
        
        auditLog("API_CALL", Map.ofEntries(
            Map.entry("api_name", apiName),
            Map.entry("method", method),
            Map.entry("model", model),
            Map.entry("status_code", String.valueOf(statusCode)),
            Map.entry("duration_ms", String.valueOf(durationMs))
        ));
    }
    
    /**
     * Log configuration change
     */
    public void logConfigChange(String configKey, String oldValue, String newValue, 
                               String userId) {
        if (!enabled) return;
        
        auditLog("CONFIG_CHANGED", Map.ofEntries(
            Map.entry("config_key", configKey),
            Map.entry("old_value", maskSensitive(configKey, oldValue)),
            Map.entry("new_value", maskSensitive(configKey, newValue)),
            Map.entry("user_id", userId)
        ));
    }
    
    /**
     * Log authentication attempt
     */
    public void logAuthenticationAttempt(String userId, boolean success, String ipAddress) {
        if (!enabled) return;
        
        auditLog(success ? "AUTH_SUCCESS" : "AUTH_FAILURE", Map.ofEntries(
            Map.entry("user_id", userId),
            Map.entry("ip_address", maskIP(ipAddress))
        ));
    }
    
    /**
     * Log access to sensitive resource
     */
    public void logAccessToResource(String resourceId, String resourceType, String userId) {
        if (!enabled) return;
        
        auditLog("RESOURCE_ACCESS", Map.ofEntries(
            Map.entry("resource_id", resourceId),
            Map.entry("resource_type", resourceType),
            Map.entry("user_id", userId)
        ));
    }
    
    /**
     * Log rate limit exceeded
     */
    public void logRateLimitExceeded(String userId, String endpoint, String ipAddress) {
        if (!enabled) return;
        
        auditLog("RATE_LIMIT_EXCEEDED", Map.ofEntries(
            Map.entry("user_id", userId),
            Map.entry("endpoint", endpoint),
            Map.entry("ip_address", maskIP(ipAddress))
        ));
    }
    
    /**
     * Log security event
     */
    public void logSecurityEvent(String eventType, String description, String userId) {
        if (!enabled) return;
        
        auditLog("SECURITY_EVENT:" + eventType, Map.ofEntries(
            Map.entry("event_type", eventType),
            Map.entry("description", truncate(description, 500)),
            Map.entry("user_id", userId)
        ));
    }
    
    /**
     * Internal method to log audit record
     */
    private void auditLog(String action, Map<String, String> data) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(action).append("] ");
            data.forEach((k, v) -> sb.append(k).append("=").append(v).append(" "));
            
            // Set MDC for structured logging
            MDC.put("action", action);
            MDC.put("timestamp", Instant.now().toString());
            data.forEach(MDC::put);
            
            auditLogger.info(sb.toString());
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Mask sensitive values (API keys, tokens, etc)
     */
    private String maskSensitive(String key, String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        key = key.toLowerCase();
        if (key.contains("password") || key.contains("token") || 
            key.contains("key") || key.contains("secret")) {
            return "***MASKED***";
        }
        
        return value;
    }
    
    /**
     * Mask IP address for privacy (last octet hidden)
     */
    private String maskIP(String ip) {
        if (ip == null || ip.isEmpty()) {
            return ip;
        }
        
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + "." + parts[2] + ".***";
        }
        
        return ip;
    }
    
    /**
     * Truncate long strings
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        
        if (str.length() <= maxLength) {
            return str;
        }
        
        return str.substring(0, maxLength) + "...[truncated]";
    }
    
    /**
     * Get audit logger statistics
     */
    public Map<String, Object> getStatistics() {
        return Map.ofEntries(
            Map.entry("enabled", enabled),
            Map.entry("status", enabled ? "ACTIVE" : "DISABLED")
        );
    }
}
