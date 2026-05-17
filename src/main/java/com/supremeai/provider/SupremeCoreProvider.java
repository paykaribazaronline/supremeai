package com.supremeai.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Collections;
import java.util.Comparator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import com.supremeai.model.APIProvider;
import com.supremeai.repository.SolutionMemoryRepository;
import com.supremeai.repository.ProviderRepository;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

/**
 * SupremeAI Core Provider — সিস্টেমের PRIMARY DEFAULT ORCHESTRATOR
 *
 * আর্কিটেকচার নীতি:
 * ─────────────────────────────────────────────────────
 *  - এটি সবসময় DEFAULT PROVIDER। কোনো external AI model default নয়।
 *  - Admin Firestore-এ যে AI helper configure করেন, SupremeCore সেটা ব্যবহার করে।
 *  - Helper AI না থাকলে / fail করলে internal memory + local seed ব্যবহার করে।
 *
 * Processing Hierarchy:
 *  1. Admin-configured Active Helper AI (Firestore থেকে dynamic, priority অনুযায়ী)
 *  2. Firestore Solution Memory (পূর্বের শেখা সমাধান)
 *  3. Local Core Knowledge Seed (core_knowledge.json)
 *  4. Built-in Emergency Response
 * ─────────────────────────────────────────────────────
 */
public class SupremeCoreProvider implements AIProvider {

    private static final Logger logger = LoggerFactory.getLogger(SupremeCoreProvider.class);

    private final SolutionMemoryRepository memoryRepository;
    private final ProviderRepository providerRepository;
    private final AIProviderFactory providerFactory;
    private final String name;
    private final List<Map<String, String>> localSeedKnowledge;

    /**
     * Primary constructor — full orchestration capability
     */
    public SupremeCoreProvider(SolutionMemoryRepository memoryRepository,
                               ProviderRepository providerRepository,
                               AIProviderFactory providerFactory) {
        this.memoryRepository = memoryRepository;
        this.providerRepository = providerRepository;
        this.providerFactory = providerFactory;
        this.name = "SupremeAI-Core";
        this.localSeedKnowledge = loadLocalSeed();
        logger.info("[SUPREME-CORE] Initialized as PRIMARY ORCHESTRATOR with helper AI support.");
    }

    /**
     * Legacy constructor — offline-only mode (no helper AI)
     * Used as final emergency fallback.
     */
    public SupremeCoreProvider(SolutionMemoryRepository memoryRepository) {
        this.memoryRepository = memoryRepository;
        this.providerRepository = null;
        this.providerFactory = null;
        this.name = "SupremeAI-Core";
        this.localSeedKnowledge = loadLocalSeed();
        logger.warn("[SUPREME-CORE] Initialized in OFFLINE-ONLY mode (no helper AI access).");
    }

