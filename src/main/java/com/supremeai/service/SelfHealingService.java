package com.supremeai.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.function.Supplier;

@Service("selfHealingService-service")
public class SelfHealingService {

    @Autowired
    private AIReasoningService reasoningService;
    
    @Autowired
    public void setReasoningService(AIReasoningService reasoningService) {
        this.reasoningService = reasoningService;
    }

    /**
     * Execute a task with retry and log reasoning on failure.
     */
    public <T> Mono<T> executeWithRetry(Supplier<Mono<T>> taskSupplier, int maxAttempts, long initialBackoff) {
        return taskSupplier.get()
            .retryWhen(reactor.util.retry.Retry.backoff(maxAttempts - 1, Duration.ofMillis(initialBackoff))
                .doBeforeRetry(signal -> log.warn("Retrying due to: {}", signal.failure().getMessage())))
            .onErrorResume(e -> {
                reasoningService.logReasoning(
                    "RETRY_" + System.currentTimeMillis(),
                    "Execution Attempt Failed",
                    "Attempt failed with: " + e.getMessage(),
                    "SelfHealingService"
                );
                handleWorkflowFailure("MAIN_SYSTEM", "TASK_" + System.currentTimeMillis(), e.getMessage());
                return Mono.error(e);
            });
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SelfHealingService.class);

    public void handleWorkflowFailure(String repo, String workflowId, String errorLog) {
        reasoningService.logReasoning(
                workflowId, 
                "Self-Healing Triggered", 
                "System failure detected. Analyzing error log: " + truncate(errorLog),
                "SupremeAI-SelfHealer"
        );

        String suggestedAction = analyzeError(errorLog);
        log.info("Self-Healing suggested action: {}", suggestedAction);
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
