package org.example.controller;

import org.example.service.QuotaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * Quota Rotation Controller
 * REST endpoints for AI quota management and rotation strategy
 * Admin dashboard + AI services use these to optimize cost
 */
@RestController
@RequestMapping("/api/quotas")
public class QuotaRotationController {
    private static final Logger logger = LoggerFactory.getLogger(QuotaRotationController.class);
    
    @Autowired
    private QuotaService quotaService;
    
    /**
     * GET /api/quotas/summary
        * Get overall quota summary for all admin-configured AI providers
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getQuotaSummary() {
        try {
            Map<String, Object> summary = quotaService.getQuotaSummary();
            logger.info("✅ Quota summary retrieved");
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("❌ Error retrieving quota summary: ", e);
            return ResponseEntity.status(500)
                .body(Map.of("status", "❌ Error", "message", e.getMessage()));
        }
    }
    
    /**
     * GET /api/quotas/status
     * Detailed status for each provider
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getQuotaStatus() {
        try {
            Map<String, Map<String, Object>> status = quotaService.getQuotaStatus();
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "✅ OK");
            response.put("timestamp", new Date());
            response.put("providers", status);
            
            logger.info("✅ Detailed quota status retrieved");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Error retrieving quota status: ", e);
            return ResponseEntity.status(500)
                .body(Map.of("status", "❌ Error", "message", e.getMessage()));
        }
    }
    
    /**
     * GET /api/quotas/next-provider
     * Get next provider for AI call (round-robin rotation)
     */
    @GetMapping("/next-provider")
    public ResponseEntity<Map<String, Object>> getNextProvider() {
        try {
            List<String> available = quotaService.getAvailableProviders();
            if (available.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "status", "⚠️ No Providers",
                    "message", "No AI providers are currently configured or available"
                ));
            }
            
            String providerId = available.get(0); // Simplification for replacement
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "✅ Selected");
            response.put("provider", providerId);
            response.put("display_name", providerId);
            response.put("quota_remaining", quotaService.getRemainingQuotaPercent(providerId) + "%");
            
            logger.info("🔄 Next provider selected: {}", providerId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Error selecting next provider: ", e);
            return ResponseEntity.status(500)
                .body(Map.of("status", "❌ Error", "message", e.getMessage()));
        }
    }
    
    /**
     * GET /api/quotas/optimal-provider
     * Get optimal provider based on highest quota + success rate
     */
    @GetMapping("/optimal-provider")
    public ResponseEntity<Map<String, Object>> getOptimalProvider() {
        try {
            List<String> available = quotaService.getAvailableProviders();
            if (available.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "status", "⚠️ No Providers",
                    "message", "No AI providers are currently configured"
                ));
            }
            
            String providerId = available.stream()
                .max(Comparator.comparingDouble(p -> quotaService.getRemainingQuotaPercent(p)))
                .orElse(available.get(0));
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "✅ Recommended");
            response.put("provider", providerId);
            response.put("display_name", providerId);
            response.put("strategy", "Highest remaining quota");
            
            logger.info("✨ Optimal provider recommended: {}", providerId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Error getting optimal provider: ", e);
            return ResponseEntity.status(500)
                .body(Map.of("status", "❌ Error", "message", e.getMessage()));
        }
    }
    
    /**
     * POST /api/quotas/record-success
     * Record successful API call (consumes tokens)
     */
    @PostMapping("/record-success")
    public ResponseEntity<Map<String, Object>> recordSuccess(
            @RequestParam String provider,
            @RequestParam(defaultValue = "1000") int tokensUsed) {
        try {
            quotaService.recordUsage(provider, tokensUsed);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "✅ Recorded");
            response.put("provider", provider);
            response.put("tokens_used", tokensUsed);
            response.put("action", "Quota consumed");
            
            logger.info("✅ Success recorded for {}", provider);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Error recording success: ", e);
            return ResponseEntity.status(400)
                .body(Map.of("status", "❌ Error", "message", e.getMessage()));
        }
    }
    
    /**
     * POST /api/quotas/record-failure
     * Record failed API call
     */
    @PostMapping("/record-failure")
    public ResponseEntity<Map<String, Object>> recordFailure(@RequestParam String provider) {
        try {
            // QuotaService doesn't have a separate failure record, just logs it
            logger.warn("⚠️ Failure reported for {}", provider);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "⚠️ Failure Recorded (Log Only)");
            response.put("provider", provider);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Error recording failure: ", e);
            return ResponseEntity.status(400)
                .body(Map.of("status", "❌ Error", "message", e.getMessage()));
        }
    }
    
    /**
     * GET /api/quotas/remaining
     * Get total remaining quota status
     */
    @GetMapping("/remaining")
    public ResponseEntity<Map<String, Object>> getTotalRemaining() {
        try {
            Map<String, Object> summary = quotaService.getQuotaSummary();
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("❌ Error getting remaining quota: ", e);
            return ResponseEntity.status(500)
                .body(Map.of("status", "❌ Error", "message", e.getMessage()));
        }
    }
    
    /**
     * POST /api/quotas/reset-monthly
     * Manually reset monthly quotas
     */
    @PostMapping("/reset-monthly")
    public ResponseEntity<Map<String, Object>> resetMonthlyQuotas() {
        try {
            quotaService.resetMonthlyQuotas();
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "✅ Reset");
            response.put("action", "Monthly quotas reset to full capacity");
            response.put("timestamp", new Date());
            
            logger.info("🔄 Monthly quotas manually reset");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Error resetting quotas: ", e);
            return ResponseEntity.status(400)
                .body(Map.of("status", "❌ Error", "message", e.getMessage()));
        }
    }
    
    /**
     * GET /api/quotas/providers
     * Get list of all configured AI providers
     */
    @GetMapping("/providers")
    public ResponseEntity<Map<String, Object>> getProvidersList() {
        try {
            Map<String, org.example.model.Quota> quotas = quotaService.getAllQuotas();
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "✅ OK");
            response.put("total_providers", quotas.size());
            response.put("providers", quotas);
            
            logger.info("✅ Provider list retrieved");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Error retrieving providers: ", e);
            return ResponseEntity.status(500)
                .body(Map.of("status", "❌ Error", "message", e.getMessage()));
        }
    }
    
    /**
     * GET /api/quotas/health
     * Health check - ensure quota system is operational
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> quotaHealth() {
        try {
            int remaining = quotaService.getTotalRemainingQuota();
            boolean healthy = remaining > 0;
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", healthy ? "✅ HEALTHY" : "❌ DEGRADED");
            response.put("quota_remaining", remaining);
            response.put("timestamp", new Date());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Quota health check failed: ", e);
            return ResponseEntity.status(503)
                .body(Map.of("status", "❌ UNHEALTHY", "message", e.getMessage()));
        }
    }
}
