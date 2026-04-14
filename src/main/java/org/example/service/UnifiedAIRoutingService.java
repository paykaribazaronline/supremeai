package org.example.service;

import org.example.model.APIProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Unified AI Provider Routing Service (Phase 3 Consolidation)
 * 
 * Consolidates:
 * - CapabilityBasedAIRoutingService (Task matrices & synthesis)
 * - AICapabilityRouter (Prioritized routing & sequential pipelines)
 * - AIProviderRoutingService (Performance metrics & affinity learning)
 */
@Service
public class UnifiedAIRoutingService {
    private static final Logger logger = LoggerFactory.getLogger(UnifiedAIRoutingService.class);

    @Autowired
    private AIAPIService aiApiService;
    
    @Autowired
    private QuotaService quotaService;

    @Autowired
    private SystemModeService systemModeService;

    // Unified Task Types
    public enum TaskType {
        CODE_GENERATION("Generating code, implementing features, debugging"),
        SECURITY_REVIEW("Security scanning, vulnerability analysis, safety checks"),
        ARCHITECTURE_DESIGN("System design, microservices, scalability planning"),
        API_DESIGN("REST API design, endpoint definition, contracts"),
        OPTIMIZATION("Performance tuning, resource optimization, efficiency"),
        COMPLIANCE_CHECK("GDPR, SOC2, regulatory compliance verification"),
        DATA_PIPELINE("Data processing, ETL pipelines, data engineering"),
        KNOWLEDGE_LOOKUP("Factual lookups, current information, web search"),
        REASONING("Complex reasoning, multi-step logic, edge cases"),
        CREATIVE("Creative solutions, novel approaches, brainstorming"),
        UI_DESIGN("User interface and design tasks"),
        CODE_REVIEW("General code review and quality checks"),
        DOCUMENTATION("Documentation generation"),
        TEST_GENERATION("Test case generation"),
        ERROR_FIXING("Error diagnosis and fixing"),
        BANGLISH_SUPPORT("Bengali/English mixed language support"),
        FALLBACK("Any available task");

        private final String description;

        TaskType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Provider performance metrics (merged from AIProviderRoutingService)
    public static class ProviderMetrics {
        private final String providerId;
        private final Map<TaskType, TaskMetrics> taskMetrics = new ConcurrentHashMap<>();
        private int totalFailures = 0;
        private int totalAttempts = 0;

        public ProviderMetrics(String providerId) {
            this.providerId = providerId;
        }

        public void recordAttempt(TaskType taskType, boolean success, long durationMs, double qualityScore) {
            totalAttempts++;
            if (!success) totalFailures++;
            
            taskMetrics.computeIfAbsent(taskType, k -> new TaskMetrics())
                .record(success, durationMs, qualityScore);
        }

        public double getFailureRate() {
            return totalAttempts == 0 ? 0 : (double) totalFailures / totalAttempts;
        }

        public double getSuccessRate(TaskType taskType) {
            TaskMetrics m = taskMetrics.get(taskType);
            return m == null ? 0.5 : m.getSuccessRate();
        }

        public double getQualityScore(TaskType taskType) {
            TaskMetrics m = taskMetrics.get(taskType);
            return m == null ? 0.5 : m.qualityScore;
        }
    }

    private static class TaskMetrics {
        int successCount = 0;
        int failureCount = 0;
        long totalResponseTimeMs = 0;
        double qualityScore = 0.5;

        void record(boolean success, long durationMs, double quality) {
            if (success) {
                successCount++;
                totalResponseTimeMs += durationMs;
                this.qualityScore = 0.7 * this.qualityScore + 0.3 * quality;
            } else {
                failureCount++;
            }
        }

        double getSuccessRate() {
            int total = successCount + failureCount;
            return total == 0 ? 0.5 : (double) successCount / total;
        }
    }

    private final Map<String, ProviderMetrics> providerMetrics = new ConcurrentHashMap<>();
    private final Map<String, Map<TaskType, Double>> staticCapabilityMatrix = new HashMap<>();

    public UnifiedAIRoutingService() {
        initializeStaticMatrix();
    }

    private void initializeStaticMatrix() {
        // Ported from CapabilityBasedAIRoutingService
        staticCapabilityMatrix.put("openai", Map.ofEntries(
            Map.entry(TaskType.CODE_GENERATION, 0.99),
            Map.entry(TaskType.ARCHITECTURE_DESIGN, 0.97),
            Map.entry(TaskType.REASONING, 0.96),
            Map.entry(TaskType.SECURITY_REVIEW, 0.92),
            Map.entry(TaskType.API_DESIGN, 0.98),
            Map.entry(TaskType.OPTIMIZATION, 0.90),
            Map.entry(TaskType.CREATIVE, 0.95),
            Map.entry(TaskType.CODE_REVIEW, 0.96)
        ));

        staticCapabilityMatrix.put("anthropic", Map.ofEntries(
            Map.entry(TaskType.SECURITY_REVIEW, 0.98),
            Map.entry(TaskType.COMPLIANCE_CHECK, 0.97),
            Map.entry(TaskType.REASONING, 0.95),
            Map.entry(TaskType.CODE_GENERATION, 0.92),
            Map.entry(TaskType.CODE_REVIEW, 0.94)
        ));

        staticCapabilityMatrix.put("google", Map.ofEntries(
            Map.entry(TaskType.ARCHITECTURE_DESIGN, 0.96),
            Map.entry(TaskType.KNOWLEDGE_LOOKUP, 0.95),
            Map.entry(TaskType.OPTIMIZATION, 0.94),
            Map.entry(TaskType.BANGLISH_SUPPORT, 0.98),
            Map.entry(TaskType.UI_DESIGN, 0.92)
        ));

        staticCapabilityMatrix.put("deepseek", Map.ofEntries(
            Map.entry(TaskType.CODE_GENERATION, 0.95),
            Map.entry(TaskType.SECURITY_REVIEW, 0.94),
            Map.entry(TaskType.ERROR_FIXING, 0.96),
            Map.entry(TaskType.OPTIMIZATION, 0.92)
        ));
    }

