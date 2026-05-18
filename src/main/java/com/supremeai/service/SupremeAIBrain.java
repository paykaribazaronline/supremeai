package com.supremeai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.supremeai.fallback.AIFallbackOrchestrator;
import com.supremeai.learning.SupremeLearningOrchestrator;
import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import com.supremeai.repository.ProviderRepository;
import com.supremeai.repository.SolutionMemoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * ════════════════════════════════════════════════════════════
 *  SupremeAI Brain — সিস্টেমের একমাত্র Central AI Execution Point
 * ════════════════════════════════════════════════════════════
 *
 * আর্কিটেকচার নীতি:
 * ──────────────────────────────────────────────────────────
 *  ✅ সিস্টেমের EVERY feature এই service দিয়ে AI call করবে
 *  ✅ SupremeAI নিজেই DEFAULT brain — কোনো external AI নয়
 *  ✅ Admin যদি helper AI configure করেন → SupremeCore সেটা ব্যবহার করে
 *  ✅ Helper AI না থাকলে → core_knowledge.json + Firebase memory
 *  ✅ সম্পূর্ণ Reactive — কোনো .block() নেই
 *  ✅ Task-aware — feature অনুযায়ী সঠিক AI route করে
 * ──────────────────────────────────────────────────────────
 *
 * Usage (যেকোনো Service-এ):
 * ─────────────────────────
 *   @Autowired SupremeAIBrain brain;
 *
 *   brain.think("generate spring boot controller for users")  // general
 *   brain.think("CODE_GENERATION", prompt)                    // task-specific
 *   brain.think("TRANSLATION", prompt)                        // translation
 *   brain.think("VISION", imagePrompt)                        // vision
 *
 * ════════════════════════════════════════════════════════════
 */
@Service
public class SupremeAIBrain {

    private static final Logger logger = LoggerFactory.getLogger(SupremeAIBrain.class);

    // ── Task category constants (Admin এই categories দিয়ে providers configure করেন)
    public static final String TASK_CHAT           = "CHAT";
    public static final String TASK_CODE_GENERATION = "CODE_GENERATION";
    public static final String TASK_CODE_REVIEW     = "CODE_REVIEW";
    public static final String TASK_TRANSLATION     = "TRANSLATION";
    public static final String TASK_VISION          = "VISION";
    public static final String TASK_ANALYSIS        = "ANALYSIS";
    public static final String TASK_REASONING       = "REASONING";
    public static final String TASK_SELF_HEALING    = "SELF_HEALING";
    public static final String TASK_SECURITY        = "SECURITY";
    public static final String TASK_TESTING         = "TESTING";
    public static final String TASK_ORCHESTRATION   = "ORCHESTRATION";
    public static final String TASK_GENERAL         = "GENERAL";

    @Autowired
    private AIProviderFactory providerFactory;

    @Autowired
    private AIFallbackOrchestrator fallbackOrchestrator;

    @Autowired
    private SupremeLearningOrchestrator learningOrchestrator;

    @Autowired
    private SolutionMemoryRepository solutionMemoryRepository;

    @Autowired
    private ProviderRepository providerRepository;

    // Simple in-memory stats (Admin dashboard এ দেখানো হবে)
    private final Map<String, Long> taskCallCount = new ConcurrentHashMap<>();
    private final Map<String, Long> taskSuccessCount = new ConcurrentHashMap<>();

    // ══════════════════════════════════════════════════════════
    //  PRIMARY API — সব feature এই method গুলো ব্যবহার করবে
    // ══════════════════════════════════════════════════════════

    /**
     * General-purpose AI thinking — task category ছাড়া।
     * SupremeCore নিজে সিদ্ধান্ত নেয় কোন helper AI (যদি থাকে) ব্যবহার করবে।
     *
     * @param prompt ব্যবহারকারীর প্রশ্ন বা request
     * @return AI response (সবসময় কিছু না কিছু দেবে, কখনো empty নয়)
     */
    public Mono<String> think(String prompt) {
        return think(TASK_GENERAL, prompt);
    }

    /**
     * Task-specific AI thinking — Admin task অনুযায়ী helper AI route করতে পারেন।
     *
     * @param taskCategory TASK_* constants ব্যবহার করুন (যেমন: TASK_CODE_GENERATION)
     * @param prompt       কাজের বিবরণ বা প্রশ্ন
     * @return AI response
     */
    public Mono<String> think(String taskCategory, String prompt) {
        return think(taskCategory, prompt, "NO_SIGNATURE");
    }

