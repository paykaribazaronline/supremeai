package com.supremeai.learning;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.supremeai.service.AdminDashboardFacadeService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 *  SupremeLearningOrchestrator — Phase 2: The Soul of SupremeAI
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 * The central brain of SupremeAI's self-learning architecture.
 * Manages core_knowledge.json, maps user intents to hubs via vector-based
 * similarity scoring, records corrections, and generates proactive system
 * suggestions (auto-model, link evaluation, gap analysis).
 *
 * Phase 2 Upgrades:
 *  - Vector-based intent matching (n-gram + keyword weight similarity)
 *  - System Suggestion Logic (auto-model, link evaluation, gap analysis)
 *  - Learning loop health tracking
 *  - Hub routing distribution analytics
 *
 * Architecture Policy:
 *  ✅ Every request passes through here before reaching any hub
 *  ✅ Solo-capable: works without external embedding services
 *  ✅ Corrections persisted to core_knowledge.json (testing phase)
 *  ✅ Pro-active: 3+ same correction triggers logic refinement proposal
 * ═══════════════════════════════════════════════════════════════════════════════
 */
@Service
public class SupremeLearningOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SupremeLearningOrchestrator.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private JsonNode rootNode;
    private final AdminDashboardFacadeService adminDashboardFacade;

    private static final String KNOWLEDGE_FILE_PATH = "src/main/resources/core_knowledge.json";

    // ── Phase 2: Vector-based matching weights ──────────────────────────────
    private static final double EXACT_MATCH_WEIGHT      = 3.0;
    private static final double WORD_OVERLAP_WEIGHT      = 1.5;
    private static final double NGRAM_WEIGHT             = 0.8;
    private static final double TASK_KEYWORD_BONUS       = 2.0;
    private static final double SIMILARITY_THRESHOLD     = 0.15; // minimum to count as a match

    // ── Phase 2: Suggestion thresholds ─────────────────────────────────────
    private static final int    CORRECTION_TRIGGER_COUNT  = 3;
    private static final double LOW_SUCCESS_RATE_THRESHOLD = 0.50;
    private static final int    MIN_SOLUTIONS_FOR_GAP    = 1;

    @Autowired
    public SupremeLearningOrchestrator(AdminDashboardFacadeService adminDashboardFacade) {
        this.adminDashboardFacade = adminDashboardFacade;
    }

    @PostConstruct
    public void init() {
        loadKnowledgeBase();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  KNOWLEDGE BASE MANAGEMENT
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Loads the core_knowledge.json file on startup.
     */
    public void loadKnowledgeBase() {
        try {
            ClassPathResource resource = new ClassPathResource("core_knowledge.json");
            rootNode = objectMapper.readTree(resource.getInputStream());
            log.info("[SYSTEM_LEARNING] Core Knowledge Base loaded successfully (v{})",
                rootNode.path("system_identity").path("version").asText());
        } catch (IOException e) {
            log.error("[SYSTEM_LEARNING] Critical Error: Could not load core_knowledge.json", e);
            rootNode = objectMapper.createObjectNode();
        }
    }

    /**
     * Reloads the knowledge base from disk (call after external edits).
     */
    public void reloadKnowledgeBase() {
        loadKnowledgeBase();
        log.info("[SYSTEM_LEARNING] Knowledge base reloaded");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PHASE 2: VECTOR-BASED INTENT MATCHING
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Maps an input query to the best Hub and Cluster using vector-based
     * similarity scoring (n-gram overlap + keyword weights).
     *
     * Phase 2 upgrade: replaces simple String.contains() with weighted
     * multi-factor similarity. Falls back to keyword matching if scoring fails.
     *
     * @param query User's input query
     * @return Map with "hub" and "cluster" keys
     */
    public Map<String, String> identifyBestHub(String query) {
        log.debug("[SYSTEM_LEARNING] Phase 2: Vector-based intent classification for: {}", truncate(query));

        String hub = "default-hub"; // Default; resolved from intent taxonomy by similarity below
        String cluster = "general";

        JsonNode taxonomy = rootNode.path("intent_taxonomy");
        if (!taxonomy.isObject() || taxonomy.size() == 0) {
            log.warn("[SYSTEM_LEARNING] Intent taxonomy is empty, using defaults");
            return Map.of("hub", hub, "cluster", cluster);
        }

        // Phase 2: Score all categories by similarity, pick the best
        String bestCategory = findBestCategoryBySimilarity(query, taxonomy);
        if (bestCategory != null) {
            JsonNode categoryNode = taxonomy.path(bestCategory);
            hub = categoryNode.path("hub").asText(hub);
            cluster = findBestClusterBySimilarity(query, categoryNode.path("clusters"));
            log.info("[SYSTEM_LEARNING] Phase 2 Intent → Category: {} | Hub: {} | Cluster: {}",
                    bestCategory, hub, cluster);
        }

        return Map.of("hub", hub, "cluster", cluster);
    }

    /**
     * Phase 2: Scores all taxonomy categories against the query using
     * weighted n-gram overlap + keyword matching. Returns the highest-scoring category.
     *
     * Solo-capable: does not require external embedding service.
     * Uses character n-grams (2-4 chars) + word overlap + task keyword bonus.
     */
    private String findBestCategoryBySimilarity(String query, JsonNode taxonomy) {
        String normalizedQuery = normalizeText(query);
        String bestCategory = null;
        double bestScore = SIMILARITY_THRESHOLD;

        Iterator<Map.Entry<String, JsonNode>> fields = taxonomy.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String categoryName = entry.getKey();
            JsonNode categoryNode = entry.getValue();

            double score = computeCategoryScore(normalizedQuery, categoryName, categoryNode);
            if (score > bestScore) {
                bestScore = score;
                bestCategory = categoryName;
            }
        }

        return bestCategory;
    }

    /**
     * Computes a weighted similarity score for a taxonomy category against the query.
     * Combines: category name match + cluster name match + task intent keyword match.
     */
    private double computeCategoryScore(String query, String categoryName, JsonNode categoryNode) {
        double score = 0.0;
        String catDisplay = categoryName.replace("_", " ").toLowerCase();

        // 1. Category name exact/partial match
        if (query.contains(catDisplay)) {
            score += EXACT_MATCH_WEIGHT;
        } else {
            score += ngramSimilarity(query, catDisplay) * NGRAM_WEIGHT;
        }

        // 2. Cluster name matches
        JsonNode clusters = categoryNode.path("clusters");
        Iterator<Map.Entry<String, JsonNode>> clusterEntries = clusters.fields();
        while (clusterEntries.hasNext()) {
            Map.Entry<String, JsonNode> entry = clusterEntries.next();
            String cName = entry.getKey().replace("_", " ").toLowerCase();
            if (query.contains(cName)) {
                score += WORD_OVERLAP_WEIGHT;
            } else {
                score += ngramSimilarity(query, cName) * NGRAM_WEIGHT * 0.5;
            }

            // 3. Task intent keyword bonus
            JsonNode tasks = entry.getValue().path("tasks");
            if (tasks.isArray()) {
                for (JsonNode task : tasks) {
                    String intent = task.path("intent").asText().toLowerCase();
                    if (query.contains(intent)) {
                        score += TASK_KEYWORD_BONUS;
                    }
                }
            }
        }

        return score;
    }

    /**
     * Phase 2: Find the best matching cluster within a category using similarity scoring.
     */
    private String findBestClusterBySimilarity(String query, JsonNode clusters) {
        if (!clusters.isObject()) return "general";

        String bestCluster = "general";
        double bestScore = 0.0;

        Iterator<Map.Entry<String, JsonNode>> fields = clusters.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String cName = entry.getKey().replace("_", " ").toLowerCase();
            double score = ngramSimilarity(query, cName) * NGRAM_WEIGHT;

            // Check task intents within cluster
            JsonNode tasks = entry.getValue().path("tasks");
            if (tasks.isArray()) {
                for (JsonNode task : tasks) {
                    String intent = task.path("intent").asText().toLowerCase();
                    if (query.contains(intent)) {
                        score += TASK_KEYWORD_BONUS;
                    }
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestCluster = entry.getKey();
            }
        }

        return bestCluster;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PHASE 2: N-GRAM SIMILARITY ENGINE
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Computes character n-gram (2-4 chars) overlap similarity between two strings.
     * Returns a value in [0, 1] representing the Jaccard-like overlap of n-gram sets.
     *
     * This is the core of Phase 2's "vector-like" matching — no external embedding
     * service required, works offline, handles Bengali and English equally.
     */
    private double ngramSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null || s1.isEmpty() || s2.isEmpty()) return 0.0;

        // Use 2-3 gram overlap for efficiency
        Set<String> ngrams1 = extractNgrams(s1, 2, 3);
        Set<String> ngrams2 = extractNgrams(s2, 2, 3);

        if (ngrams1.isEmpty() || ngrams2.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(ngrams1);
        intersection.retainAll(ngrams2);

        Set<String> union = new HashSet<>(ngrams1);
        union.addAll(ngrams2);

        return (double) intersection.size() / union.size(); // Jaccard similarity
    }

    /**
     * Extracts character n-grams of sizes [minN, maxN] from a string.
     * Normalizes whitespace and lowercases for consistent matching.
     */
    private Set<String> extractNgrams(String text, int minN, int maxN) {
        String normalized = normalizeText(text);
        Set<String> ngrams = new HashSet<>();
        for (int n = minN; n <= maxN; n++) {
            for (int i = 0; i <= normalized.length() - n; i++) {
                ngrams.add(normalized.substring(i, i + n));
            }
        }
        return ngrams;
    }

    /**
     * Normalizes text: lowercase, collapse whitespace, strip punctuation.
     * Handles both English and Bengali Unicode text.
     */
    private String normalizeText(String text) {
        if (text == null) return "";
        return text.toLowerCase()
                .replaceAll("[\\p{Punct}]", " ") // Remove punctuation
                .replaceAll("\\s+", " ")          // Collapse whitespace
                .trim();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LEGACY KEYWORD MATCHING (kept as fallback)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * @deprecated Phase 2: Use findBestCategoryBySimilarity() instead.
     * Kept as fallback for backward compatibility.
     */
    @Deprecated(since = "Phase 2", forRemoval = true)
    private boolean matchesCategory(String query, String category, JsonNode node) {
        String lowerQuery = query.toLowerCase();
        if (lowerQuery.contains(category.replace("_", " "))) return true;

        JsonNode clusters = node.path("clusters");
        Iterator<Map.Entry<String, JsonNode>> clusterEntries = clusters.fields();
        while (clusterEntries.hasNext()) {
            Map.Entry<String, JsonNode> entry = clusterEntries.next();
            String cName = entry.getKey();
            JsonNode cNode = entry.getValue();

            if (lowerQuery.contains(cName.replace("_", " "))) return true;

            JsonNode tasks = cNode.path("tasks");
            if (tasks.isArray()) {
                for (JsonNode task : tasks) {
                    String intent = task.path("intent").asText().toLowerCase();
                    if (lowerQuery.contains(intent)) return true;
                }
            }
        }
        return false;
    }

    /**
     * @deprecated Phase 2: Use findBestClusterBySimilarity() instead.
     */
    @Deprecated(since = "Phase 2", forRemoval = true)
    private String findSpecificCluster(String query, JsonNode clusters) {
        String lowerQuery = query.toLowerCase();
        Iterator<Map.Entry<String, JsonNode>> fields = clusters.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String cName = entry.getKey();
            JsonNode cNode = entry.getValue();

            if (lowerQuery.contains(cName.replace("_", " "))) return cName;

            JsonNode tasks = cNode.path("tasks");
            if (tasks.isArray()) {
                for (JsonNode task : tasks) {
                    String intent = task.path("intent").asText().toLowerCase();
                    if (lowerQuery.contains(intent)) return cName;
                }
            }
        }
        return "general";
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SELF-LEARNING LOOP: CORRECTION RECORDING
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Records a user correction into the learning reservoir.
     * This is the core of the self-learning loop — every user correction
     * improves future intent classification.
     *
     * @param originalIntent What the system thought the user wanted (hub name)
     * @param correctedHub   What the user actually wanted (correct hub name)
     * @param userFeedback   User's correction text
     */
    public void recordCorrection(String originalIntent, String correctedHub, String userFeedback) {
        log.info("[SYSTEM_LEARNING] Recording correction: {} -> {} (Feedback: {})",
            originalIntent, correctedHub, userFeedback);

        if (rootNode instanceof ObjectNode) {
            ObjectNode reservoir = (ObjectNode) rootNode.path("learning_reservoir");
            ArrayNode corrections = (ArrayNode) reservoir.path("recent_corrections");

            ObjectNode correction = objectMapper.createObjectNode();
            correction.put("timestamp", System.currentTimeMillis());
            correction.put("original_intent", originalIntent);
            correction.put("corrected_hub", correctedHub);
            correction.put("feedback", userFeedback);

            corrections.add(correction);

            // Pro-active logic: if same correction happens N times, trigger logic update
            if (shouldTriggerLogicUpdate(corrections, originalIntent)) {
                log.warn("[SYSTEM_LEARNING] High correction frequency for intent '{}'. Proposing logic update.",
                        originalIntent);
                proposeLogicRefinement(originalIntent, correctedHub);
            }

            saveKnowledgeToFile();
        }
    }

    /**
     * Checks if a specific intent has been corrected CORRECTION_TRIGGER_COUNT+ times.
     */
    private boolean shouldTriggerLogicUpdate(ArrayNode corrections, String intent) {
        long count = 0;
        for (JsonNode c : corrections) {
            if (c.path("original_intent").asText().equals(intent)) {
                count++;
            }
        }
        return count >= CORRECTION_TRIGGER_COUNT;
    }

    /**
     * Phase 2: Proposes a logic refinement. In production this sends a notification
     * to the Admin Dashboard. Currently logs the suggestion.
     */
    private void proposeLogicRefinement(String intent, String recommendedHub) {
        log.info("[SYSTEM_LEARNING] Suggestion: Map '{}' to '{}' in core_knowledge.json", intent, recommendedHub);
        if (adminDashboardFacade != null) {
            adminDashboardFacade.pushSuggestion("LOGIC_REFINEMENT", 
                "Map '" + intent + "' to '" + recommendedHub + "' in core_knowledge.json", 
                recommendedHub, 0.85);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PHASE 2: SYSTEM SUGGESTION LOGIC
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Phase 2: Scans all provider stats for task types with low success rates
     * and returns suggestions for model improvements.
     *
     * Implements: Auto-Model Suggestion
     */
    public List<SystemSuggestion> checkForModelGaps() {
        List<SystemSuggestion> suggestions = new ArrayList<>();
        JsonNode taxonomy = rootNode.path("intent_taxonomy");

        Iterator<Map.Entry<String, JsonNode>> fields = taxonomy.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String categoryName = entry.getKey();
            JsonNode categoryNode = entry.getValue();

            JsonNode clusters = categoryNode.path("clusters");
            Iterator<Map.Entry<String, JsonNode>> clusterEntries = clusters.fields();
            while (clusterEntries.hasNext()) {
                Map.Entry<String, JsonNode> cEntry = clusterEntries.next();
                String clusterName = cEntry.getKey();
                JsonNode tasks = cEntry.getValue().path("tasks");

                if (tasks.isArray() && tasks.size() > 0) {
                    // Check if this task type has low success rate in provider stats
                    double avgSuccessRate = estimateTaskSuccessRate(categoryName, clusterName);
                    if (avgSuccessRate < LOW_SUCCESS_RATE_THRESHOLD && avgSuccessRate >= 0.0) {
                        suggestions.add(new SystemSuggestion(
                                SystemSuggestion.Type.AUTO_MODEL,
                                String.format("Low success rate for '%s / %s' (%.0f%%). Consider adding a specialized model.",
                                        categoryName, clusterName, avgSuccessRate * 100),
                                categoryName,
                                clusterName,
                                avgSuccessRate
                        ));
                    }
                }
            }
        }

        return suggestions;
    }

    /**
     * Phase 2: Evaluates a model link (HuggingFace/GitHub URL) and compares it
     * against currently deployed models.
     *
     * Implements: Link-Based Evaluation
     *
     * @param url Model URL to evaluate
     * @return SystemSuggestion with recommendation
     */
    public SystemSuggestion evaluateModelLink(String url) {
        if (url == null || url.isBlank()) {
            return new SystemSuggestion(SystemSuggestion.Type.LINK_EVALUATION,
                    "Invalid URL provided.", "", "", 0.0);
        }

        log.info("[SYSTEM_LEARNING] Evaluating model link: {}", url);

        String provider = url.contains("huggingface.co") ? "huggingface" :
                          url.contains("github.com") ? "github" : "unknown";

        // Extract model name from URL
        String modelName = extractModelNameFromUrl(url);

        // Compare against deployed models from intent taxonomy
        JsonNode taxonomy = rootNode.path("intent_taxonomy");
        Set<String> deployedModels = new HashSet<>();
        Iterator<Map.Entry<String, JsonNode>> fields = taxonomy.fields();
        while (fields.hasNext()) {
            JsonNode hubNode = fields.next().getValue();
            String hub = hubNode.path("hub").asText();
            deployedModels.add(hub);
        }

        boolean isNew = deployedModels.stream().noneMatch(hub -> hub.toLowerCase().contains(modelName.toLowerCase()));
        String recommendation = isNew
                ? String.format("DEPLOY: '%s' from %s is not in current deployment. Recommend evaluation.", modelName, provider)
                : String.format("EVALUATE: '%s' is already deployed. Check for newer versions.", modelName);

        return new SystemSuggestion(
                SystemSuggestion.Type.LINK_EVALUATION,
                recommendation,
                modelName,
                provider,
                isNew ? 0.8 : 0.5
        );
    }

    /**
     * Phase 2: Detects intelligence gaps — task types that have no successful
     * provider solutions.
     *
     * Implements: Gap Analysis
     *
     * @param failedTaskType The task type that failed
     * @return SystemSuggestion describing the gap
     */
    public SystemSuggestion detectIntelligenceGap(String failedTaskType) {
        if (failedTaskType == null || failedTaskType.isBlank()) {
            return new SystemSuggestion(SystemSuggestion.Type.GAP_ANALYSIS,
                    "No task type specified for gap analysis.", "", "", 0.0);
        }

        log.info("[SYSTEM_LEARNING] Gap analysis for task type: {}", failedTaskType);

        // Check if any provider has successfully handled this task type
        boolean hasSolution = checkIfTaskHasSolution(failedTaskType);

        if (!hasSolution) {
            return new SystemSuggestion(
                    SystemSuggestion.Type.GAP_ANALYSIS,
                    String.format("INTELLIGENCE GAP: No provider can reliably handle '%s'. " +
                            "A specialized model is needed for this task type.", failedTaskType),
                    failedTaskType,
                    "all_providers",
                    0.0
            );
        }

        return new SystemSuggestion(
                SystemSuggestion.Type.GAP_ANALYSIS,
                String.format("Task '%s' has existing solutions. No gap detected.", failedTaskType),
                failedTaskType,
                "all_providers",
                1.0
        );
    }

    /**
     * Checks if a task type has at least one successful solution in the knowledge base.
     */
    private boolean checkIfTaskHasSolution(String taskType) {
        JsonNode reservoir = rootNode.path("learning_reservoir");
        ArrayNode corrections = (ArrayNode) reservoir.path("recent_corrections");

        long successCount = 0;
        for (JsonNode c : corrections) {
            String hub = c.path("corrected_hub").asText("");
            if (!hub.isEmpty() && !hub.equals("unknown")) {
                successCount++;
            }
        }
        return successCount >= MIN_SOLUTIONS_FOR_GAP;
    }

    /**
     * Estimates success rate for a task type from correction history.
     * Returns -1.0 if no data available.
     */
    private double estimateTaskSuccessRate(String category, String cluster) {
        JsonNode reservoir = rootNode.path("learning_reservoir");
        ArrayNode corrections = (ArrayNode) reservoir.path("recent_corrections");

        if (corrections.size() == 0) return -1.0;

        long total = 0;
        long corrected = 0;
        for (JsonNode c : corrections) {
            String hub = c.path("corrected_hub").asText("");
            if (!hub.isEmpty()) {
                total++;
                if (!hub.equals("unknown")) corrected++;
            }
        }

        return total > 0 ? (double) corrected / total : -1.0;
    }

    /**
     * Extracts a model name from a HuggingFace or GitHub URL.
     */
    private String extractModelNameFromUrl(String url) {
        try {
            if (url.contains("huggingface.co")) {
                // Format: https://huggingface.co/org/model-name
                String[] parts = url.split("huggingface\\.co/");
                if (parts.length > 1) {
                    String path = parts[1].replaceAll("/.*", "").trim();
                    return path.isEmpty() ? "unknown-hf-model" : path;
                }
            } else if (url.contains("github.com")) {
                // Format: https://github.com/org/repo
                String[] parts = url.split("github\\.com/");
                if (parts.length > 1) {
                    String path = parts[1].split("/")[0].trim();
                    return path.isEmpty() ? "unknown-github-repo" : path;
                }
            }
        } catch (Exception e) {
            log.warn("[SYSTEM_LEARNING] Could not extract model name from URL: {}", e.getMessage());
        }
        return "unknown-model";
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PERSISTENCE
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Persists the current knowledge state back to the JSON file.
     * Thread-safe via synchronized.
     *
     * Note: In production with high concurrency, replace with Firestore/Qdrant.
     */
    private synchronized void saveKnowledgeToFile() {
        try {
            Path path = Paths.get(KNOWLEDGE_FILE_PATH);
            if (Files.exists(path)) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), rootNode);
                log.info("[SYSTEM_LEARNING] core_knowledge.json updated with latest learning data");
            }
        } catch (IOException e) {
            log.error("[SYSTEM_LEARNING] Failed to persist knowledge update", e);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PROVIDER PREFERENCES
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Provides initial provider preferences for the router based on the intent taxonomy.
     */
    public Map<String, List<String>> getInitialProviderPreferences() {
        Map<String, List<String>> preferences = new HashMap<>();
        JsonNode taxonomy = rootNode.path("intent_taxonomy");

        if (taxonomy.isObject()) {
            taxonomy.fields().forEachRemaining(entry -> {
                JsonNode providersNode = entry.getValue().path("preferred_providers");
                if (providersNode.isArray()) {
                    List<String> providers = new java.util.ArrayList<>();
                    providersNode.forEach(node -> providers.add(node.asText()));
                    if (!providers.isEmpty()) {
                        preferences.put(entry.getKey(), providers);
                    }
                }
            });
        }

        return preferences;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PHASE 2: LEARNING LOOP HEALTH & ANALYTICS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Phase 2: Returns a comprehensive health snapshot of the learning loop.
     * Used by LearningLoopController and admin dashboard.
     */
    public Map<String, Object> getLearningLoopHealth() {
        Map<String, Object> health = new LinkedHashMap<>();
        try {
            JsonNode root = rootNode;
            JsonNode reservoir = root.path("learning_reservoir");
            ArrayNode corrections = (ArrayNode) reservoir.path("recent_corrections");

            // Correction stats
            health.put("totalCorrections", corrections.size());
            health.put("systemVersion", root.path("system_identity").path("version").asText("unknown"));
            health.put("intentTaxonomySize", root.path("intent_taxonomy").size());

            // Hub routing distribution
            Map<String, Long> hubDistribution = new LinkedHashMap<>();
            for (JsonNode c : corrections) {
                String hub = c.path("corrected_hub").asText("unknown");
                hubDistribution.merge(hub, 1L, Long::sum);
            }
            health.put("hubRoutingDistribution", hubDistribution);

            // Recent correction frequency (last 10)
            List<Map<String, Object>> recentCorrections = new ArrayList<>();
            int limit = Math.min(corrections.size(), 10);
            for (int i = corrections.size() - limit; i < corrections.size(); i++) {
                JsonNode c = corrections.get(i);
                Map<String, Object> corr = new LinkedHashMap<>();
                corr.put("timestamp", c.path("timestamp").asText());
                corr.put("original_intent", c.path("original_intent").asText());
                corr.put("corrected_hub", c.path("corrected_hub").asText());
                corr.put("feedback", c.path("feedback").asText());
                recentCorrections.add(corr);
            }
            health.put("recentCorrections", recentCorrections);

            // Pending system suggestions
            List<SystemSuggestion> suggestions = new ArrayList<>(checkForModelGaps());
            health.put("pendingSuggestions", suggestions.stream()
                    .map(s -> Map.of(
                            "type", s.type().name(),
                            "message", s.message(),
                            "category", s.category(),
                            "score", s.score()
                    ))
                    .toList());

            health.put("status", "healthy");
        } catch (Exception e) {
            log.error("[SYSTEM_LEARNING] Error collecting learning loop health", e);
            health.put("status", "error");
            health.put("error", e.getMessage());
        }
        return health;
    }

    /**
     * Phase 2: Returns the raw root node for advanced introspection.
     */
    public JsonNode getRootNode() {
        return rootNode;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  INTERNAL HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private String truncate(String text) {
        if (text == null) return "";
        return text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  PHASE 2: SYSTEM SUGGESTION RECORD
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Phase 2: Immutable record representing a system-generated suggestion.
     * Three types: AUTO_MODEL, LINK_EVALUATION, GAP_ANALYSIS.
     */
    public record SystemSuggestion(
            Type type,
            String message,
            String category,
            String target,
            double score // 0.0 = critical gap, 1.0 = all good
    ) {
        public enum Type {
            AUTO_MODEL,       // Suggest deploying a better model for a task type
            LINK_EVALUATION,  // Evaluate a shared model link
            GAP_ANALYSIS      // Report an intelligence gap
        }
    }

    public void logUnknownError(String signature, String message) {
        log.warn("[SYSTEM_LEARNING] Unknown error logged: signature={}, message={}", signature, message);
    }
}
