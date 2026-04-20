package com.supremeai.fallback;

import com.supremeai.cost.QuotaManager;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class AIFallbackOrchestrator {

    private final QuotaManager quotaManager;
    private final APIKeyManager apiKeyManager;

    // Ordered by preference: Primary -> Secondary -> Free tier backup
    private final List<AIProvider> fallbackChain = Arrays.asList(
            AIProvider.GROQ_LLAMA3,
            AIProvider.GEMINI_PRO,
            AIProvider.HUGGINGFACE_FREE
    );

    public AIFallbackOrchestrator(QuotaManager quotaManager, APIKeyManager apiKeyManager) {
        this.quotaManager = quotaManager;
        this.apiKeyManager = apiKeyManager;
    }

    public String executeWithFallback(String prompt) {
        for (AIProvider provider : fallbackChain) {
            
            // Step 1: Check if the AI service global quota is reached
            if (!isServiceQuotaAvailable(provider)) {
                System.out.println("-> [Orchestrator] " + provider + " global quota exhausted. Moving to next AI Model...");
                continue; // Skip this provider entirely
            }

            // Step 2: Intelligent Key Selection (Round Robin + Cooldown)
            String safeKey = apiKeyManager.getNextHealthyKey(provider);
            
            if (safeKey == null) {
                System.out.println("-> [Warning] All keys for " + provider + " are currently rate-limited (cooling down). Skipping to next Model...");
                continue; // Fast failover without waiting/timing out!
            }

            try {
                System.out.println("-> [Orchestrator] Attempting: " + provider + " (Key: ***" + safeKey.substring(Math.max(0, safeKey.length() - 4)) + ")");
                
                String result = callAIProvider(provider, safeKey, prompt);
                
                // Record successful usage
                recordUsage(provider);
                return result;
                
            } catch (RateLimitException e) {
                System.err.println("   [Rate Limit 429] Key hit limit! Putting it in cooldown for 60 seconds.");
                // Put the key in a 60-second cooldown so we don't try it again immediately 
                apiKeyManager.markKeyAsRateLimited(safeKey, 60000); 
                
                // Instead of a simple continue, we recursively call the orchestrator 
                // so it can immediately grab the next healthy key from the Round-Robin pool
                return executeWithFallback(prompt); 
                
            } catch (TimeoutException e) {
                 System.err.println("   [Timeout 504] " + provider + " server is dead. Failing over to next model immediately!");
                 // Skip the whole provider, no point trying other keys if the provider's server is down
                 continue; 
                 
            } catch (Exception e) {
                System.err.println("   [Error] Unknown error with " + provider + ": " + e.getMessage());
            }
        }
        
        throw new RuntimeException("CRITICAL DOWN: All AI models and all their respective API keys have failed or are rate-limited!");
    }

    private boolean isServiceQuotaAvailable(AIProvider provider) {
        String serviceName = getServiceNameForProvider(provider);
        if (quotaManager.getQuotaStatus(serviceName, "Requests") == null) return true; // Unlimited/Untracked
        return quotaManager.getQuotaStatus(serviceName, "Requests").getRemainingQuota() > 0;
    }

    private void recordUsage(AIProvider provider) {
        String serviceName = getServiceNameForProvider(provider);
        quotaManager.recordUsage(serviceName, "Requests", 1);
    }

    private String getServiceNameForProvider(AIProvider provider) {
         switch(provider) {
             case GROQ_LLAMA3: return "Groq";
             case GEMINI_PRO: return "Google";
             case ANTHROPIC_CLAUDE: return "Anthropic";
             default: return "Unknown";
         }
    }

    private String callAIProvider(AIProvider provider, String apiKey, String prompt) throws Exception {
        // Mocking the actual API calls
        if (provider == AIProvider.GROQ_LLAMA3 && apiKey.contains("11111")) {
            throw new RateLimitException("HTTP 429 Too Many Requests");
        }
        if (provider == AIProvider.GEMINI_PRO) {
             throw new TimeoutException("HTTP 504 Gateway Timeout");
        }
        return "Success! [Model: " + provider + ", Result: Validated answer]";
    }
    
    // Custom exception classes for precise handling
    private static class RateLimitException extends Exception {
        public RateLimitException(String msg) { super(msg); }
    }
    private static class TimeoutException extends Exception {
        public TimeoutException(String msg) { super(msg); }
    }
}