package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FIXED: AI Capability-Based Router
 * 
 * Problem: Multi-AI consensus was technically impossible with 3 agents and 70% threshold
 * Solution: Replace consensus with capability-based routing
 * 
 * Each AI provider is assigned tasks based on their strengths:
 * - OpenAI GPT-4: Code generation (primary strength)
 * - DeepSeek: Security review (better at security analysis)
 * - Gemini: UI/Design (better at design tasks)
 * - Fallback: Any available provider
 * 
 * No consensus needed - each AI does what it's best at.
 */
@Service
public class AICapabilityRouter {
    
    private static final Logger logger = LoggerFactory.getLogger(AICapabilityRouter.class);
    
    @Autowired
    private AIAPIService aiApiService;
    
    @Autowired
    private QuotaService quotaService;
    
    // AI capabilities mapping
    private final Map<TaskType, List<String>> capabilityMap = new HashMap<>();
    
    // Provider health scores
    private final Map<String, ProviderHealth> providerHealth = new ConcurrentHashMap<>();
    
    // Task routing statistics
    private final Map<TaskType, RoutingStats> routingStats = new ConcurrentHashMap<>();
    
    public enum TaskType {
        CODE_GENERATION("Code generation and implementation"),
        SECURITY_REVIEW("Security analysis and vulnerability detection"),
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
        
        public String getDescription() { return description; }
    }
    
    public AICapabilityRouter() {
        initializeCapabilities();
    }
    
    /**
     * Initialize default AI capabilities
     */
    private void initializeCapabilities() {
        // OpenAI GPT-4 is best at code generation
        capabilityMap.put(TaskType.CODE_GENERATION, 
            Arrays.asList("openai-gpt4", "deepseek", "anthropic-claude"));
        
        // DeepSeek is best at security
        capabilityMap.put(TaskType.SECURITY_REVIEW, 
            Arrays.asList("deepseek", "anthropic-claude", "openai-gpt4"));
        
        // Gemini is best at UI/Design
        capabilityMap.put(TaskType.UI_DESIGN, 
            Arrays.asList("google-gemini", "openai-gpt4", "anthropic-claude"));
        
        // Code review
        capabilityMap.put(TaskType.CODE_REVIEW, 
            Arrays.asList("anthropic-claude", "deepseek", "openai-gpt4"));
        
        // Documentation
        capabilityMap.put(TaskType.DOCUMENTATION, 
            Arrays.asList("openai-gpt4", "anthropic-claude", "google-gemini"));
        
        // Test generation
        capabilityMap.put(TaskType.TEST_GENERATION, 
            Arrays.asList("openai-gpt4", "deepseek", "anthropic-claude"));
        
        // Error fixing
        capabilityMap.put(TaskType.ERROR_FIXING, 
            Arrays.asList("deepseek", "openai-gpt4", "anthropic-claude"));
        
        // Banglish support
        capabilityMap.put(TaskType.BANGLISH_SUPPORT, 
            Arrays.asList("google-gemini", "openai-gpt4", "anthropic-claude"));
        
        // Fallback
        capabilityMap.put(TaskType.FALLBACK, 
            Arrays.asList("openai-gpt4", "anthropic-claude", "deepseek", "google-gemini", "cohere", "perplexity"));
    }
    
    /**
     * Route task to best AI provider based on capability
     */
    public RoutingDecision route(TaskType taskType, String prompt) {
        logger.info("🎯 Routing {} task to best AI provider", taskType);
        
        long startTime = System.currentTimeMillis();
        
        // Available providers are already quota-filtered by QuotaService.
        List<String> availableProviders = getAnyAvailableProvider();
        List<String> prioritizedProviders = prioritizeAvailableProviders(taskType, availableProviders);

        if (prioritizedProviders.isEmpty()) {
            logger.warn("⚠️ No capable providers available for {}, using fallback", taskType);
            return new RoutingDecision(null, taskType, null, 
                "No AI providers available", false, 0);
        }
        
        // Select best provider (first in priority list)
        String selectedProvider = prioritizedProviders.get(0);
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Record stats
        recordRouting(taskType, selectedProvider, duration);
        
        logger.info("✅ Routed {} task to {} (took {}ms)", 
            taskType, selectedProvider, duration);
        
        return new RoutingDecision(
            selectedProvider, 
            taskType, 
            prioritizedProviders,
            "Routed based on capability", 
            true,
            duration
        );
    }