    /**
     * Route task to best AI provider
     */
    public RoutingDecision route(TaskType taskType, String prompt) {
        long startTime = System.currentTimeMillis();
        
        List<String> availableProviders = quotaService.getAvailableProviders();
        List<String> prioritized = prioritizeProviders(taskType, availableProviders);

        if (prioritized.isEmpty()) {
            return new RoutingDecision(null, taskType, Collections.emptyList(), 
                "No providers available", false, System.currentTimeMillis() - startTime);
        }

        String selected = prioritized.get(0);
        long duration = System.currentTimeMillis() - startTime;
        
        return new RoutingDecision(selected, taskType, prioritized, 
            "Routed by capability & performance", true, duration);
    }

    /**
     * Prioritize providers based on static capabilities and learned performance
     */
    public List<String> prioritizeProviders(TaskType taskType, List<String> available) {
        if (available == null || available.isEmpty()) return Collections.emptyList();

        List<String> sorted = available.stream()
            .sorted((p1, p2) -> Double.compare(
                calculateProviderScore(p2, taskType),
                calculateProviderScore(p1, taskType)
            ))
            .collect(Collectors.toList());
            
        // Log prioritization logic
        if (sorted.size() > 1) {
            logger.info("🔄 Provider prioritization for {}: {} (scores: {} vs {})", 
                taskType, sorted.get(0), 
                String.format("%.2f", calculateProviderScore(sorted.get(0), taskType)),
                String.format("%.2f", calculateProviderScore(sorted.get(1), taskType)));
        }
        
        return sorted;
    }

    private double calculateProviderScore(String providerId, TaskType taskType) {
        String normalizedId = normalizeId(providerId);
        
        // 1. Quota Check (Hard filter)
        if (!quotaService.canUseAI(providerId)) {
            return -1.0; // De-prioritize to bottom
        }
        
        // 2. Static capability (40%)
        double staticScore = staticCapabilityMatrix.getOrDefault(normalizedId, new HashMap<>())
            .getOrDefault(taskType, 0.7);
        
        // 3. Learned performance (60%)
        ProviderMetrics metrics = providerMetrics.get(providerId);
        double learnedScore = (metrics == null) ? 0.7 : metrics.getSuccessRate(taskType) * 0.8 + metrics.getQualityScore(taskType) * 0.2;
        
        // 4. Quota Weight (Soft preference for higher remaining quota)
        double quotaWeight = quotaService.getRemainingQuotaPercent(providerId) / 100.0;
        
        // Penalty for high failure rate
        double healthPenalty = (metrics == null) ? 1.0 : (1.0 - metrics.getFailureRate());
        
        return (staticScore * 0.3 + learnedScore * 0.5 + quotaWeight * 0.2) * healthPenalty;
    }

    private String normalizeId(String id) {
        return id.toLowerCase().split("-")[0].split("_")[0];
    }

    /**
     * Infer task type from text
     */
    public TaskType inferTaskType(String text) {
        if (text == null || text.isBlank()) return TaskType.FALLBACK;
        String q = text.toLowerCase();

        if (matchesPattern(q, "security|vulnerable|exploit|attack|authentication|encrypt|owasp")) return TaskType.SECURITY_REVIEW;
        if (matchesPattern(q, "design|architecture|microservice|scalable|pattern|structure")) return TaskType.ARCHITECTURE_DESIGN;
        if (matchesPattern(q, "api|endpoint|rest|http|route|request|response|contract")) return TaskType.API_DESIGN;
        if (matchesPattern(q, "optimize|performance|speed|efficient|memory|cpu|latency")) return TaskType.OPTIMIZATION;
        if (matchesPattern(q, "compliance|gdpr|ccpa|sox|pci|regulation|requirement")) return TaskType.COMPLIANCE_CHECK;
        if (matchesPattern(q, "pipeline|etl|data|batch|stream|spark|hadoop")) return TaskType.DATA_PIPELINE;
        if (matchesPattern(q, "what|when|where|how|why|current|latest|today|news")) return TaskType.KNOWLEDGE_LOOKUP;
        if (matchesPattern(q, "reason|analyze|explain|deduce|prove|logic|think deeply")) return TaskType.REASONING;
        if (matchesPattern(q, "ui|design|layout|frontend|css|html|component")) return TaskType.UI_DESIGN;
        if (matchesPattern(q, "review|quality|refactor")) return TaskType.CODE_REVIEW;
        if (matchesPattern(q, "document|readme|guide")) return TaskType.DOCUMENTATION;
        if (matchesPattern(q, "test|assert|coverage|junit|pytest")) return TaskType.TEST_GENERATION;
        if (matchesPattern(q, "error|exception|stack trace|fix|bug")) return TaskType.ERROR_FIXING;
        if (matchesPattern(q, "bangla|bengali|banglish")) return TaskType.BANGLISH_SUPPORT;
        if (matchesPattern(q, "generate|code|implement|write|build|create")) return TaskType.CODE_GENERATION;

        return TaskType.CODE_GENERATION;
    }

