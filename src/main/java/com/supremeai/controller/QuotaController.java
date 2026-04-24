package com.supremeai.controller;

import com.supremeai.model.ActivityLog;
import com.supremeai.repository.ActivityLogRepository;
import com.supremeai.service.QuotaService;
import com.supremeai.service.QuotaPredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Quota management endpoints.
 * Users can only view their own quota. Admins can view and manage any user's quota.
 */
@RestController
@RequestMapping("/api/quota")
@CrossOrigin(origins = "*")
public class QuotaController {

    private final QuotaService quotaService;
    
    @Autowired
    private QuotaPredictionService predictionService;
    
    @Autowired
    private ActivityLogRepository activityLogRepository;

    public QuotaController(QuotaService quotaService) {
        this.quotaService = quotaService;
    }
    
    private String getCurrentAdminUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("Not authenticated");
        }
        return auth.getName();
    }

    /**
     * GET /api/quota/{userId} - Get quota usage for a user.
     * Users can only view their own quota. Admins can view any user's quota.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getQuota(@PathVariable String userId) {
        // Check authorization
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("Not authenticated");
        }
        
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        // Only allow users to view their own quota or admins to view any user
        if (!isAdmin && !auth.getName().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You can only view your own quota");
        }
        
        QuotaService.UserUsageStats stats = quotaService.getUsageStats(userId);
        if (stats == null) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        return ResponseEntity.ok(Map.of(
                "currentUsage", stats.getCurrentUsage(),
                "monthlyQuota", stats.getMonthlyQuota(),
                "lastUsedAt", stats.getLastUsedAt() != null ? stats.getLastUsedAt().toString() : null,
                "hasQuotaRemaining", stats.isHasQuotaRemaining(),
                "usagePercentage", stats.getUsagePercentage()
        ));
    }

    /**
     * POST /api/quota/{userId}/reset - Reset a user's quota (ADMIN only).
     */
    @PostMapping("/{userId}/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> resetQuota(@PathVariable String userId) {
        boolean success = quotaService.resetUserUsage(userId);
        if (success) {
            // Log admin action
            ActivityLog log = new ActivityLog();
            log.setUser(getCurrentAdminUserId());
            log.setAction("RESET_USER_QUOTA");
            log.setCategory("QUOTA_MANAGEMENT");
            log.setSeverity("WARN"); // Quota resets are notable events
            log.setOutcome("SUCCESS");
            log.setDetails("Reset monthly quota for user: " + userId);
            activityLogRepository.save(log).block();
            
            return ResponseEntity.ok(Map.of("status", "success", "message", "Quota reset for user " + userId));
        }
        return ResponseEntity.status(404).body(Map.of("error", "User not found"));
    }

    /**
     * GET /api/quota/{userId}/prediction - Get quota prediction for a user.
     * Users can only view their own prediction. Admins can view any user's prediction.
     */
    @GetMapping("/{userId}/prediction")
    public ResponseEntity<Map<String, Object>> getQuotaPrediction(@PathVariable String userId) {
        // Check authorization
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("Not authenticated");
        }
        
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        
        // Only allow users to view their own prediction or admins to view any user
        if (!isAdmin && !auth.getName().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You can only view your own quota prediction");
        }
        
        return ResponseEntity.ok(predictionService.getPrediction(userId));
    }

    @GetMapping("/warnings")
    public ResponseEntity<Map<String, Object>> getQuotaWarnings() {
        return ResponseEntity.ok(Map.of(
            "warnings", predictionService.getUsersNeedingWarning(),
            "timestamp", System.currentTimeMillis()
        ));
    }
}
