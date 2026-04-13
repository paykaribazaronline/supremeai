package org.example.controller;

import org.example.model.UserTier;
import org.example.model.UserQuotaAllocation;
import org.example.model.User;
import org.example.service.AuthenticationService;
import org.example.service.UserQuotaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * User Tier Management Controller
 * Admin API to manage user tiers and quotas
 */
@RestController
@RequestMapping("/api/tier")
public class UserTierController {
    private static final Logger logger = LoggerFactory.getLogger(UserTierController.class);
    
    @Autowired
    private UserQuotaService userQuotaService;

    @Autowired
    private AuthenticationService authService;
    
    /**
     * GET /api/tier/my-quota - Get current user's quota status
     */
    @GetMapping("/my-quota")
    public ResponseEntity<Map<String, Object>> getMyQuota(@RequestParam(defaultValue = "admin") String userId) {
        logger.info("📊 User {} requesting quota status", userId);
        Map<String, Object> status = userQuotaService.getQuotaStatus(userId);
        return ResponseEntity.ok(status);
    }
    
    /**
     * GET /api/tier/user/{userId} - Get specific user's quota (admin only)
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserQuota(
            @PathVariable String userId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (requireAdmin(authHeader) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admin access required"));
        }
        logger.info("📊 Admin requesting quota for user: {}", userId);
        Map<String, Object> status = userQuotaService.getQuotaStatus(userId);
        return ResponseEntity.ok(status);
    }
    
    /**
     * GET /api/tier/all - Get all users' quotas (admin only)
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllQuotas(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (requireAdmin(authHeader) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Admin access required"));
        }
        logger.info("📊 Admin requesting all user quotas");
        return ResponseEntity.ok(userQuotaService.getAllUserQuotas());
    }
    
    /**
     * POST /api/tier/set-user-tier - Set a user's tier (admin only)
     */
    @PostMapping("/set-user-tier")
    public ResponseEntity<Map<String, String>> setUserTier(
            @RequestParam String userId,
            @RequestParam String tier,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (requireAdmin(authHeader) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("status", "error", "message", "Admin access required"));
        }
        
        logger.info("⚙️ Admin setting tier for user {}: {}", userId, tier);
        
