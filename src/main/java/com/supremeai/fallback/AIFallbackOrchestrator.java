package com.supremeai.fallback;

import com.supremeai.cost.QuotaManager;
import com.supremeai.learning.knowledge.GlobalKnowledgeBase;
import com.supremeai.learning.immunity.CodeImmunitySystem;
import com.supremeai.intelligence.profiling.AIProfiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class AIFallbackOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AIFallbackOrchestrator.class);
    private final QuotaManager quotaManager;
    private final APIKeyManager apiKeyManager;
    private final GlobalKnowledgeBase knowledgeBase;
    private final CodeImmunitySystem immunitySystem;
    private final AIProfiler aiProfiler;

    private final List<AIProvider> allProviders = Arrays.asList(
            AIProvider.GROQ_LLAMA3,
            AIProvider.GEMINI_PRO,
            AIProvider.HUGGINGFACE_FREE,
            AIProvider.ANTHROPIC_CLAUDE
    );

    public AIFallbackOrchestrator(QuotaManager quotaManager, APIKeyManager apiKeyManager, 
                                  GlobalKnowledgeBase knowledgeBase, CodeImmunitySystem immunitySystem,
                                  AIProfiler aiProfiler) {
        this.quotaManager = quotaManager;
        this.apiKeyManager = apiKeyManager;
        this.knowledgeBase = knowledgeBase;
        this.immunitySystem = immunitySystem;
        this.aiProfiler = aiProfiler;
    }

    public String executeWithSupremeIntelligence(String taskCategory, String errorSignature, String prompt) {
        
        // STEP 1: CHECK GLOBAL KNOWLEDGE BASE
        String knownSolution = knowledgeBase.findKnownSolution(errorSignature);
        if (knownSolution != null) return knownSolution; 

        // STEP 2: DYNAMICALLY RE-ORDER THE FALLBACK CHAIN BASED ON AI PROFILER
        // Instead of hardcoding Groq -> Gemini -> HF...
        // The system asks: "Who is the historic expert for THIS specific task (e.g., 'SQL_FIX')?"
        AIProvider expertProvider = aiProfiler.getBestAIForTask(taskCategory);
        
        List<AIProvider> dynamicChain = new ArrayList<>();
        dynamicChain.add(expertProvider); // Put the expert first!
        
        for (AIProvider p : allProviders) {
            if (p != expertProvider) dynamicChain.add(p); // Add the rest as backups
        }

        // STEP 3: EXECUTE WITH LEARNING, IMMUNITY, AND PERFORMANCE TRACKING
        for (AIProvider provider : dynamicChain) {
            if (!isServiceQuotaAvailable(provider)) continue;

            String safeKey = apiKeyManager.getNextHealthyKey(provider);
            if (safeKey == null) continue;

            long startTime = System.currentTimeMillis();

            try {
                log.info("-> Asking {} (Expert Mode) to handle task: {}", provider, taskCategory);

                String generatedCode = callAIProvider(provider, safeKey, prompt);
                long timeTaken = System.currentTimeMillis() - startTime;

                recordUsage(provider);

                // STEP 4: IMMUNITY CHECK (Did the AI generate toxic code?)
                if (immunitySystem.isCodeInfected(generatedCode)) {
                    log.error("-> [Orchestrator] AI generated toxic/broken code! Rejecting...");
                    // Record failure so AIProfiler knows this AI is bad at this task!
                    aiProfiler.recordPerformance(taskCategory, provider, false, timeTaken);
                    continue;
                }

                // If code is clean:
                // 1. Save to Knowledge Base for future
                // 2. Tell AI Profiler this AI did a GREAT job so it gets higher ranking next time!
                knowledgeBase.recordSuccessWithPermission(errorSignature, generatedCode, provider.name(), timeTaken, 0.95);
                aiProfiler.recordPerformance(taskCategory, provider, true, timeTaken);

                return generatedCode;

            } catch (RateLimitException e) {
                log.warn("Rate limit exceeded for provider: {}", provider);
                apiKeyManager.markKeyAsRateLimited(safeKey, 60000);
                return executeWithSupremeIntelligence(taskCategory, errorSignature, prompt);
            } catch (TimeoutException e) {
                log.warn("Timeout for provider: {} on task: {}", provider, taskCategory);
                long timeTaken = System.currentTimeMillis() - startTime;
                aiProfiler.recordPerformance(taskCategory, provider, false, timeTaken); // Record timeout as failure
                continue;
            } catch (Exception e) {
                log.error("Unknown error from provider: {} on task: {}", provider, taskCategory, e);
                // Continue to next provider
            }
        }

        throw new RuntimeException("CRITICAL: All AI failed. Cannot execute task.");
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
        Thread.sleep((long)(Math.random() * 500)); // Simulate API delay 0-500ms
        return "public void fixedMethod() { /* Clean Code Fix */ }";
    }
    
    private static class RateLimitException extends Exception { public RateLimitException(String msg) { super(msg); } }
    private static class TimeoutException extends Exception { public TimeoutException(String msg) { super(msg); } }
}