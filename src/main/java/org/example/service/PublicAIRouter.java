package org.example.service;

import org.example.model.AIAccount;
import java.util.*;
import java.util.concurrent.*;

/**
 * Public AI Router - Smart request routing
 * 
 * Core logic:
 * 1. Request comes for provider (e.g., "GPT-4")
 * 2. Router selects best available account
 * 3. If account fails → auto-rotate to next
 * 4. Track usage, handle billing
 * 5. Return response
 * 
 * Handles:
 * - Multi-account routing
 * - Rate limiting
 * - Budget enforcement
 * - Fallback chains
 * - Error recovery
 */
public class PublicAIRouter {
    private final AIAccountManager accountManager;
    private final BudgetManager budgetManager;
    private final AIAPIService apiService;
    private final FirebaseService firebaseService;
    
    // Metrics tracking
    private final Map<String, RouterMetrics> metrics;
    
    public PublicAIRouter(AIAccountManager accountManager, BudgetManager budgetManager,
                          AIAPIService apiService, FirebaseService firebase) {
        this.accountManager = accountManager;
        this.budgetManager = budgetManager;
        this.apiService = apiService;
        this.firebaseService = firebase;
        this.metrics = new ConcurrentHashMap<>();
    }
    
    /**
     * 🚀 MAIN ENTRY POINT: Route a request to best available account
     * 
     * @param provider AI Provider (e.g., "GPT-4", "Claude-3")
     * @param prompt User's prompt/request
     * @param metadata Additional info (project ID, user email, etc)
     * @return Response from AI or error message
     */
    public RouterResponse routeRequest(String provider, String prompt, Map<String, String> metadata) {
        long startTime = System.currentTimeMillis();
        RouterResponse response = new RouterResponse();
        response.provider = provider;
        response.timestamp = new Date();
        
        try {
            // Step 1: Check if we can proceed (hard stop enforcement)
            if (!budgetManager.canProceedWithRequest(provider)) {
                response.success = false;
                response.error = "All accounts for " + provider + " have exceeded budget. Processing blocked.";
                response.errorCode = "HARD_STOP";
                recordMetrics(provider, false, 0);
                return response;
            }
            
            // Step 2: Select best account
            AIAccount account = accountManager.selectBestAccount(provider);
            if (account == null) {
                response.success = false;
                response.error = "No available accounts for " + provider;
                response.errorCode = "NO_ACCOUNTS";
                recordMetrics(provider, false, 0);
                return response;
            }
            
            response.selectedAccount = account.getAccountId();
            
            // Step 3: Rate limiting check
            if (!account.canMakeRequest()) {
                response.success = false;
                response.error = "Rate limit exceeded for " + account.getAccountId();
                response.errorCode = "RATE_LIMIT";
                recordMetrics(provider, false, 0);
                return response;
            }
            
            // Step 4: Send API request
            try {
                APIResponse apiResponse = sendToAPI(account, prompt, metadata);
                
                if (apiResponse.success) {
                    // ✅ Success path
                    response.success = true;
                    response.content = apiResponse.content;
                    response.tokensUsed = apiResponse.tokensUsed;
                    response.costEstimate = apiResponse.costEstimate;
                    
                    // Record usage
                    account.recordSuccess(apiResponse.costEstimate, 
                                         (int)(System.currentTimeMillis() - startTime));
                    accountManager.recordSuccess(account.getAccountId(), 
                                               apiResponse.costEstimate,
                                               (int)(System.currentTimeMillis() - startTime));
                    
                    recordMetrics(provider, true, apiResponse.costEstimate);
                    
                    System.out.println("✅ Request successful via: " + account.getAccountId());
                    
                } else {
                    // ❌ API call failed - try fallback
                    System.out.println("⚠️ Request failed: " + apiResponse.error);
                    return handleAPIFailure(provider, prompt, metadata, account, apiResponse.error);
                }
                
            } catch (Exception e) {
                System.out.println("❌ Exception during API call: " + e.getMessage());
                return handleAPIFailure(provider, prompt, metadata, account, e.getMessage());
            }
            
        } catch (Exception e) {
            response.success = false;
            response.error = "Router error: " + e.getMessage();
            response.errorCode = "ROUTER_ERROR";
            System.err.println("❌ Router error: " + e.getMessage());
        }
        
        response.processingTime = System.currentTimeMillis() - startTime;
        return response;
    }
    
