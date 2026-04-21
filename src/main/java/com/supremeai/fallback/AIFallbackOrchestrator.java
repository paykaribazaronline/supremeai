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
    private final GlobalKnowledgeBase knowledgeBase;
    private final CodeImmunitySystem immunitySystem;
    private final AIProfiler aiProfiler;

    private final List<AIProvider> allProviders = Arrays.asList(
            AIProvider.GROQ_LLAMA3,
            AIProvider.GEMINI_PRO,
            AIProvider.HUGGINGFACE_FREE,
            AIProvider.ANTHROPIC_CLAUDE
    );

    public AIFallbackOrchestrator(QuotaManager quotaManager, 
                                  GlobalKnowledgeBase knowledgeBase, CodeImmunitySystem immunitySystem,
                                  AIProfiler aiProfiler) {
        this.quotaManager = quotaManager;
        this.knowledgeBase = knowledgeBase;
        this.immunitySystem = immunitySystem;
        this.aiProfiler = aiProfiler;
    }

    public String executeWithSupremeIntelligence(String taskCategory, String errorSignature, String prompt) {
        
        // STEP 1: CHECK GLOBAL KNOWLEDGE BASE
        String knownSolution = knowledgeBase.findKnownSolution(errorSignature);
        if (knownSolution != null) return knownSolution; 

        // STEP 2: DYNAMICALLY RE-ORDER THE FALLBACK CHAIN
        AIProvider expertProvider = aiProfiler.getBestAIForTask(taskCategory);
        
        List<AIProvider> dynamicChain = new ArrayList<>();
        dynamicChain.add(expertProvider);
        
        for (AIProvider p : allProviders) {
            if (p != expertProvider) dynamicChain.add(p);
        }

        // STEP 3: EXECUTE
        for (AIProvider provider : dynamicChain) {
            if (!isServiceQuotaAvailable(provider)) continue;

            long startTime = System.currentTimeMillis();

            try {
                log.info("-> Asking {} (Expert Mode) to handle task: {}", provider, taskCategory);

                String generatedCode = callAIProvider(provider, "NO_KEY_NEEDED", prompt);
                long timeTaken = System.currentTimeMillis() - startTime;

                recordUsage(provider);

                if (immunitySystem.isCodeInfected(generatedCode)) {
                    log.error("-> [Orchestrator] AI generated toxic/broken code! Rejecting...");
                    aiProfiler.recordPerformance(taskCategory, provider, false, timeTaken);
                    continue;
                }

                knowledgeBase.recordSuccessWithPermission(errorSignature, generatedCode, provider.name(), timeTaken, 0.95);
                aiProfiler.recordPerformance(taskCategory, provider, true, timeTaken);

                return generatedCode;

            } catch (Exception e) {
                log.error("Error from provider: {} on task: {}", provider, taskCategory, e);
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
        Thread.sleep((long)(Math.random() * 500)); 
        return "public void fixedMethod() { /* Clean Code Fix */ }";
    }
}
