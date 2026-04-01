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
            
            // ✅ CHECK: Is user ADMIN?
            if (!user.getRole().equals("ADMIN")) {
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
            
            // ✅ NEW: Check provider quotas
            if (quotaService.shouldUseFallback()) {
                logger.warn("⚠️ Provider quotas critically low - less than 5 AIs have quota");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("status", "error", "message", "System quota critically low. Please try again later."));
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
     * Submit multiple requirements at once
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
            
            @SuppressWarnings("unchecked")
            List<String> requirements = (List<String>) request.get("requirements");
            if (requirements == null || requirements.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Requirements list required"));
            }
            
            Map<String, Boolean> results = new HashMap<>();
            int successCount = 0;
            
            for (String req : requirements) {
                boolean success = selfExtender.implementRequirement(req);
                results.put(req, success);
                if (success) successCount++;
            }
            
            logger.info("✅ Batch processing: {}/{} successful", successCount, requirements.size());
            
            return ResponseEntity.ok(Map.of(
                "status", successCount == requirements.size() ? "success" : "partial",
                "processed", requirements.size(),
                "successful", successCount,
                "results", results
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // ========== PRIVATE HELPERS ==========
    
    private User extractUser(String authHeader) throws Exception {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        
        String token = authHeader.substring(7);
        return authService.validateToken(token);
    }
}
