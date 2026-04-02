package org.example.controller;

import org.example.model.Quota;
import org.example.service.QuotaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * Quota Controller - REST API for quota management
 * Admin can view quotas, reset them, and manage provider limits
 */
@RestController
@RequestMapping("/api/quota")
public class QuotaController {
    private static final Logger logger = LoggerFactory.getLogger(QuotaController.class);
    
    @Autowired
    private QuotaService quotaService;
    
    /**
     * GET /api/quota/summary - Get overall quota summary
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getQuotaSummary() {
        logger.info("📊 Admin requesting quota summary");
        return ResponseEntity.ok(quotaService.getQuotaSummary());
    }
    
    /**
     * GET /api/quota/all - Get all provider quotas
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Quota>> getAllQuotas() {
        logger.info("📊 Admin requesting all quotas");
        return ResponseEntity.ok(quotaService.getAllQuotas());
    }
    
    /**
     * GET /api/quota/provider/{providerId} - Get specific provider quota
     */
    @GetMapping("/provider/{providerId}")
    public ResponseEntity<?> getProviderQuota(@PathVariable String providerId) {
        Quota quota = quotaService.getQuotaDetails(providerId);
        if (quota == null) {
            return ResponseEntity.notFound().build();
        }
        logger.info("📊 Admin requesting quota for {}", providerId);
        return ResponseEntity.ok(quota);
    }
    
    /**
     * GET /api/quota/available - Get list of providers with available quota
     */
    @GetMapping("/available")
    public ResponseEntity<List<String>> getAvailableProviders() {
        logger.info("📊 Checking available providers");
        List<String> available = quotaService.getAvailableProviders();
        return ResponseEntity.ok(available);
    }
    
    /**
     * POST /api/quota/reset - Reset daily quotas (admin only)
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetQuotas() {
        logger.info("⚠️ Admin requesting quota reset");
        quotaService.resetDailyQuotas();
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Daily quotas reset for all providers");
        response.put("timestamp", new Date().toString());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * POST /api/quota/check/{providerId} - Check if specific provider has quota
     */
    @PostMapping("/check/{providerId}")
    public ResponseEntity<Map<String, Object>> checkQuota(@PathVariable String providerId) {
        boolean available = quotaService.canUseAI(providerId);
        Quota quota = quotaService.getQuotaDetails(providerId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("providerId", providerId);
        response.put("available", available);
        response.put("remainingPercent", available ? quota.getRemainingPercentage() : 0);
        response.put("status", quota != null ? quota.getStatus() : "UNKNOWN");
        response.put("message", available ? "✅ Quota available" : "❌ Quota exhausted or insufficient");
        
        logger.info("✅ Quota check for {}: {}", providerId, available);
        return ResponseEntity.ok(response);
    }
    
    /**
     * PUT /api/quota/limit/{providerId} - Update quota limit (admin only)
     */
    @PutMapping("/limit/{providerId}")
    public ResponseEntity<Map<String, String>> updateQuotaLimit(
            @PathVariable String providerId,
            @RequestParam long newLimit) {
        
        logger.info("⚙️ Admin updating quota limit for {}: new limit = {}", providerId, newLimit);
        quotaService.updateQuotaLimit(providerId, newLimit);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("providerId", providerId);
        response.put("newLimit", String.valueOf(newLimit));
        response.put("message", "Quota limit updated");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * POST /api/quota/record - Record usage for a provider (internal use)
     */
    @PostMapping("/record")
    public ResponseEntity<Map<String, String>> recordUsage(
            @RequestParam String providerId,
            @RequestParam(defaultValue = "0") long tokenCount) {
        
        quotaService.recordUsage(providerId, tokenCount);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("providerId", providerId);
        response.put("tokensRecorded", String.valueOf(tokenCount));
        
        logger.debug("📊 Usage recorded for {}: {} tokens", providerId, tokenCount);
        return ResponseEntity.ok(response);
    }
    
    /**
     * GET /api/quota/status - Get overall quota status report
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getQuotaStatus() {
        Map<String, Object> status = new HashMap<>();
        Map<String, Quota> allQuotas = quotaService.getAllQuotas();
        
        List<Map<String, Object>> providers = new ArrayList<>();
        for (Map.Entry<String, Quota> entry : allQuotas.entrySet()) {
            Quota quota = entry.getValue();
            Map<String, Object> providerStatus = new HashMap<>();
            providerStatus.put("providerId", quota.getProviderId());
            providerStatus.put("providerName", quota.getProviderName());
            providerStatus.put("status", quota.getStatus());
            providerStatus.put("usagePercent", String.format("%.2f", quota.getUsagePercentage()));
            providerStatus.put("requestsUsed", quota.getRequestsUsedToday());
            providerStatus.put("dailyLimit", quota.getDailyLimit());
            providerStatus.put("remainingRequests", quota.getRemainingRequests());
            providerStatus.put("canUse", quotaService.canUseAI(entry.getKey()));
            providers.add(providerStatus);
        }
        
        status.put("providers", providers);
        status.put("summary", quotaService.getQuotaSummary());
        status.put("timestamp", new Date().toString());
        
        logger.info("📊 Quota status report generated");
        return ResponseEntity.ok(status);
    }
    
    /**
     * GET /api/quota/fallback-required - Check if fallback mechanism needed
     */
    @GetMapping("/fallback-required")
    public ResponseEntity<Map<String, Object>> checkFallbackRequired() {
        boolean needsFallback = quotaService.shouldUseFallback();
        int healthyCount = quotaService.getHealthyProviderCount();
        
        Map<String, Object> response = new HashMap<>();
        response.put("needsFallback", needsFallback);
        response.put("healthyProviders", healthyCount);
        response.put("message", needsFallback ? 
            "⚠️ No healthy configured AI providers have quota - using fallback mechanism" :
            "✅ Sufficient quota available from multiple providers");
        
        logger.info("⚠️ Fallback required: {}", needsFallback);
        return ResponseEntity.ok(response);
    }
}
