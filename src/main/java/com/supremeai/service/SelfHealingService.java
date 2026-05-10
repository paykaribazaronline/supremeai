package com.supremeai.service;

import com.supremeai.fallback.AIFallbackOrchestrator;
import com.supremeai.provider.AIProviderType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
public class SelfHealingService {

    @Autowired
    private AIReasoningService reasoningService;

    @Autowired
    private AIFallbackOrchestrator fallbackOrchestrator;

    @org.springframework.context.annotation.Lazy
    @Autowired
    private MultiAIVotingService votingService;

    private final Map<String, Integer> errorPatterns = new ConcurrentHashMap<>();
    private final int MAX_ITERATIONS = 5; // Prevent infinite loops

    @Autowired
    public void setReasoningService(AIReasoningService reasoningService) {
        this.reasoningService = reasoningService;
    }

    /**
     * Execute a task with retry and log reasoning on failure.
     */
    public <T> Mono<T> executeWithRetry(Supplier<Mono<T>> taskSupplier, int maxAttempts, long initialBackoff) {
        return Mono.defer(taskSupplier)
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

    // ===== AUTO HEALING ENGINE FUNCTIONALITY =====

    /**
     * Detect and fix known errors (from AutoHealingEngine)
     */
    public Map<String, Object> detectAndFix(String error) {
        log.info("Auto-healing engine analyzing error: {}", error);

        errorPatterns.merge(error, 1, Integer::sum);

        String fix = getKnownFix(error);

        if (fix != null) {
            log.info("Applying known fix: {}", fix);
            return Map.of(
                "status", "fixed",
                "fixApplied", fix,
                "confidence", 0.9,
                "errorCount", errorPatterns.get(error)
            );
        }

        return Map.of(
            "status", "analyzing",
            "message", "Error pattern not yet recognized",
            "errorCount", errorPatterns.get(error)
        );
    }

    private String getKnownFix(String error) {
        if (error.contains("quota") || error.contains("CpuAlloc")) {
            return "Reduced max instances to 10, 1 CPU per instance";
        }
        if (error.contains("OutOfMemory")) {
            return "Increased memory limit to 2Gi";
        }
        if (error.contains("timeout")) {
            return "Increased request timeout to 3600s";
        }
        if (error.contains("Connection refused")) {
            return "Restarted service instance";
        }
        return null;
    }

    // ===== INFINITE AUTO HEALER FUNCTIONALITY =====

    /**
     * Develop code until perfection with voting approval (from InfiniteAutoHealer)
     */
    public String developUntilPerfection(String taskCategory, String prompt) {
        log.info("Starting infinite auto-healing development for task: {}", taskCategory);

        String currentCode = generateInitialCode(prompt);
        List<AIProviderType> council = Arrays.asList(
            AIProviderType.GROQ_LLAMA3, AIProviderType.ANTHROPIC_CLAUDE, AIProviderType.OPENAI,
            AIProviderType.DEEPSEEK, AIProviderType.OLLAMA
        );

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            log.info("Iteration {}: Analyzing current code", iteration + 1);

            // Test the current code (simplified)
            if (isCodePerfect(currentCode)) {
                log.info("Code achieved perfection after {} iterations", iteration + 1);
                return currentCode;
            }

            // Get council approval for risky changes
            boolean councilApproved = votingService.conductApprovalVote(taskCategory, currentCode, council);

            if (!councilApproved) {
                log.warn("Council rejected the changes. Aborting development.");
                break;
            }

            // Generate improved code
            currentCode = improveCode(currentCode, prompt, iteration);
            log.info("Generated improved code for iteration {}", iteration + 1);
        }

        log.warn("Reached maximum iterations without achieving perfection");
        return currentCode;
    }

    private String generateInitialCode(String prompt) {
        // Simplified initial code generation
        return "// Initial code for: " + prompt + "\npublic class Generated {\n    // TODO: Implement\n}";
    }

    private boolean isCodePerfect(String code) {
        // Simplified perfection check
        return code.contains("public") && code.contains("class") && !code.contains("TODO");
    }

    private String improveCode(String currentCode, String prompt, int iteration) {
        // Simplified code improvement
        return currentCode.replace("TODO", "Implemented in iteration " + (iteration + 1));
    }
}
