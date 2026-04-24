package com.supremeai.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.concurrent.Callable;

@Service
public class SelfHealingService {

    @Autowired
    private AIReasoningService reasoningService;

    /**
     * Execute a task with retry and log reasoning on failure.
     */
    public <T> T executeWithRetry(Callable<T> task, int maxAttempts, long initialBackoff) throws Exception {
        if (maxAttempts < 1) maxAttempts = 1;
        long backoff = initialBackoff;
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return task.call();
            } catch (Exception e) {
                lastException = e;
                
                reasoningService.logReasoning(
                    "RETRY_" + System.currentTimeMillis(),
                    "Execution Attempt Failed",
                    "Attempt " + attempt + " of " + maxAttempts + " failed with: " + e.getMessage(),
                    "SelfHealingService"
                );

                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                    backoff *= 2; 
                } else {
                    handleWorkflowFailure("MAIN_SYSTEM", "TASK_" + System.currentTimeMillis(), e.getMessage());
                }
            }
        }
        throw lastException != null ? lastException : new IllegalStateException("Retry failed");
    }

    public void handleWorkflowFailure(String repo, String workflowId, String errorLog) {
        reasoningService.logReasoning(
                workflowId, 
                "Self-Healing Triggered", 
                "System failure detected. Analyzing error log: " + truncate(errorLog),
                "SupremeAI-SelfHealer"
        );

        String suggestedAction = analyzeError(errorLog);
        System.out.println("Self-Healing suggested action: " + suggestedAction);
    }

    private String analyzeError(String log) {
        if (log == null) return "UNKNOWN";
        if (log.contains("Dependency resolution failed")) return "CHECK_DEPENDENCIES";
        if (log.contains("Tests failed")) return "FIX_TESTS";
        if (log.contains("Unauthorized") || log.contains("401")) return "CHECK_AUTH_TOKENS";
        if (log.contains("Quota exceeded") || log.contains("429")) return "ROTATE_API_KEYS";
        return "GENERAL_SYSTEM_CHECK";
    }

    private String truncate(String text) {
        if (text == null) return "";
        return text.length() > 150 ? text.substring(0, 150) + "..." : text;
    }
}
