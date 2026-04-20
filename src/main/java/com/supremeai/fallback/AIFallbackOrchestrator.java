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
            
            // Step 1: Check if the AI service itself has quota left before even trying keys
            if (!isServiceQuotaAvailable(provider)) {
                System.out.println("-> [Orchestrator] " + provider + " global quota exhausted. Moving to next AI Model...");
                continue; // Skip this provider entirely
            }

            // Step 2: Try multiple API keys for the SAME provider (API Key Rotation)
            List<String> availableKeys = apiKeyManager.getKeys(provider);
            for (int i = 0; i < availableKeys.size(); i++) {
                String currentKey = availableKeys.get(i);
                
                try {
                    System.out.println("-> [Orchestrator] Attempting: " + provider + " (Key: ***" + currentKey.substring(Math.max(0, currentKey.length() - 4)) + ")");
                    
                    String result = callAIProvider(provider, currentKey, prompt);
                    
                    // Record successful usage
                    recordUsage(provider);
                    return result;
                    
                } catch (Exception e) {
                    System.err.println("   [Failed] Key " + (i + 1) + " of " + provider + " failed. Reason: " + e.getMessage());
                    // Loop continues, trying the next API key for the same model
                }
            }
            
            // Step 3: If all keys for this provider failed, move to the next AI Model in the fallback chain
            System.err.println("-> [Warning] All " + availableKeys.size() + " keys failed for " + provider + ". Failing over to next AI Model.");
        }
        
        throw new RuntimeException("CRITICAL DOWN: All AI models and all their respective API keys have failed!");
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
        // Mocking the actual API calls based on Key and Model
        
        // Simulating that the first Groq key is "Rate Limited", forcing it to try the second key
        if (provider == AIProvider.GROQ_LLAMA3 && apiKey.contains("11111")) {
            throw new Exception("HTTP 429 Too Many Requests (Rate Limit)");
        }
        
        return "Success! [Model: " + provider + ", Result: Validated answer]";
    }
}