    private boolean matchesPattern(String text, String pattern) {
        return Pattern.compile(pattern).matcher(text).find();
    }

    /**
     * Execute task with routed AI
     */
    public String execute(TaskType taskType, String prompt) {
        RoutingDecision decision = route(taskType, prompt);
        if (!decision.success) return "[ERROR] " + decision.reason;

        long start = System.currentTimeMillis();
        try {
            String response = aiApiService.callProvider(decision.provider, prompt);
            boolean isSuccess = response != null && !response.startsWith("[ERROR]");
            recordPerformance(decision.provider, taskType, isSuccess, System.currentTimeMillis() - start, isSuccess ? 0.9 : 0.0);
            return response;
        } catch (Exception e) {
            recordPerformance(decision.provider, taskType, false, System.currentTimeMillis() - start, 0.0);
            return "[ERROR] " + e.getMessage();
        }
    }

    /**
     * Execute with sequential verification pipeline
     */
    public SequentialResult executeSequential(TaskType taskType, String prompt) {
        logger.info("🔄 Executing sequential pipeline for {}", taskType);
        
        // Step 1: Generate
        RoutingDecision primaryDecision = route(taskType, prompt);
        if (!primaryDecision.success) return new SequentialResult(null, null, false, "Primary routing failed");
        
        String draft = execute(taskType, prompt);
        if (draft == null || draft.startsWith("[ERROR]")) return new SequentialResult(null, draft, false, "Generation failed");
        
        // Step 2: Review
        String reviewPrompt = "Review this content for quality and accuracy:\n" + draft;
        String review = execute(TaskType.CODE_REVIEW, reviewPrompt);
        
        boolean hasIssues = review != null && matchesPattern(review.toLowerCase(), "issue|error|problem|bug|incorrect");
        
        if (hasIssues) {
            SystemModeService.OperationDecision autoFixDecision = systemModeService.canExecuteOperation("FIX_BUGS", 90);
            if (autoFixDecision.isAllowed()) {
                logger.info("🔧 Issues found, attempting auto-fix");
                String fixPrompt = "Fix these issues:\n" + review + "\n\nIn this content:\n" + draft;
                String fixed = execute(TaskType.ERROR_FIXING, fixPrompt);
                return new SequentialResult(draft, review, true, "Auto-fixed", fixed, primaryDecision.provider);
            } else {
                return new SequentialResult(draft, review, true, "Issues found; auto-fix skipped: " + autoFixDecision.getReason(), draft, primaryDecision.provider);
            }
        }
        
        return new SequentialResult(draft, review, true, "No issues found", draft, primaryDecision.provider);
    }

    public void recordPerformance(String providerId, TaskType taskType, boolean success, long durationMs, double qualityScore) {
        providerMetrics.computeIfAbsent(providerId, k -> new ProviderMetrics(providerId))
            .recordAttempt(taskType, success, durationMs, qualityScore);
        
        if (success) {
            quotaService.recordUsage(providerId, 200); // Default estimate
        }
    }

    // Result Classes
    public static class RoutingDecision {
        public final String provider;
        public final TaskType taskType;
        public final List<String> alternatives;
        public final String reason;
        public final boolean success;
        public final long routingTimeMs;

        public RoutingDecision(String provider, TaskType taskType, List<String> alternatives, String reason, boolean success, long routingTimeMs) {
            this.provider = provider;
            this.taskType = taskType;
            this.alternatives = alternatives;
            this.reason = reason;
            this.success = success;
            this.routingTimeMs = routingTimeMs;
        }
    }

    public static class SequentialResult {
        public final String draft;
        public final String review;
        public final boolean success;
        public final String message;
        public final String finalOutput;
        public final String primaryProvider;

        public SequentialResult(String draft, String review, boolean success, String message) {
            this(draft, review, success, message, null, null);
        }

        public SequentialResult(String draft, String review, boolean success, String message, String finalOutput, String primaryProvider) {
            this.draft = draft;
            this.review = review;
            this.success = success;
            this.message = message;
            this.finalOutput = finalOutput;
            this.primaryProvider = primaryProvider;
        }
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("providers", providerMetrics.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> Map.of(
                "failureRate", e.getValue().getFailureRate(),
                "totalAttempts", e.getValue().totalAttempts
            ))));
        return stats;
    }
}
