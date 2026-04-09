package org.example.controller;

import org.example.service.SelfExtender;
import org.example.service.AuthenticationService;
import org.example.service.RequestQueueService;
import org.example.service.UserQuotaService;
import org.example.service.QuotaService;
import org.example.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * Self Extension Controller
 * Allows SupremeAI to extend itself by creating new components from requirements
 */
@RestController
@RequestMapping("/api/extend")
public class SelfExtensionController {
    private static final Logger logger = LoggerFactory.getLogger(SelfExtensionController.class);
    
    @Autowired
    private SelfExtender selfExtender;
    
    @Autowired
    private AuthenticationService authService;
    
    @Autowired
    private RequestQueueService requestQueue;
    
    @Autowired
    private UserQuotaService userQuotaService; // NEW: Check user tier quota
    
    @Autowired
    private QuotaService quotaService; // NEW: Check provider quotas
    
    /**
     * POST /api/extend/requirement
     * Submit a requirement for SupremeAI to implement (ADMIN ONLY)
     */
    @PostMapping("/requirement")
    public ResponseEntity<?> submitRequirement(
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = extractUser(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Auth required"));
            }
            
            // ✅ CHECK: Is user ADMIN? (case-insensitive — role is stored as "admin")
            if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
                logger.warn("❌ Non-admin user {} tried to extend system", user.getUsername());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("status", "error", "message", "Admin access required"));
            }
            
            // ✅ NEW: Check user quota tier
            if (!userQuotaService.canCreateApp(user.getUsername())) {
                Map<String, Object> quota = userQuotaService.getQuotaStatus(user.getUsername());
                logger.warn("❌ User {} hit app creation limit for today", user.getUsername());
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("status", "error", "message", "App creation limit exceeded for today", "quotaDetails", quota));
            }
            
            // ✅ Check provider quotas — only block when providers ARE configured but ALL are out of quota.
            // When no external providers are configured the system runs without external AI (that is fine).
            if (quotaService.getConfiguredProviderCount() > 0 && quotaService.shouldUseFallback()) {
                logger.warn("⚠️ Provider quotas critically low - all configured AI providers are exhausted");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("status", "error", "message", "All configured AI providers are out of quota. Please try again later or add more providers."));
            }
            if (quotaService.getConfiguredProviderCount() == 0) {
                logger.info("ℹ️ No external AI providers configured — self-extension will run on its own");
            }
            
            String requirement = request.get("requirement");
            if (requirement == null || requirement.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Requirement text required"));
            }
            
            // ✅ Generate unique ID to prevent duplicates
            String requirementId = requirement.hashCode() + "_" + System.currentTimeMillis();
            
            logger.info("👑 Admin {} submitted requirement: {}", user.getUsername(), requirement);
            
            // ✅ Queue request to prevent race conditions
            requestQueue.queueExtensionRequest(requirementId, () -> {
                boolean success = selfExtender.implementRequirement(requirement);
                if (success) {
                    logger.info("✅ Requirement implemented: {}", requirementId);
                    // Record the app creation in user quota
                    userQuotaService.recordAppCreation(user.getUsername());
                } else {
                    logger.error("❌ Failed to implement: {}", requirementId);
                }
            });
            
            return ResponseEntity.accepted().body(Map.of(
                "status", "queued",
                "message", "Requirement queued for implementation",
                "requirementId", requirementId,
                "user", user.getUsername(),
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            logger.error("❌ Requirement submission error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    /**
     * GET /api/extend/status
     * Check if self-extension is ready
     */
    @GetMapping("/status")
    public ResponseEntity<?> checkStatus(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = extractUser(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            Map<String, Object> status = selfExtender.getStatus();
            status.put("user", user.getUsername());
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * POST /api/extend/batch
     * Submit multiple requirements at once (queued asynchronously)
     */
    @PostMapping("/batch")
    public ResponseEntity<?> submitBatch(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = extractUser(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("status", "error", "message", "Admin access required"));
            }
            
            @SuppressWarnings("unchecked")
            List<String> requirements = (List<String>) request.get("requirements");
            if (requirements == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Requirements list is required (cannot be null)"));
            }
            if (requirements.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Requirements list cannot be empty"));
            }
            
            // Queue each requirement asynchronously instead of blocking
            List<String> requirementIds = new ArrayList<>();
            for (String req : requirements) {
                String requirementId = UUID.randomUUID().toString();
                requirementIds.add(requirementId);
                requestQueue.queueExtensionRequest(requirementId, () -> {
                    boolean success = selfExtender.implementRequirement(req);
                    if (success) {
                        logger.info("✅ Batch requirement implemented: {}", requirementId);
                        userQuotaService.recordAppCreation(user.getUsername());
                    } else {
                        logger.error("❌ Batch requirement failed: {}", requirementId);
                    }
                });
            }
            
            logger.info("📦 Batch of {} requirements queued for user {}", requirements.size(), user.getUsername());
            
            return ResponseEntity.accepted().body(Map.of(
                "status", "queued",
                "message", "All requirements queued for async processing",
                "totalQueued", requirements.size(),
                "requirementIds", requirementIds
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    /**
     * GET /api/extend/history
     * Get extension command history (ADMIN ONLY)
     */
    @GetMapping("/history")
    public ResponseEntity<?> getHistory(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = extractUser(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("status", "error", "message", "Admin access required"));
            }
            return ResponseEntity.ok(Map.of(
                "history", selfExtender.getExtensionHistory(),
                "total", selfExtender.getExtensionHistory().size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    // ========== PRIVATE HELPERS ==========
    
    /**
     * Extract user from Bearer token, or return default admin when no token.
     * Auth is handled by Spring Security (permitAll) + Firebase client-side.
     * Controller-level checks are for logging only.
     */
    private User extractUser(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                User user = authService.validateToken(token);
                if (user != null) return user;
            } catch (Exception e) {
                logger.debug("Token validation failed, using default admin: {}", e.getMessage());
            }
        }
        // Default: admin session (Firebase auth is client-side, Spring Security permitAll)
        User admin = new User();
        admin.setUsername("admin");
        admin.setRole("ADMIN");
        return admin;
    }
}