    /**
     * Full AI thinking — task, prompt এবং error signature সহ।
     * AIFallbackOrchestrator এর সাথে SupremeCore orchestration একত্রিত।
     *
     * @param taskCategory   TASK_* constant
     * @param prompt         AI-কে দেওয়া instruction
     * @param errorSignature known error/task signature (cache lookup-এর জন্য)
     * @return AI response — সবসময় non-null, কখনো error throw করে না
     */
    public Mono<String> think(String taskCategory, String prompt, String errorSignature) {
        if (prompt == null || prompt.isBlank()) {
            return Mono.just("[SupremeAI Brain] প্রম্পট খালি। কিছু লিখুন।");
        }

        String task = taskCategory != null ? taskCategory.toUpperCase() : TASK_GENERAL;
        trackCall(task);
        long startTime = System.currentTimeMillis();

        // ── Phase 3 Optimization: Check Solution Memory Cache first ──
        if (errorSignature != null && !errorSignature.equals("NO_SIGNATURE")) {
            return solutionMemoryRepository.findByTriggerError(errorSignature)
                .filter(sol -> !sol.isObsolete())
                .sort((a, b) -> Double.compare(b.calculateSupremeScore(), a.calculateSupremeScore()))
                .next()
                .map(sol -> {
                    logger.info("[BRAIN] Optimized Path: Found known solution in memory for {}", errorSignature);
                    trackSuccess(task);
                    return sol.getResolvedCode();
                })
                .switchIfEmpty(executeWithHubOrchestration(task, prompt, errorSignature, startTime));
        }

        return executeWithHubOrchestration(task, prompt, errorSignature, startTime);
    }

    private Mono<String> executeWithHubOrchestration(String task, String prompt, String errorSignature, long startTime) {
        Map<String, String> hubInfo;
        try {
            hubInfo = learningOrchestrator.identifyBestHub(prompt);
        } catch (Exception e) {
            logger.warn("[BRAIN] Orchestrator intent classification failed: {}. Using defaults.", e.getMessage());
            hubInfo = Map.of("hub", "general", "cluster", "general");
        }
        String suggestedHub = hubInfo.get("hub");
        String suggestedCluster = hubInfo.get("cluster");
        logger.info("[BRAIN] Task={} | Intent→Hub: {} | Cluster: {} | Prompt={}",
                task, suggestedHub, suggestedCluster, truncate(prompt));

        return fallbackOrchestrator
                .executeWithSupremeIntelligence(task, errorSignature, prompt)
                .filter(response -> response != null && !response.isBlank())
                .switchIfEmpty(Mono.defer(() -> {
                    logger.info("[BRAIN] Fallback orchestrator returned empty. Trying any available cloud provider.");
                    return tryAnyCloudProvider(prompt);
                }))
                .onErrorResume(e -> {
                    logger.warn("[BRAIN] Fallback orchestrator failed: {}. Trying any available cloud provider.", e.getMessage());
                    return tryAnyCloudProvider(prompt);
                })
                .doOnNext(response -> {
                    long ms = System.currentTimeMillis() - startTime;
                    trackSuccess(task);
                    logger.debug("[BRAIN] Task={} completed in {}ms", task, ms);
                })
                .defaultIfEmpty("[SupremeAI Core] কোনো response পাওয়া যায়নি। পরে চেষ্টা করুন।");
    }

    private Mono<String> tryAnyCloudProvider(String prompt) {
        List<String> providers = providerFactory.getAvailableProviderIds();
        for (String providerName : providers) {
            try {
                AIProvider provider = providerFactory.getProvider(providerName);
                logger.info("[BRAIN] Trying cloud provider: {}", providerName);
                return provider.generate(prompt)
                    .onErrorResume(e -> {
                        logger.warn("[BRAIN] Cloud provider {} failed: {}", providerName, e.getMessage());
                        return Mono.empty();
                    });
            } catch (Exception e) {
                logger.debug("[BRAIN] Could not create provider {}: {}", providerName, e.getMessage());
            }
        }
        return Mono.just("[SupremeAI Core] সকল cloud provider বর্তমানে অনুপলব্ধ। Admin প্যানেল থেকে provider চেক করুন।");
    }

    // ══════════════════════════════════════════════════════════
    //  FEATURE-SPECIFIC CONVENIENCE METHODS
    //  (প্রতিটি feature এর জন্য clean, named API)
    // ══════════════════════════════════════════════════════════

    /** Code generate করতে */
    public Mono<String> generateCode(String specification) {
        return think(TASK_CODE_GENERATION, specification);
    }

    /** Code review করতে */
    public Mono<String> reviewCode(String code) {
        return think(TASK_CODE_REVIEW, "Review this code and provide detailed feedback:\n\n" + code);
    }

    /** Error fix suggestion দিতে */
    public Mono<String> fixError(String errorDescription, String errorSignature) {
        return think(TASK_SELF_HEALING,
                "Analyze this error and provide a fix:\n\n" + errorDescription,
                errorSignature);
    }

