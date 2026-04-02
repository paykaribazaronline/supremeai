package org.example.controller;

import org.example.service.DataCollectorService;
import org.example.service.AdminMessagePusher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Phase 5: REST API Controller for Data Collection
 * 
 * Exposes DataCollectorService via HTTP endpoints
 * All endpoints are rate-limited and require authentication
 * 
 * Base URL: /api/v1/data
 * 
 * Endpoints:
 * GET /api/v1/data/github/{owner}/{repo} - Collect GitHub data
 * GET /api/v1/data/vercel/{projectId} - Collect Vercel status
 * GET /api/v1/data/firebase - Collect Firebase status
 * GET /api/v1/data/health - System health check
 * GET /api/v1/data/stats - Request statistics
 */
@RestController
@RequestMapping("/api/v1/data")
public class DataController {
    private static final Logger logger = LoggerFactory.getLogger(DataController.class);
    
    @Autowired
    private DataCollectorService dataCollectorService;
    
    @Autowired
    private AdminMessagePusher adminMessagePusher;
    
    /**
     * GET /api/v1/data/github/{owner}/{repo}
     * Collect GitHub repository data
     */
    @GetMapping("/github/{owner}/{repo}")
    public ResponseEntity<?> collectGitHubData(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        try {
            logger.info("📡 REST: GitHub data request for {}/{}", owner, repo);
            
            Map<String, Object> data = dataCollectorService.getGitHubData(owner, repo);
            
            // Push to admin dashboard (non-blocking)
            try {
                adminMessagePusher.pushDataUpdate("github", owner + "/" + repo, data, 0);
            } catch (Exception e) {
                logger.warn("⚠️ Failed to push GitHub data to dashboard", e);
            }
            
            // Wrap in response envelope
            return ResponseEntity.ok(createSuccessResponse(data));
            
        } catch (IllegalArgumentException e) {
            logger.error("❌ Invalid parameters for GitHub data", e);
            return ResponseEntity.badRequest()
                .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("❌ GitHub data collection failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * GET /api/v1/data/vercel/{projectId}
     * Collect Vercel deployment status
     */
    @GetMapping("/vercel/{projectId}")
    public ResponseEntity<?> collectVercelStatus(
            @PathVariable String projectId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        try {
            logger.info("📡 REST: Vercel status request for {}", projectId);
            
            Map<String, Object> data = dataCollectorService.getVercelStatus(projectId);
            
            // Push to admin dashboard (non-blocking)
            try {
                adminMessagePusher.pushDataUpdate("vercel", projectId, data, 0);
            } catch (Exception e) {
                logger.warn("⚠️ Failed to push Vercel data to dashboard", e);
            }
            
            // Wrap in response envelope
            return ResponseEntity.ok(createSuccessResponse(data));
            
        } catch (IllegalArgumentException e) {
            logger.error("❌ Invalid parameters for Vercel status", e);
            return ResponseEntity.badRequest()
                .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("❌ Vercel status collection failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * GET /api/v1/data/firebase
     * Collect Firebase metrics
     */
    @GetMapping("/firebase")
    public ResponseEntity<?> collectFirebaseStatus(
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        try {
            logger.info("📡 REST: Firebase status request");
            
            Map<String, Object> data = dataCollectorService.getFirebaseStatus();
            
            // Push to admin dashboard (non-blocking)
            try {
                adminMessagePusher.pushDataUpdate("firebase", "default", data, 0);
            } catch (Exception e) {
                logger.warn("⚠️ Failed to push Firebase data to dashboard", e);
            }
            
            // Wrap in response envelope
            return ResponseEntity.ok(createSuccessResponse(data));
            
        } catch (Exception e) {
            logger.error("❌ Firebase status collection failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * GET /api/v1/data/health
     * System health check
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        try {
            logger.debug("✅ Health check requested");
            
            Map<String, Object> health = dataCollectorService.getSystemHealth();
            
            return ResponseEntity.ok(createSuccessResponse(health));
            
        } catch (Exception e) {
            logger.error("❌ Health check failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * GET /api/v1/data/stats
     * Request statistics for monitoring
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        try {
            logger.debug("📊 Stats request");
            
            Map<String, Object> stats = dataCollectorService.getRequestStats();
            
            return ResponseEntity.ok(createSuccessResponse(stats));
            
        } catch (Exception e) {
            logger.error("❌ Stats retrieval failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse(e.getMessage()));
        }
    }
    
    /**
     * POST /api/v1/data/cache/clear
     * Clear request cache (admin only)
     */
    @PostMapping("/cache/clear")
    public ResponseEntity<?> clearCache(
            @RequestParam(defaultValue = "all") String key,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        try {
            logger.info("🗑️ Cache clear request for: {}", key);
            
            dataCollectorService.clearCache(key);
            
            return ResponseEntity.ok(Map.of(
                "message", "Cache cleared for: " + key,
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            logger.error("❌ Cache clear failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse(e.getMessage()));
        }
    }
    
    // ========== Helper Methods ==========
    
    /**
     * Create standardized success response envelope
     */
    private Map<String, Object> createSuccessResponse(Map<String, Object> data) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("data", data);
        response.put("message", "success");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
    
    /**
     * Create standardized error response envelope
     */
    private Map<String, Object> createErrorResponse(String errorMessage) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("error", errorMessage);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
}
