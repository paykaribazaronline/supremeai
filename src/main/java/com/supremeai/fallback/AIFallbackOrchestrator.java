package com.supremeai.fallback;

import com.supremeai.cost.QuotaManager;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class AIFallbackOrchestrator {

    private final QuotaManager quotaManager;

    // Ordered by preference: Primary -> Secondary -> Free tier backup
    private final List<AIProvider> fallbackChain = Arrays.asList(
            AIProvider.GROQ_LLAMA3,
            AIProvider.GEMINI_PRO,
            AIProvider.HUGGINGFACE_FREE
    );

    public AIFallbackOrchestrator(QuotaManager quotaManager) {
        this.quotaManager = quotaManager;
    }

    public String executeWithFallback(String prompt) {
        for (AIProvider provider : fallbackChain) {
            if (isProviderAvailable(provider)) {
                try {
                    System.out.println("Attempting execution with: " + provider);
                    String result = callAIProvider(provider, prompt);
                    
                    // Record usage upon success
                    recordUsage(provider);
                    return result;
                } catch (Exception e) {
                    System.err.println(provider + " failed. Switching to next fallback...");
                    // Continue loop to try next provider
                }
            } else {
                System.out.println(provider + " quota exhausted. Skipping...");
            }
        }
        
        throw new RuntimeException("CRITICAL: All AI providers failed or exhausted their quotas!");
    }

    private boolean isProviderAvailable(AIProvider provider) {
        // Map Enum to our Quota Manager keys
        String serviceName = getServiceNameForProvider(provider);
        
        // If we don't track it, assume it's free/unlimited (like HuggingFace Free)
        if (quotaManager.getQuotaStatus(serviceName, "Requests") == null) {
            return true; 
        }

        // Try to record a dry run (0 cost) just to check if limit is reached
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

    private String callAIProvider(AIProvider provider, String prompt) throws Exception {
        // Mocking the actual API calls
        if (provider == AIProvider.GROQ_LLAMA3 && Math.random() < 0.2) {
             throw new Exception("Groq Timeout"); // Simulate 20% random failure
        }
        return "Response from " + provider + ": Validated answer.";
    }
}