    /** Translation করতে */
    public Mono<String> translate(String text, String targetLanguage) {
        return think(TASK_TRANSLATION,
                String.format("Translate the following to %s:\n\n%s", targetLanguage, text));
    }

    /** Security analysis করতে */
    public Mono<String> analyzeSecurity(String code) {
        return think(TASK_SECURITY,
                "Perform a security audit on this code and identify vulnerabilities:\n\n" + code);
    }

    /** Test generation করতে */
    public Mono<String> generateTests(String code) {
        return think(TASK_TESTING,
                "Generate comprehensive unit tests for this code:\n\n" + code);
    }

    /** General reasoning/analysis করতে */
    public Mono<String> analyze(String topic) {
        return think(TASK_ANALYSIS, topic);
    }

    /** Vision/image analysis করতে */
    public Mono<String> analyzeImage(String imageDescription) {
        return think(TASK_VISION, imageDescription);
    }

    /** Chat/conversational response */
    public Mono<String> chat(String message) {
        return think(TASK_CHAT, message);
    }

    /** Agent orchestration decisions */
    public Mono<String> orchestrate(String agentTask) {
        return think(TASK_ORCHESTRATION, agentTask);
    }

    /**
     * Phase 1: Hub Identification for Super-Hub Orchestrator.
     * Maps user intent to a specific Hub ID.
     */
    public Mono<String> identifyHub(String query) {
        return Mono.fromCallable(() -> {
            Map<String, String> hubInfo = learningOrchestrator.identifyBestHub(query);
            String hubName = hubInfo.get("hub").toLowerCase();
            
            if (hubName.contains("development")) return "dev_hub";
            if (hubName.contains("language") || hubName.contains("marketing")) return "lang_hub";
            if (hubName.contains("security")) return "security_hub";
            if (hubName.contains("visual") || hubName.contains("voice") || hubName.contains("multimodal")) return "multimodal_hub";
            if (hubName.contains("memory")) return "memory_hub";
            
            return "lang_hub"; // Default fallback
        });
    }

    // ══════════════════════════════════════════════════════════
    //  ADMIN STATS
    // ══════════════════════════════════════════════════════════

    /** Admin dashboard এর জন্য Brain-এর usage statistics */
    public Mono<Map<String, Object>> getBrainStats() {
        return providerFactory.getActiveHelperProviderIds().collectList()
                .timeout(java.time.Duration.ofSeconds(3))
                .onErrorReturn(java.util.Collections.emptyList())
                .map(helpers -> {
                    Map<String, Object> stats = new java.util.LinkedHashMap<>();
                    stats.put("totalTaskCalls", taskCallCount);
                    stats.put("totalSuccesses", taskSuccessCount);
                    stats.put("availableHelperAIs", helpers);
                    stats.put("defaultProvider", "SupremeAI-Core");
                    return stats;
                });
    }

    // ══════════════════════════════════════════════════════════
    //  PHASE 2: LEARNING ORCHESTRATOR INTEGRATION
    // ══════════════════════════════════════════════════════════

    /**
     * Phase 2: Expose orchestrator learning stats for admin dashboard.
     * Returns intent classification counts, hub routing distribution,
     * and correction history summary.
     */
    public Mono<Map<String, Object>> getLearningStats() {
        return Mono.fromCallable(() -> {
            Map<String, Object> stats = new java.util.LinkedHashMap<>();
            try {
                JsonNode root = learningOrchestrator.getRootNode();
                JsonNode reservoir = root.path("learning_reservoir");
                ArrayNode corrections = (ArrayNode) reservoir.path("recent_corrections");

                stats.put("totalCorrections", corrections.size());
                stats.put("systemVersion", root.path("system_identity").path("version").asText("unknown"));
                stats.put("intentTaxonomyCategories", root.path("intent_taxonomy").size());

                // Hub routing distribution from recent corrections
                Map<String, Long> hubDistribution = new LinkedHashMap<>();
                for (JsonNode c : corrections) {
                    String hub = c.path("corrected_hub").asText("unknown");
                    hubDistribution.merge(hub, 1L, Long::sum);
                }
                stats.put("hubRoutingDistribution", hubDistribution);
            } catch (Exception e) {
                logger.warn("[BRAIN] Could not collect learning stats: {}", e.getMessage());
                stats.put("error", e.getMessage());
            }
            return stats;
        });
    }

    // ══════════════════════════════════════════════════════════
    //  INTERNAL HELPERS
    // ══════════════════════════════════════════════════════════

    private void trackCall(String task) {
        taskCallCount.merge(task, 1L, Long::sum);
    }

    private void trackSuccess(String task) {
        taskSuccessCount.merge(task, 1L, Long::sum);
    }

    private String truncate(String text) {
        if (text == null) return "";
        return text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }
}