    /**
     * Handle API failure - Try fallback accounts
     */
    private RouterResponse handleAPIFailure(String provider, String prompt, Map<String, String> metadata,
                                            AIAccount failedAccount, String error) {
        RouterResponse response = new RouterResponse();
        response.provider = provider;
        response.selectedAccount = failedAccount.getAccountId();
        response.timestamp = new Date();
        
        // Log failure
        accountManager.recordFailure(failedAccount.getAccountId(), error);
        
        // Special handling for specific errors
        if (error.contains("quota") || error.contains("limit exceeded")) {
            try {
                accountManager.banAccount(failedAccount.getAccountId(), 
                    "Ban triggered: " + error);
            } catch (Exception e) {
                System.err.println("Error banning account: " + e.getMessage());
            }
        }
        
        // Attempt fallback
        System.out.println("🔄 Attempting fallback...");
        AIAccount fallbackAccount = accountManager.getNextAvailableAccount(provider, failedAccount.getAccountId());
        
        if (fallbackAccount != null) {
            // Retry with fallback account
            response.fallbackUsed = true;
            response.selectedAccount = fallbackAccount.getAccountId();
            
            try {
                APIResponse apiResponse = sendToAPI(fallbackAccount, prompt, metadata);
                
                if (apiResponse.success) {
                    response.success = true;
                    response.content = apiResponse.content;
                    response.tokensUsed = apiResponse.tokensUsed;
                    response.costEstimate = apiResponse.costEstimate;
                    
                    accountManager.recordSuccess(fallbackAccount.getAccountId(), 
                                               apiResponse.costEstimate, 
                                               (int)(System.currentTimeMillis() - response.timestamp.getTime()));
                    
                    System.out.println("✅ Fallback successful via: " + fallbackAccount.getAccountId());
                    return response;
                }
            } catch (Exception e) {
                System.out.println("❌ Fallback also failed: " + e.getMessage());
            }
        }
        
        // All attempts failed
        response.success = false;
        response.error = "Primary and all fallback accounts failed. Last error: " + error;
        response.errorCode = "ALL_ACCOUNTS_FAILED";
        
        return response;
    }
    
    /**
     * Send request to actual API
     */
    private APIResponse sendToAPI(AIAccount account, String prompt, Map<String, String> metadata) {
        APIResponse response = new APIResponse();
        
        // TODO: Implement actual API call via apiService
        // This would call the AIAPIService with account.getApiKey()
        
        // For now: Mock response
        response.success = true;
        response.content = "Mock response from " + account.getProvider();
        response.tokensUsed = 150;
        response.costEstimate = 0.0015;  // Rough estimate
        
        // In production:
        // try {
        //     response = apiService.callAPI(
        //         account.getApiKey(),
        //         account.getProvider(),
        //         prompt,
        //         metadata
        //     );
        // } catch (Exception e) {
        //     response.success = false;
        //     response.error = e.getMessage();
        // }
        
        // Track rate limiting
        account.recordRequest();
        
        return response;
    }
    
    /**
     * Record metrics for monitoring
     */
    private void recordMetrics(String provider, boolean success, double cost) {
        metrics.computeIfAbsent(provider, k -> new RouterMetrics())
               .recordRequest(success, cost);
    }
    
    /**
     * Get metrics for a provider
     */
    public RouterMetrics getMetrics(String provider) {
        return metrics.getOrDefault(provider, new RouterMetrics());
    }
    
    /**
     * Get all metrics
     */
    public Map<String, RouterMetrics> getAllMetrics() {
        return new HashMap<>(metrics);
    }
    
    /**
     * Print routing statistics
     */
    public void printRoutingStats() {
        System.out.println("\n📊 ===== ROUTING STATISTICS =====\n");
        
        for (Map.Entry<String, RouterMetrics> entry : metrics.entrySet()) {
            RouterMetrics m = entry.getValue();
            System.out.printf("%s:\n", entry.getKey());
            System.out.printf("  Total Requests: %d (Success: %d, Failed: %d)\n", 
                m.totalRequests, m.successCount, m.failedCount);
            System.out.printf("  Success Rate: %.1f%%\n", m.getSuccessRate());
            System.out.printf("  Total Cost: $%.2f\n", m.totalCost);
            System.out.printf("  Avg Response Time: %.0fms\n", m.getAverageResponseTime());
            System.out.println();
        }
    }
    
    // ========== INNER CLASSES ==========
    
    /**
     * Response from router
     */
    public static class RouterResponse {
        public boolean success;
        public String provider;
        public String selectedAccount;
        public String content;
        public String error;
        public String errorCode;
        public int tokensUsed;
        public double costEstimate;
        public boolean fallbackUsed;
        public long processingTime;
        public Date timestamp;
        
        @Override
        public String toString() {
            return String.format(
                "RouterResponse{success=%b, provider='%s', account='%s', error='%s', cost=$%.3f, time=%dms}",
                success, provider, selectedAccount, error, costEstimate, processingTime
            );
        }
    }
    
    /**
     * Response from API provider
     */
    private static class APIResponse {
        boolean success;
        String content;
        String error;
        int tokensUsed;
        double costEstimate;
    }
    
    /**
     * Metrics for a provider
     */
    public static class RouterMetrics {
        public long totalRequests;
        public long successCount;
        public long failedCount;
        public double totalCost;
        public long totalResponseTime;
        
        public void recordRequest(boolean success, double cost) {
            totalRequests++;
            if (success) {
                successCount++;
            } else {
                failedCount++;
            }
            totalCost += cost;
        }
        
        public double getSuccessRate() {
            if (totalRequests == 0) return 0;
            return (double) successCount / totalRequests * 100;
        }
        
        public double getAverageResponseTime() {
            if (totalRequests == 0) return 0;
            return (double) totalResponseTime / totalRequests;
        }
    }
}
