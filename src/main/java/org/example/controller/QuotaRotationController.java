package org.example.controller;

import org.example.service.QuotaRotationService;
import org.example.service.QuotaRotationService.AIProvider;
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
    private QuotaRotationService quotaService;
    
    /**
     * GET /api/quotas/summary
     * Get overall quota summary for all 10 AI providers
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
            AIProvider provider = quotaService.getNextProvider();
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "✅ Selected");
            response.put("provider", provider.name());
            response.put("display_name", provider.displayName);
            response.put("quota_remaining", "check status endpoint");
            response.put("quota_status", "check status endpoint");
            
            logger.info("🔄 Next provider selected: {}", provider.displayName);
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
            AIProvider provider = quotaService.getOptimalProvider();
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "✅ Recommended");
            response.put("provider", provider.name());
            response.put("display_name", provider.displayName);
            response.put("strategy", "Highest remaining quota + lowest failure rate");
            
            logger.info("✨ Optimal provider recommended: {}", provider.displayName);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Error getting optimal provider: ", e);
            return ResponseEntity.status(500)
                .body(Map.of("status", "❌ Error", "message", e.getMessage()));
        }
    }
    
    /**
     * POST /api/quotas/record-success
     * Record successful API call (consumes 1 quota unit)
     */
    @PostMapping("/record-success")
    public ResponseEntity<Map<String, Object>> recordSuccess(
            @RequestParam String provider,
            @RequestParam(defaultValue = "1") int tokensUsed) {
        try {
            quotaService.recordSuccess(provider, tokensUsed);
            quotaService.resetFailureCount(provider);
            
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
     * Record failed API call (doesn't consume quota)
     */
    @PostMapping("/record-failure")
    public ResponseEntity<Map<String, Object>> recordFailure(@RequestParam String provider) {
        try {
            quotaService.recordFailure(provider);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "⚠️ Failure Recorded");
            response.put("provider", provider);
            response.put("action", "Failure tracked, no quota consumed");
            response.put("note", "After 3 failures, provider will be skipped in rotation");
            
            logger.warn("⚠️ Failure recorded for {}", provider);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Error recording failure: ", e);
            return ResponseEntity.status(400)
                .body(Map.of("status", "❌ Error", "message", e.getMessage()));
        }
    }
    
    /**
     * GET /api/quotas/remaining
     * Get total remaining quota across all providers
     */
    @GetMapping("/remaining")
    public ResponseEntity<Map<String, Object>> getTotalRemaining() {
        try {
            int remaining = quotaService.getTotalRemainingQuota();
            double cost = quotaService.getProjectedMonthlyCost();
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "✅ OK");
            response.put("total_remaining_quota", remaining);
            response.put("projected_monthly_cost", String.format("$%.2f", cost));
            response.put("cost_optimization", cost == 0.0 ? "✅ Using FREE tiers only" : "⚠️ Paid APIs active");
            
            logger.info("✅ Total remaining quota: {} across all providers", remaining);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ Error getting remaining quota: ", e);
            return ResponseEntity.status(500)
                .body(Map.of("status", "❌ Error", "message", e.getMessage()));
        }
    }
    
    /**
     * POST /api/quotas/reset-monthly
     * Manually reset monthly quotas (usually auto-triggered on month boundary)
     */
    @PostMapping("/reset-monthly")
    public ResponseEntity<Map<String, Object>> resetMonthlyQuotas() {
        try {
            quotaService.checkAndResetMonthlyQuotas();
            
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
     * Get list of all 10 supported AI providers
     */
    @GetMapping("/providers")
    public ResponseEntity<Map<String, Object>> getProvidersList() {
        try {
            List<Map<String, Object>> providers = new ArrayList<>();
            
            for (AIProvider provider : AIProvider.values()) {
                providers.add(new LinkedHashMap<String, Object>() {{
                    put("name", provider.name());
                    put("display_name", provider.displayName);
                    put("daily_quota", provider.dailyQuota);
                    put("monthly_quota", provider.dailyQuota * 30);
                    put("monthly_cost_free_tier", "$0.00");
                }});
            }
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "✅ OK");
            response.put("total_providers", providers.size());
            response.put("providers", providers);
            
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