    /**
     * Prioritize currently available providers for a task.
     */
    public List<String> prioritizeAvailableProviders(TaskType taskType, List<String> availableProviders) {
        if (availableProviders == null || availableProviders.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> preferred = capabilityMap.getOrDefault(taskType, capabilityMap.get(TaskType.FALLBACK));
        Set<String> available = new HashSet<>(availableProviders);
        LinkedHashSet<String> ordered = new LinkedHashSet<>();

        for (String provider : preferred) {
            if (available.contains(provider) && isProviderHealthy(provider)) {
                ordered.add(provider);
            }
        }

        for (String provider : availableProviders) {
            if (isProviderHealthy(provider)) {
                ordered.add(provider);
            }
        }

        return new ArrayList<>(ordered);
    }

    /**
     * Infer task type from prompt content when caller does not provide one.
     */
    public TaskType inferTaskType(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return TaskType.FALLBACK;
        }

        String text = prompt.toLowerCase(Locale.ROOT);
        if (text.contains("security") || text.contains("vulnerability") || text.contains("owasp")) {
            return TaskType.SECURITY_REVIEW;
        }
        if (text.contains("ui") || text.contains("design") || text.contains("layout") || text.contains("frontend")) {
            return TaskType.UI_DESIGN;
        }
        if (text.contains("test") || text.contains("assert") || text.contains("coverage")) {
            return TaskType.TEST_GENERATION;
        }
        if (text.contains("error") || text.contains("exception") || text.contains("stack trace") || text.contains("fix")) {
            return TaskType.ERROR_FIXING;
        }
        if (text.contains("review") || text.contains("quality") || text.contains("refactor")) {
            return TaskType.CODE_REVIEW;
        }
        if (text.contains("document") || text.contains("readme") || text.contains("guide")) {
            return TaskType.DOCUMENTATION;
        }
        if (text.contains("bangla") || text.contains("bengali") || text.contains("banglish")) {
            return TaskType.BANGLISH_SUPPORT;
        }

        return TaskType.CODE_GENERATION;
    }
    
    /**
     * Execute task with routed AI
     */
    public String execute(TaskType taskType, String prompt) {
        RoutingDecision decision = route(taskType, prompt);
        
        if (!decision.isSuccess()) {
            return "[ERROR] " + decision.getReason();
        }
        
        try {
            String response = aiApiService.callProvider(decision.getProvider(), prompt);
            
            // Update provider health based on response
            updateProviderHealth(decision.getProvider(), response != null);
            
            return response;
        } catch (Exception e) {
            logger.error("❌ AI execution failed for {}: {}", 
                decision.getProvider(), e.getMessage());
            updateProviderHealth(decision.getProvider(), false);
            return "[ERROR] " + e.getMessage();
        }
    }
    
    /**
     * Execute with sequential verification pipeline
     * Step 1: Generate with primary AI
     * Step 2: Review with secondary AI
     * Step 3: Auto-fix if issues found
     */
    public SequentialResult executeSequential(TaskType taskType, String prompt) {
        logger.info("🔄 Executing sequential pipeline for {}", taskType);
        
        // Step 1: Generate with primary AI
        RoutingDecision primaryDecision = route(taskType, prompt);
        if (!primaryDecision.isSuccess()) {
            return new SequentialResult(null, null, false, "Primary routing failed");
        }
        
        String draft = aiApiService.callProvider(primaryDecision.getProvider(), prompt);
        if (draft == null || draft.startsWith("[ERROR]")) {
            return new SequentialResult(null, draft, false, "Generation failed");
        }
        
        // Step 2: Review with secondary AI
        RoutingDecision reviewDecision = route(TaskType.CODE_REVIEW, 
            "Review this code:\n" + draft);
        
        String review = aiApiService.callProvider(reviewDecision.getProvider(),
            "Review this code for issues:\n" + draft);
        
        // Step 3: Check if issues found
        boolean hasIssues = review != null && (
            review.toLowerCase().contains("issue") ||
            review.toLowerCase().contains("error") ||
            review.toLowerCase().contains("problem")
        );
        
        if (hasIssues) {
            logger.info("🔧 Issues found in review, attempting auto-fix");
            
            // Auto-fix
            RoutingDecision fixDecision = route(TaskType.ERROR_FIXING,
                "Fix these issues:\n" + review + "\n\nIn this code:\n" + draft);
            
            String fixed = aiApiService.callProvider(fixDecision.getProvider(),
                "Fix these issues in the code:\nIssues: " + review + 
                "\n\nCode:\n" + draft);
            
            return new SequentialResult(
                draft, review, true, "Auto-fixed", fixed, primaryDecision.getProvider()
            );
        }
        
        return new SequentialResult(
            draft, review, true, "No issues found", draft, primaryDecision.getProvider()
        );
    }
    
    /**
     * Check if provider is healthy
     */
    private boolean isProviderHealthy(String provider) {
        ProviderHealth health = providerHealth.get(provider);
        if (health == null) return true; // Assume healthy if unknown
        return health.getFailureRate() < 0.5; // Less than 50% failure rate
    }
    
    /**
     * Update provider health score
     */
    private void updateProviderHealth(String provider, boolean success) {
        providerHealth.compute(provider, (k, v) -> {
            if (v == null) {
                return new ProviderHealth(provider, success ? 0 : 1, 1);
            }
            return v.recordAttempt(success);
        });
    }
    
    /**
     * Get any available provider (fallback)
     */
    private List<String> getAnyAvailableProvider() {
        return quotaService.getAvailableProviders();
    }
    