    private List<Map<String, String>> loadLocalSeed() {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.io.InputStream is = new org.springframework.core.io.ClassPathResource("core_knowledge.json").getInputStream();
            return mapper.readValue(is, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, String>>>() {});
        } catch (Exception e) {
            logger.error("[SUPREME-CORE] Failed to load local core knowledge: {}", e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, Object> getCapabilities() {
        return Map.of(
            "name", name,
            "role", "primary_orchestrator",
            "type", "internal",
            "offline", true,
            "usesHelperAI", providerRepository != null,
            "speed", "adaptive"
        );
    }

    /**
     * মূল generate() মেথড — Helper AI → Memory → Seed → Emergency
     *
     * Admin Firestore-এ যে providers active করেন, সেগুলো priority অনুযায়ী
     * ব্যবহার করা হয়। SupremeCore নিজে call করে এবং response orchestrate করে।
     */
    @Override
    public Mono<String> generate(String prompt) {
        logger.info("[SUPREME-CORE] Processing prompt via orchestration pipeline: {}", truncate(prompt));

        // Layer 1: Admin-configured helper AI (Firestore থেকে dynamic)
        if (providerRepository != null && providerFactory != null) {
            return generateWithHelperAI(prompt)
                    .switchIfEmpty(Mono.defer(() -> generateFromMemory(prompt)))
                    .onErrorResume(e -> {
                        logger.warn("[SUPREME-CORE] Helper AI layer failed: {}. Falling to memory layer.", e.getMessage());
                        return generateFromMemory(prompt);
                    });
        }

        // Legacy offline mode — no helper AI configured
        return generateFromMemory(prompt);
    }

    /**
     * Layer 1: Admin-configured active helper AI ব্যবহার করে respond করে।
     * Helper AI গুলো priority অনুযায়ী sorted, Admin যাকে সর্বোচ্চ priority দেন সে প্রথমে try হয়।
     */
    private Mono<String> generateWithHelperAI(String prompt) {
        return Flux.defer(() -> providerRepository.findAll())
                .onErrorResume(e -> {
                    logger.error("[SUPREME-CORE] Failed to load helper AI list from Firestore: {}", e.getMessage());
                    return Flux.empty();
                })
                .filter(p -> "active".equalsIgnoreCase(p.getStatus()))
                // Admin-configured priority: lower number = higher priority
                .sort(Comparator.comparingInt(APIProvider::getPriority))
                .flatMap(config -> {
                    AIProvider helper = providerFactory.createProviderFromConfig(config);
                    if (helper == null) return Flux.empty();
                    logger.info("[SUPREME-CORE] Delegating to Admin-configured helper: {} (priority={})",
                            config.getName(), config.getPriority());
                    return helper.generate(prompt)
                            .map(response -> wrapWithCoreHeader(response, config.getName()))
                            .onErrorResume(e -> {
                                logger.warn("[SUPREME-CORE] Helper AI '{}' failed: {}. Trying next.", config.getName(), e.getMessage());
                                return Mono.empty();
                            })
                            .flux();
                }, 1) // concurrency=1: একটি একটি করে try করে (waterfall fallback)
                .next(); // প্রথম সফল response নাও
    }

    /**
     * Layer 2: Firestore Solution Memory থেকে similar task খুঁজে response দেয়।
     */
    private Mono<String> generateFromMemory(String prompt) {
        return Flux.defer(() -> memoryRepository.findAll())
                .onErrorResume(e -> {
                    logger.error("[SUPREME-CORE] Firestore memory unavailable: {}. Using local seed.", e.getMessage());
                    return Flux.empty();
                })
                .filter(m -> m.getTriggerError() != null && isSimilar(prompt, m.getTriggerError()))
                .next()
                .map(memory -> {
                    logger.info("[SUPREME-CORE] Found matching solution in Firestore memory.");
                    return "[SupremeAI Core — Internal Memory]\n" +
                           "আমি আমার শেখা সমাধান থেকে উত্তর দিচ্ছি:\n\n" + memory.getResolvedCode();
                })
                .switchIfEmpty(Mono.<String>defer(() -> {
                    logger.info("[SUPREME-CORE] No memory match found. Trying local knowledge seed.");
                    return Mono.just(findInLocalSeed(prompt));
                }));
    }

    /**
     * Layer 3: Local core_knowledge.json থেকে response দেয়।
     */
    private String findInLocalSeed(String prompt) {
        for (Map<String, String> entry : localSeedKnowledge) {
            String task = entry.get("task");
            if (task != null && isSimilar(prompt, task)) {
                logger.info("[SUPREME-CORE] Found match in local knowledge seed.");
                return "[SupremeAI Core — স্থানীয় জ্ঞানভাণ্ডার]\n" +
                       "আমার স্থানীয় জ্ঞান থেকে উত্তর:\n\n" + entry.get("solution");
            }
        }

        // Layer 4: Emergency built-in response
        logger.warn("[SUPREME-CORE] No match found in any layer. Sending emergency response.");
        return "[SupremeAI Core — অনলাইন মোড]\n" +
               "আপনার বার্তা পেয়েছি। এই মুহূর্তে helper AI সংযোগ নেই এবং আমার মেমোরিতে " +
               "এই বিষয়ে সরাসরি তথ্য নেই। তবে আমি সচল আছি। " +
               "Admin প্যানেল থেকে কোনো AI provider activate করুন।\n" +
               "আপনার প্রশ্ন: " + prompt;
    }

    /**
     * Helper AI response-কে SupremeCore header সহ wrap করে।
     * User জানতে পারে কোন helper AI respond করেছে।
     */
    private String wrapWithCoreHeader(String response, String helperName) {
        return response; // Clean response — header add করা হচ্ছে না ব্যবহারকারীর অভিজ্ঞতার জন্য
    }

    private boolean isSimilar(String prompt, String task) {
        String p = prompt.toLowerCase();
        String t = task.toLowerCase();
        // শব্দ মিল খোঁজা
        String[] promptWords = p.split("\\s+");
        int matchCount = 0;
        for (String word : promptWords) {
            if (word.length() > 3 && t.contains(word)) matchCount++;
        }
        return p.contains(t) || t.contains(p) || (promptWords.length > 0 && matchCount >= Math.min(2, promptWords.length / 2));
    }

    private String truncate(String text) {
        if (text == null) return "";
        return text.length() > 80 ? text.substring(0, 80) + "..." : text;
    }
}
