package com.supremeai.fallback;

import com.supremeai.cost.QuotaManager;
import com.supremeai.learning.knowledge.GlobalKnowledgeBase;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class AIFallbackOrchestrator {

    private final QuotaManager quotaManager;
    private final APIKeyManager apiKeyManager;
    private final GlobalKnowledgeBase knowledgeBase;

    // Ordered by preference: Primary -> Secondary -> Free tier backup
    private final List<AIProvider> fallbackChain = Arrays.asList(
            AIProvider.GROQ_LLAMA3,
            AIProvider.GEMINI_PRO,
            AIProvider.HUGGINGFACE_FREE
    );

    public AIFallbackOrchestrator(QuotaManager quotaManager, APIKeyManager apiKeyManager, GlobalKnowledgeBase knowledgeBase) {
        this.quotaManager = quotaManager;
        this.apiKeyManager = apiKeyManager;
        this.knowledgeBase = knowledgeBase;
    }

    /**
     * Executes a fix for a given error. This is where Learning happens!
     */
    public String executeWithLearning(String errorSignature, String prompt) {
        
        // STEP 1: CHECK GLOBAL KNOWLEDGE BASE (Has any AI solved this exact error before?)
        String knownSolution = knowledgeBase.findKnownSolution(errorSignature);
        if (knownSolution != null) {
            // We saved money and time! No API call needed.
            return knownSolution; 
        }

        // STEP 2: NO KNOWN SOLUTION. ASK AI TO FIX IT.
        for (AIProvider provider : fallbackChain) {
            
            if (!isServiceQuotaAvailable(provider)) {
                continue; 
            }

            String safeKey = apiKeyManager.getNextHealthyKey(provider);
            if (safeKey == null) {
                continue; 
            }

            try {
                System.out.println("-> Asking " + provider + " to fix new error: " + errorSignature);
                
                String newSolutionCode = callAIProvider(provider, safeKey, prompt);
                
                // Record API usage
                recordUsage(provider);
                
                // STEP 3: THE AI FIXED IT! NOW SYSTEM LEARNS AND STORES IT.
                // The next time this error happens, the system won't ask the AI, it will just use this solution!
                knowledgeBase.recordSuccess(errorSignature, newSolutionCode, provider.name());
                
                return newSolutionCode;
                
            } catch (RateLimitException e) {
                apiKeyManager.markKeyAsRateLimited(safeKey, 60000); 
                return executeWithLearning(errorSignature, prompt); 
            } catch (TimeoutException e) {
                 continue; 
            } catch (Exception e) {
                 // Unknown error
            }
        }
        
        throw new RuntimeException("CRITICAL: All AI failed. Cannot fix error.");
    }

    private boolean isServiceQuotaAvailable(AIProvider provider) {
        String serviceName = getServiceNameForProvider(provider);
        if (quotaManager.getQuotaStatus(serviceName, "Requests") == null) return true; 
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
        if (provider == AIProvider.GROQ_LLAMA3 && apiKey.contains("11111")) throw new RateLimitException("HTTP 429");
        if (provider == AIProvider.GEMINI_PRO) throw new TimeoutException("HTTP 504");
        
        // Simulating the AI giving a perfect code fix
        return "public void fixedMethod() { /* " + provider + " magically fixed the bug! */ }";
    }
    
    private static class RateLimitException extends Exception { public RateLimitException(String msg) { super(msg); } }
    private static class TimeoutException extends Exception { public TimeoutException(String msg) { super(msg); } }
}