    /**
     * Record routing statistics
     */
    private void recordRouting(TaskType taskType, String provider, long duration) {
        routingStats.compute(taskType, (k, v) -> {
            if (v == null) {
                return new RoutingStats(taskType, provider, duration);
            }
            return v.recordRouting(provider, duration);
        });
    }
    
    /**
     * Get routing statistics
     */
    public Map<String, Object> getRoutingStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalRoutings", routingStats.values().stream()
            .mapToLong(RoutingStats::getCount)
            .sum());
        
        stats.put("byTaskType", routingStats.entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                e -> e.getKey().name(),
                e -> Map.of(
                    "count", e.getValue().getCount(),
                    "avgDuration", e.getValue().getAverageDuration(),
                    "topProvider", e.getValue().getTopProvider()
                )
            )));
        
        stats.put("providerHealth", providerHealth.entrySet().stream()
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                e -> Map.of(
                    "failureRate", e.getValue().getFailureRate(),
                    "totalAttempts", e.getValue().getTotalAttempts()
                )
            )));
        
        return stats;
    }
    
    /**
     * Update capabilities (can be configured at runtime)
     */
    public void updateCapabilities(TaskType taskType, List<String> providers) {
        capabilityMap.put(taskType, new ArrayList<>(providers));
        logger.info("📝 Updated capabilities for {}: {}", taskType, providers);
    }
    
    /**
     * Routing decision result
     */
    public static class RoutingDecision {
        private final String provider;
        private final TaskType taskType;
        private final List<String> alternativeProviders;
        private final String reason;
        private final boolean success;
        private final long routingTimeMs;
        
        public RoutingDecision(String provider, TaskType taskType,
                              List<String> alternativeProviders,
                              String reason, boolean success, long routingTimeMs) {
            this.provider = provider;
            this.taskType = taskType;
            this.alternativeProviders = alternativeProviders;
            this.reason = reason;
            this.success = success;
            this.routingTimeMs = routingTimeMs;
        }
        
        public String getProvider() { return provider; }
        public TaskType getTaskType() { return taskType; }
        public List<String> getAlternativeProviders() { return alternativeProviders; }
        public String getReason() { return reason; }
        public boolean isSuccess() { return success; }
        public long getRoutingTimeMs() { return routingTimeMs; }
    }
    
    /**
     * Sequential execution result
     */
    public static class SequentialResult {
        private final String draft;
        private final String review;
        private final boolean success;
        private final String message;
        private final String finalOutput;
        private final String primaryProvider;
        
        public SequentialResult(String draft, String review, boolean success, 
                               String message) {
            this(draft, review, success, message, null, null);
        }
        
        public SequentialResult(String draft, String review, boolean success,
                               String message, String finalOutput, String primaryProvider) {
            this.draft = draft;
            this.review = review;
            this.success = success;
            this.message = message;
            this.finalOutput = finalOutput;
            this.primaryProvider = primaryProvider;
        }
        
        public String getDraft() { return draft; }
        public String getReview() { return review; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getFinalOutput() { return finalOutput; }
        public String getPrimaryProvider() { return primaryProvider; }
    }
    
    /**
     * Provider health tracking
     */
    private static class ProviderHealth {
        private final String provider;
        private final int failures;
        private final int totalAttempts;
        
        ProviderHealth(String provider, int failures, int totalAttempts) {
            this.provider = provider;
            this.failures = failures;
            this.totalAttempts = totalAttempts;
        }
        
        ProviderHealth recordAttempt(boolean success) {
            return new ProviderHealth(
                provider,
                success ? failures : failures + 1,
                totalAttempts + 1
            );
        }
        
        double getFailureRate() {
            return totalAttempts == 0 ? 0 : (double) failures / totalAttempts;
        }
        
        int getTotalAttempts() { return totalAttempts; }
    }
    
    /**
     * Routing statistics
     */
    private static class RoutingStats {
        private final TaskType taskType;
        private final Map<String, Long> providerCounts;
        private final long totalCount;
        private final long totalDuration;
        
        RoutingStats(TaskType taskType, String provider, long duration) {
            this.taskType = taskType;
            this.providerCounts = new HashMap<>();
            this.providerCounts.put(provider, 1L);
            this.totalCount = 1;
            this.totalDuration = duration;
        }
        
        private RoutingStats(TaskType taskType, Map<String, Long> providerCounts,
                            long totalCount, long totalDuration) {
            this.taskType = taskType;
            this.providerCounts = providerCounts;
            this.totalCount = totalCount;
            this.totalDuration = totalDuration;
        }
        
        RoutingStats recordRouting(String provider, long duration) {
            Map<String, Long> newCounts = new HashMap<>(providerCounts);
            newCounts.merge(provider, 1L, Long::sum);
            return new RoutingStats(taskType, newCounts, totalCount + 1, 
                totalDuration + duration);
        }
        
        long getCount() { return totalCount; }
        long getAverageDuration() { return totalCount == 0 ? 0 : totalDuration / totalCount; }
        
        String getTopProvider() {
            return providerCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("none");
        }
    }
}
