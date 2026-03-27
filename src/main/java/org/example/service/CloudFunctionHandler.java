package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Phase 4: Firebase Cloud Functions Handler
 * 
 * Bridges Java backend with Firebase Cloud Functions
 * Allows deploying data collection logic as serverless functions
 * 
 * Features:
 * - HTTP function interface for Cloud Functions
 * - Request validation and rate limiting
 * - Response formatting (JSON)
 * - Error handling and logging
 * - Admin authentication via header tokens
 * 
 * Cloud Functions deploy pattern:
 * 1. Extract this class and use as Cloud Function handler
 * 2. Deploy to Firebase: firebase deploy --only functions
 * 3. Access via: https://region-projectid.cloudfunctions.net/collectData
 * 
 * URL patterns:
 * POST /collectData?source=github&owner=X&repo=Y
 * POST /collectData?source=vercel&projectId=Z
 * POST /collectData?source=firebase&projectId=Z
 * GET /health - Health check
 */
public class CloudFunctionHandler {
    private static final Logger logger = LoggerFactory.getLogger(CloudFunctionHandler.class);
    
    private static final ObjectMapper mapper = new ObjectMapper();
    
    // Rate limiting: requests per minute per IP
    private static final Map<String, RateLimitBucket> rateLimitBuckets = new ConcurrentHashMap<>();
    private static final int RATE_LIMIT_PER_MINUTE = 60;
    
    // Simple token-based auth for Cloud Function access
    private static final String ADMIN_TOKEN = System.getenv("SUPREMEAI_ADMIN_TOKEN");
    
    private final DataCollectorService collectorService;
    
    /**
     * Constructor - Initialize with DataCollectorService
     * Used when deploying as Cloud Function
     */
    public CloudFunctionHandler(DataCollectorService collectorService) {
        this.collectorService = collectorService;
        logger.info("🚀 Cloud Function Handler initialized");
    }
    
    /**
     * Handle HTTP request from Cloud Function
     * This method is called by Firebase Cloud Functions framework
     * 
     * @param queryParams Query parameters from request
     * @param headers Request headers
     * @param method HTTP method
     * @return Response data as Map
     */
    public Map<String, Object> handleRequest(Map<String, String> queryParams, 
                                            Map<String, String> headers, 
                                            String method) {
        try {
            // Extract client IP for rate limiting
            String clientIP = headers.getOrDefault("X-Forwarded-For", "unknown");
            
            // Rate limit check
            if (!checkRateLimit(clientIP)) {
                return createErrorResponse(429, "Rate limit exceeded - max 60 requests/minute");
            }
            
            // Get request path from query params
            String path = queryParams.getOrDefault("path", "/healthCheck");
            
            // Route based on path
            return switch (path) {
                case "/health", "/healthCheck" -> handleHealthCheck();
                case "/collectData" -> handleCollectData(queryParams, headers);
                case "/stats" -> handleStats();
                default -> createErrorResponse(404, "Endpoint not found");
            };
            
        } catch (Exception e) {
            logger.error("❌ Cloud Function error", e);
            return createErrorResponse(500, "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * Handle health check request
     */
    private Map<String, Object> handleHealthCheck() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "healthy");
        health.put("version", "4.0");
        health.put("timestamp", System.currentTimeMillis());
        health.put("function_type", "Cloud Function");
        health.put("region", System.getenv("REGION") != null ? System.getenv("REGION") : "auto");
        
        logger.debug("✅ Health check passed");
        return health;
    }
    
    /**
     * Handle data collection request
     * Query parameters:
     * - source: github | vercel | firebase
     * - owner, repo (for github)
     * - projectId (for vercel/firebase)
     * - token (optional, for admin verification)
     */
    private Map<String, Object> handleCollectData(Map<String, String> queryParams,
                                                  Map<String, String> headers) {
        String source = queryParams.getOrDefault("source", "");
        String token = headers.getOrDefault("Authorization", 
            queryParams.getOrDefault("token", ""));
        
        // Verify admin token if required
        if (ADMIN_TOKEN != null && !ADMIN_TOKEN.isEmpty() && !token.equals(ADMIN_TOKEN)) {
            logger.warn("🔐 Unauthorized access attempt");
            return createErrorResponse(401, "Unauthorized - invalid token");
        }
        
        Map<String, Object> result;
        
        try {
            switch (source) {
                case "github" -> {
                    String owner = queryParams.get("owner");
                    String repo = queryParams.get("repo");
                    
                    if (owner == null || repo == null) {
                        return createErrorResponse(400, "Missing parameters: owner, repo");
                    }
                    
                    logger.info("📡 Cloud Function: Collecting GitHub data for {}/{}", owner, repo);
                    result = collectorService.getGitHubData(owner, repo);
                }
                
                case "vercel" -> {
                    String projectId = queryParams.get("projectId");
                    
                    if (projectId == null) {
                        return createErrorResponse(400, "Missing parameter: projectId");
                    }
                    
                    logger.info("📡 Cloud Function: Collecting Vercel status for {}", projectId);
                    result = collectorService.getVercelStatus(projectId);
                }
                
                case "firebase" -> {
                    String projectId = queryParams.get("projectId");
                    // Note: Firebase status collection doesn't require projectId
                    // It collects status for the current project
                    
                    logger.info("📡 Cloud Function: Collecting Firebase status");
                    result = collectorService.getFirebaseStatus();
                }
                
                default -> {
                    return createErrorResponse(400, 
                        "Invalid source - use: github, vercel, or firebase");
                }
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("❌ Data collection failed", e);
            return createErrorResponse(500, "Collection failed: " + e.getMessage());
        }
    }
    
    /**
     * Handle statistics request
     */
    private Map<String, Object> handleStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("rate_limit_buckets", rateLimitBuckets.size());
        stats.put("uptime", "tracking enabled");
        stats.put("timestamp", System.currentTimeMillis());
        
        return stats;
    }
    
    /**
     * Check rate limit for IP address
     * Returns true if request is allowed
     */
    private boolean checkRateLimit(String clientIP) {
        RateLimitBucket bucket = rateLimitBuckets.computeIfAbsent(clientIP, 
            k -> new RateLimitBucket());
        
        return bucket.allowRequest();
    }
    
    /**
     * Create error response
     */
    private Map<String, Object> createErrorResponse(int statusCode, String message) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("status", "error");
        error.put("statusCode", statusCode);
        error.put("message", message);
        error.put("timestamp", System.currentTimeMillis());
        
        return error;
    }
    
    // ========== Inner Classes ==========
    
    /**
     * Simple sliding window rate limiter
     */
    public static class RateLimitBucket {
        private final Queue<Long> timestamps = new java.util.LinkedList<>();
        private static final long WINDOW_MS = 60_000; // 1 minute
        
        public synchronized boolean allowRequest() {
            long now = System.currentTimeMillis();
            
            // Remove old timestamps outside window
            while (!timestamps.isEmpty() && timestamps.peek() < now - WINDOW_MS) {
                timestamps.poll();
            }
            
            // Check limit
            if (timestamps.size() >= RATE_LIMIT_PER_MINUTE) {
                return false;
            }
            
            timestamps.offer(now);
            return true;
        }
    }
}