        try {
            UserTier userTier = UserTier.fromString(tier);
            userQuotaService.setUserTier(userId, userTier);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("userId", userId);
            response.put("newTier", userTier.name);
            response.put("dailyLimit", String.valueOf(userTier.dailyLimit));
            response.put("monthlyPrice", "$" + userTier.monthlyPrice + "/month");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Failed to set tier: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * POST /api/tier/make-superadmin - Make user SUPERADMIN (admin only)
     */
    @PostMapping("/make-superadmin")
    public ResponseEntity<Map<String, String>> makeSuperAdmin(
            @RequestParam String userId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (requireAdmin(authHeader) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("status", "error", "message", "Admin access required"));
        }
        logger.warn("⚙️ Making user {} SUPERADMIN", userId);
        
        userQuotaService.setUserTier(userId, UserTier.SUPERADMIN);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("userId", userId);
        response.put("tier", "SUPERADMIN");
        response.put("access", "UNLIMITED");
        response.put("message", "User is now SUPERADMIN with unlimited access");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /api/tier/available-tiers - List all available tiers
     */
    @GetMapping("/available-tiers")
    public ResponseEntity<Map<String, Object>> getAvailableTiers() {
        Map<String, Object> tiers = new HashMap<>();
        
        for (UserTier tier : UserTier.values()) {
            Map<String, Object> tierInfo = new HashMap<>();
            tierInfo.put("name", tier.name);
            tierInfo.put("dailyLimit", tier.dailyLimit == -1 ? "UNLIMITED" : tier.dailyLimit);
            tierInfo.put("monthlyLimit", tier.monthlyLimit == -1 ? "UNLIMITED" : tier.monthlyLimit);
            tierInfo.put("appCreationsPerDay", tier.appCreationsPerDay == -1 ? "UNLIMITED" : tier.appCreationsPerDay);
            tierInfo.put("monthlyPrice", "$" + tier.monthlyPrice + "/month");
            tierInfo.put("isUnlimited", tier.isUnlimited());
            
            tiers.put(tier.name, tierInfo);
        }
        
        logger.info("📋 Available tiers requested");
        return ResponseEntity.ok(tiers);
    }
    
    /**
     * POST /api/tier/reset-monthly - Reset monthly quotas for all users (admin only)
     */
    @PostMapping("/reset-monthly")
    public ResponseEntity<Map<String, String>> resetMonthlyQuotas(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (requireAdmin(authHeader) == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("status", "error", "message", "Admin access required"));
        }
        logger.warn("🔄 Admin resetting monthly quotas for all users");
        
        userQuotaService.resetMonthlyQuotas();
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Monthly quotas reset for all users");
        response.put("timestamp", new Date().toString());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * POST /api/tier/can-request - Check if user can make a request
     */
    @PostMapping("/can-request")
    public ResponseEntity<Map<String, Object>> canMakeRequest(@RequestParam String userId) {
        boolean allowed = userQuotaService.canMakeRequest(userId);
        Map<String, Object> quota = userQuotaService.getQuotaStatus(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("canMakeRequest", allowed);
        response.put("quotaDetails", quota);
        response.put("message", allowed ? "✅ User can make request" : "❌ Quota exceeded");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * POST /api/tier/can-create-app - Check if user can create app
     */
    @PostMapping("/can-create-app")
    public ResponseEntity<Map<String, Object>> canCreateApp(@RequestParam String userId) {
        boolean allowed = userQuotaService.canCreateApp(userId);
        Map<String, Object> quota = userQuotaService.getQuotaStatus(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("canCreateApp", allowed);
        response.put("quotaDetails", quota);
        response.put("message", allowed ? "✅ User can create app" : "❌ App creation limit exceeded");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /api/tier/pricing - Get pricing information for all tiers
     */
    @GetMapping("/pricing")
    public ResponseEntity<Map<String, Object>> getPricing() {
        Map<String, Object> pricing = new HashMap<>();
        
        pricing.put("FREE", createTierPricing(UserTier.FREE));
        pricing.put("STARTER", createTierPricing(UserTier.STARTER));
        pricing.put("PROFESSIONAL", createTierPricing(UserTier.PROFESSIONAL));
        pricing.put("ENTERPRISE", createTierPricing(UserTier.ENTERPRISE));
        
        logger.info("📋 Pricing information requested");
        return ResponseEntity.ok(pricing);
    }
    
    private Map<String, Object> createTierPricing(UserTier tier) {
        Map<String, Object> info = new HashMap<>();
        info.put("name", tier.name);
        info.put("price", "$" + tier.monthlyPrice + "/month");
        info.put("dailyRequests", tier.dailyLimit == -1 ? "UNLIMITED" : tier.dailyLimit);
        info.put("monthlyRequests", tier.monthlyLimit == -1 ? "UNLIMITED" : tier.monthlyLimit);
        info.put("appsPerDay", tier.appCreationsPerDay == -1 ? "UNLIMITED" : tier.appCreationsPerDay);
        return info;
    }

    /**
     * Validate admin access from Bearer token. Requires valid authentication and ADMIN role.
     * Auth is handled by Spring Security + Firebase + JWT validation.
     */
    private User requireAdmin(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing or invalid Authorization header");
        }
        try {
            String token = authHeader.substring(7);
            if (token.isEmpty()) {
                throw new IllegalArgumentException("Bearer token is empty");
            }
            User user = authService.validateToken(token);
            if (user != null && authService.isAdmin(user)) return user;
            if (user == null) {
                throw new IllegalArgumentException("Token validation returned null");
            }
            throw new IllegalArgumentException("User is not an admin");
        } catch (Exception e) {
            logger.warn("Admin authentication failed: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid or expired token: " + e.getMessage());
        }
    }
}
