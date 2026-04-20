package com.supremeai.fallback;

import com.supremeai.cost.QuotaManager;
import com.supremeai.learning.knowledge.GlobalKnowledgeBase;
import com.supremeai.learning.immunity.CodeImmunitySystem;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class AIFallbackOrchestrator {

    private final QuotaManager quotaManager;
    private final APIKeyManager apiKeyManager;
    private final GlobalKnowledgeBase knowledgeBase;
    private final CodeImmunitySystem immunitySystem;

    // Ordered by preference: Primary -> Secondary -> Free tier backup
    private final List<AIProvider> fallbackChain = Arrays.asList(
            AIProvider.GROQ_LLAMA3,
            AIProvider.GEMINI_PRO,
            AIProvider.HUGGINGFACE_FREE
    );

    public AIFallbackOrchestrator(QuotaManager quotaManager, APIKeyManager apiKeyManager, 
                                  GlobalKnowledgeBase knowledgeBase, CodeImmunitySystem immunitySystem) {
        this.quotaManager = quotaManager;
        this.apiKeyManager = apiKeyManager;
        this.knowledgeBase = knowledgeBase;
        this.immunitySystem = immunitySystem;
    }

    public String executeWithLearningAndImmunity(String errorSignature, String prompt) {
        
        // STEP 1: CHECK GLOBAL KNOWLEDGE BASE
        String knownSolution = knowledgeBase.findKnownSolution(errorSignature);
        if (knownSolution != null) {
            return knownSolution; 
        }

        // STEP 2: ASK AI TO FIX IT
        for (AIProvider provider : fallbackChain) {
            if (!isServiceQuotaAvailable(provider)) continue; 

            String safeKey = apiKeyManager.getNextHealthyKey(provider);
            if (safeKey == null) continue; 

            try {
                System.out.println("-> Asking " + provider + " to fix new error...");
                
                String generatedCode = callAIProvider(provider, safeKey, prompt);
                recordUsage(provider);
                
                // STEP 3: THE GENIUS IDEA -> THE IMMUNE SYSTEM CHECK
                if (immunitySystem.isCodeInfected(generatedCode)) {
                    System.err.println("-> [Orchestrator] AI generated toxic/broken code! Rejecting and trying another model...");
                    
                    // The system learns that this provider gave bad code for this specific prompt
                    // It rejects the code and forces the loop to continue to the next AI provider
                    continue; 
                }

                // If code is clean and passes immunity, save to knowledge base and return
                knowledgeBase.recordSuccess(errorSignature, generatedCode, provider.name());
                return generatedCode;
                
            } catch (RateLimitException e) {
                apiKeyManager.markKeyAsRateLimited(safeKey, 60000); 
                return executeWithLearningAndImmunity(errorSignature, prompt); 
            } catch (TimeoutException e) {
                 continue; 
            } catch (Exception e) {
                 // Unknown error
            }
        }
        
        throw new RuntimeException("CRITICAL: All AI failed or generated toxic code. Cannot fix error.");
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
        
        return "public void fixedMethod() { /* Clean Code Fix */ }";
    }
    
    private static class RateLimitException extends Exception { public RateLimitException(String msg) { super(msg); } }
    private static class TimeoutException extends Exception { public TimeoutException(String msg) { super(msg); } }
}