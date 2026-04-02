package org.example.service;

import org.example.model.SystemLearning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.google.firebase.database.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * System Learning Service - SupremeAI's Brain
 * Learns from errors, tracks patterns, prevents future mistakes
 */
@Service
public class SystemLearningService {
    private static final Logger logger = LoggerFactory.getLogger(SystemLearningService.class);
    
    @Autowired(required = false)
    private FirebaseService firebaseService;
    
    private static final String LEARNINGS_PATH = "system/learnings";
    private static final String PATTERNS_PATH = "system/patterns";
    private Map<String, SystemLearning> learningsCache = new ConcurrentHashMap<>();
    
    /**
     * Check if Firebase is available
     */
    private boolean isFirebaseAvailable() {
        if (firebaseService == null || !firebaseService.isInitialized()) {
            logger.warn("⚠️ Firebase not configured - using in-memory learning cache only");
            return false;
        }
        return true;
    }

    private FirebaseDatabase getFirebaseDb() {
        if (!isFirebaseAvailable()) {
            return null;
        }

        try {
            return firebaseService.getDatabase();
        } catch (Exception e) {
            logger.warn("⚠️ Firebase database unavailable - using in-memory learning cache only: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Record an error for learning
     */
    public void recordError(String category, String errorMessage, Exception e, String solution) {
        try {
            String safeCategory = normalizeCategory(category);
            String safeMessage = defaultString(errorMessage, "Unknown error");
            SystemLearning learning = new SystemLearning();
            learning.setType("ERROR");
            learning.setCategory(safeCategory);
            learning.setContent(safeMessage);
            learning.setSeverity(analyzeSeverity(safeMessage));
            learning.setResolved(solution != null && !solution.isBlank());
            learning.setResolution(defaultString(solution, "Pending investigation"));
            learning.setConfidenceScore(0.9);
            addSolutionIfMissing(learning, solution);
            learning.setContext(buildErrorContext(e));
            ensureStableIdentity(learning);
            
            // Check if similar error exists
            SystemLearning existing = findSimilarError(safeCategory, safeMessage);
            if (existing != null) {
                existing.incrementErrorCount();
                existing.setSeverity(higherSeverity(existing.getSeverity(), learning.getSeverity()));
                existing.setResolved(existing.getResolved() || learning.getResolved());
                if (existing.getResolution() == null || existing.getResolution().isBlank()) {
                    existing.setResolution(learning.getResolution());
                }
                existing.setConfidenceScore(Math.max(existing.getConfidenceScore(), learning.getConfidenceScore()));
                mergeContexts(existing, learning.getContext());
                addSolutionIfMissing(existing, solution);
                updateLearning(existing);
                logger.info("📚 Previous {} error found {}: incremented to {}", 
                    safeCategory, existing.getId(), existing.getErrorCount());
            } else {
                learning.incrementErrorCount();
                saveLearning(learning);
                logger.info("📚 New {} error recorded: {}", category, learning.getId());
            }
        } catch (Exception ex) {
            logger.error("❌ Failed to record learning: {}", ex.getMessage());
        }
    }
    
    /**
     * Record a pattern discovered (best practice)
     */
    public void recordPattern(String category, String pattern, String reasoning) {
        try {
            upsertKnowledge(
                "PATTERN",
                category,
                pattern,
                "INFO",
                Arrays.asList(reasoning),
                Map.of("source", "system-pattern"),
                0.8,
                false,
                null
            );
        } catch (Exception e) {
            logger.error("❌ Failed to record pattern: {}", e.getMessage());
        }
    }
    
    /**
     * Record admin requirement (CRITICAL)
     */
    public void recordRequirement(String requirement, String details) {
        try {
            upsertKnowledge(
                "REQUIREMENT",
                "ADMIN",
                requirement,
                "CRITICAL",
                Arrays.asList(details),
                Map.of("source", "admin-requirement"),
                1.0,
                true,
                details
            );
        } catch (Exception e) {
            logger.error("❌ Failed to record requirement: {}", e.getMessage());
        }
    }

    /**
     * Record an operational technique for app creation, debugging, or validation.
     */
    public void recordTechnique(String category, String techniqueName, String summary,
                                List<String> steps, double confidenceScore) {
        recordTechnique(category, techniqueName, summary, steps, confidenceScore, new HashMap<>());
    }

    /**
     * Record an operational technique with extra metadata.
     */
    public void recordTechnique(String category, String techniqueName, String summary,
                                List<String> steps, double confidenceScore,
                                Map<String, Object> context) {
        try {
            Map<String, Object> techniqueContext = new HashMap<>();
            if (context != null) {
                techniqueContext.putAll(context);
            }
            techniqueContext.put("kind", "TECHNIQUE");
            techniqueContext.put("summary", defaultString(summary, ""));
            techniqueContext.put("stepCount", steps == null ? 0 : steps.size());

            upsertKnowledge(
                "PATTERN",
                category,
                techniqueName,
                "HIGH",
                steps,
                techniqueContext,
                confidenceScore,
                true,
                summary
            );
        } catch (Exception e) {
            logger.error("❌ Failed to record technique: {}", e.getMessage());
        }
    }

    /**
     * Record AI model-specific memory so SupremeAI can learn provider-wise strengths.
     */
    public void recordAIModelMemory(String modelName, String question, String response,
                                    double confidenceScore, Map<String, Object> metadata) {
        try {
            String safeModel = defaultString(modelName, "unknown-model").trim().toLowerCase(Locale.ROOT);
            String trimmedQuestion = defaultString(question, "").trim();
            String trimmedResponse = defaultString(response, "").trim();

            Map<String, Object> context = new HashMap<>();
            context.put("kind", "AI_MODEL_MEMORY");
            context.put("model", safeModel);
            context.put("question", trimmedQuestion);
            context.put("responseLength", trimmedResponse.length());
            context.put("timestamp", System.currentTimeMillis());
            if (metadata != null) {
                context.putAll(metadata);
            }

            String content = String.format("[%s] %s", safeModel, abbreviate(trimmedQuestion, 240));
            upsertKnowledge(
                "PATTERN",
                "AI_MODEL_MEMORY",
                content,
                "HIGH",
                Arrays.asList(abbreviate(trimmedResponse, 1200)),
                context,
                confidenceScore,
                true,
                "Recorded model-wise memory for future routing and error solving"
            );
        } catch (Exception e) {
            logger.error("❌ Failed to record AI model memory: {}", e.getMessage());
        }
    }

    /**
     * Fetch memories for a specific AI model.
     */
    public List<SystemLearning> getMemoriesByAIModel(String modelName) {
        String safeModel = defaultString(modelName, "").trim().toLowerCase(Locale.ROOT);
        if (safeModel.isBlank()) {
            return new ArrayList<>();
        }

        return learningsCache.values().stream()
            .filter(learning -> "AI_MODEL_MEMORY".equals(normalizeCategory(learning.getCategory())))
            .filter(learning -> learning.getContext() != null)
            .filter(learning -> safeModel.equals(String.valueOf(learning.getContext().getOrDefault("model", "")).toLowerCase(Locale.ROOT)))
            .sorted(Comparator.comparingLong(SystemLearning::getTimestamp).reversed())
            .collect(Collectors.toList());
    }
    
    /**
     * Get solutions for a category
     */
    public List<String> getSolutionsFor(String category) {
        String safeCategory = normalizeCategory(category);
        return learningsCache.values().stream()
            .filter(learning -> safeCategory.equals(normalizeCategory(learning.getCategory())))
            .filter(learning -> learning.getSolutions() != null)
            .flatMap(learning -> learning.getSolutions().stream())
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    }
    
    /**
     * Get all critical requirements
     */
    public List<SystemLearning> getCriticalRequirements() {
        return learningsCache.values().stream()
            .filter(learning -> "CRITICAL".equalsIgnoreCase(defaultString(learning.getSeverity(), "")))
            .sorted(Comparator.comparingLong(SystemLearning::getTimestamp).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Get operational techniques, optionally filtered by category.
     */
    public List<SystemLearning> getTechniques(String category) {
        String normalizedCategory = category == null || category.isBlank()
            ? null
            : normalizeCategory(category);

        return learningsCache.values().stream()
            .filter(this::isTechnique)
            .filter(learning -> normalizedCategory == null
                || normalizedCategory.equals(normalizeCategory(learning.getCategory())))
            .sorted(Comparator.comparingDouble((SystemLearning learning) ->
                learning.getConfidenceScore() == null ? 0.0 : learning.getConfidenceScore()).reversed()
                .thenComparing(SystemLearning::getTimestamp, Comparator.reverseOrder()))
            .collect(Collectors.toList());
    }
    
    /**
     * Check if error should have been prevented
     */
    public Boolean shouldHaveBeenPrevented(String category, String errorMessage) {
        SystemLearning similar = findSimilarError(category, errorMessage);
        
        if (similar == null) {
            return false;
        }
        
        // If we've seen this error 3+ times, we should have prevented it
        return similar.getErrorCount() >= 3;
    }
    
    /**
     * Get learning statistics
     */
    public Map<String, Object> getLearningStats() {
        Map<String, Object> stats = new HashMap<>();

        Collection<SystemLearning> learnings = learningsCache.values();
        int totalLearnings = learnings.size();
        long errorsResolved = learnings.stream()
            .filter(learning -> "ERROR".equals(learning.getType()) && Boolean.TRUE.equals(learning.getResolved()))
            .count();
        long patternsFound = learnings.stream()
            .filter(learning -> "PATTERN".equals(learning.getType()))
            .count();
        long requirements = learnings.stream()
            .filter(learning -> "REQUIREMENT".equals(learning.getType()))
            .count();
        long techniques = learnings.stream()
            .filter(this::isTechnique)
            .count();

        Map<String, Long> byCategory = learnings.stream()
            .collect(Collectors.groupingBy(
                learning -> normalizeCategory(learning.getCategory()),
                Collectors.counting()
            ));

        double averageConfidence = learnings.stream()
            .map(SystemLearning::getConfidenceScore)
            .filter(Objects::nonNull)
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);

        stats.put("status", isFirebaseAvailable() ? "active" : "memory-only");
        stats.put("totalLearnings", totalLearnings);
        stats.put("errorsResolved", errorsResolved);
        stats.put("patternsFound", patternsFound);
        stats.put("requirements", requirements);
        stats.put("techniques", techniques);
        stats.put("byCategory", byCategory);
        stats.put("averageConfidence", averageConfidence);
        stats.put("timestamp", System.currentTimeMillis());
        return stats;
    }
    
    // ========== PRIVATE HELPERS ==========
    
    private void saveLearning(SystemLearning learning) throws Exception {
        ensureStableIdentity(learning);
        learningsCache.put(learning.getId(), learning);
        FirebaseDatabase firebaseDb = getFirebaseDb();
        if (firebaseDb == null) {
            logger.debug("⚠️ Firebase unavailable, storing in memory cache only");
            return;
        }
        DatabaseReference ref = firebaseDb.getReference(LEARNINGS_PATH + "/" + learning.getId());
        ref.setValueAsync(learning);
    }
    
    private void updateLearning(SystemLearning learning) throws Exception {
        ensureStableIdentity(learning);
        learningsCache.put(learning.getId(), learning);
        FirebaseDatabase firebaseDb = getFirebaseDb();
        if (firebaseDb == null) {
            logger.debug("⚠️ Firebase unavailable, updating memory cache only");
            return;
        }
        DatabaseReference ref = firebaseDb.getReference(LEARNINGS_PATH + "/" + learning.getId());
        ref.setValueAsync(learning);
    }
    
    private SystemLearning findSimilarError(String category, String errorMessage) {
        return learningsCache.values().stream()
            .filter(l -> "ERROR".equals(l.getType()))
            .filter(l -> normalizeCategory(category).equals(normalizeCategory(l.getCategory())))
            .filter(l -> normalizedContains(errorMessage, l.getContent()) || normalizedContains(l.getContent(), errorMessage))
            .findFirst()
            .orElse(null);
    }
    
    private String analyzeSeverity(String errorMessage) {
        String normalized = defaultString(errorMessage, "").toLowerCase(Locale.ROOT);
        if (normalized.contains("security") || normalized.contains("injection")) {
            return "CRITICAL";
        }
        if (normalized.contains("null") || normalized.contains("compilation") || normalized.contains("failed")) {
            return "HIGH";
        }
        return "MEDIUM";
    }
    
    private Map<String, Object> buildErrorContext(Exception e) {
        Map<String, Object> context = new HashMap<>();
        context.put("timestamp", System.currentTimeMillis());

        if (e == null) {
            context.put("exceptionClass", "unknown");
            context.put("message", "No exception object provided");
            return context;
        }

        context.put("exceptionClass", e.getClass().getName());
        context.put("message", e.getMessage());
        if (e.getStackTrace() != null && e.getStackTrace().length > 0) {
            StackTraceElement first = e.getStackTrace()[0];
            context.put("location", first.getClassName() + ":" + first.getLineNumber());
        }
        
        return context;
    }

    private void upsertKnowledge(String type, String category, String content, String severity,
                                 List<String> solutions, Map<String, Object> context,
                                 double confidenceScore, boolean resolved, String resolution) throws Exception {
        String safeType = defaultString(type, "PATTERN");
        String safeCategory = normalizeCategory(category);
        String safeContent = defaultString(content, "Unnamed learning");

        SystemLearning existing = findExactLearning(safeType, safeCategory, safeContent);
        if (existing != null) {
            existing.setSeverity(higherSeverity(existing.getSeverity(), severity));
            existing.setConfidenceScore(Math.max(existing.getConfidenceScore(), confidenceScore));
            existing.setResolved(existing.getResolved() || resolved);
            if (resolution != null && !resolution.isBlank()) {
                existing.setResolution(resolution);
            }
            if (solutions != null) {
                for (String solution : solutions) {
                    addSolutionIfMissing(existing, solution);
                }
            }
            mergeContexts(existing, context);
            updateLearning(existing);
            logger.info("🧠 Learning updated {}: {}", safeCategory, existing.getId());
            return;
        }

        SystemLearning learning = new SystemLearning();
        learning.setType(safeType);
        learning.setCategory(safeCategory);
        learning.setContent(safeContent);
        learning.setSeverity(defaultString(severity, "INFO"));
        learning.setConfidenceScore(confidenceScore);
        learning.setResolved(resolved);
        learning.setResolution(resolution);
        if (solutions != null) {
            for (String solution : solutions) {
                addSolutionIfMissing(learning, solution);
            }
        }
        if (context != null) {
            learning.setContext(new HashMap<>(context));
        }

        saveLearning(learning);
        logger.info("🧠 Learning recorded {}: {}", safeCategory, learning.getId());
    }

    private SystemLearning findExactLearning(String type, String category, String content) {
        return learningsCache.values().stream()
            .filter(learning -> defaultString(type, "").equalsIgnoreCase(defaultString(learning.getType(), "")))
            .filter(learning -> normalizeCategory(category).equals(normalizeCategory(learning.getCategory())))
            .filter(learning -> normalizeText(content).equals(normalizeText(learning.getContent())))
            .findFirst()
            .orElse(null);
    }

    private void ensureStableIdentity(SystemLearning learning) {
        learning.setId(generateLearningId(learning.getType(), learning.getCategory(), learning.getContent()));
    }

    private String generateLearningId(String type, String category, String content) {
        String fingerprint = String.join("|",
            defaultString(type, "UNKNOWN"),
            normalizeCategory(category),
            normalizeText(content)
        );
        return UUID.nameUUIDFromBytes(fingerprint.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private void addSolutionIfMissing(SystemLearning learning, String solution) {
        if (solution == null || solution.isBlank()) {
            return;
        }
        if (learning.getSolutions() == null) {
            learning.setSolutions(new ArrayList<>());
        }
        boolean exists = learning.getSolutions().stream()
            .filter(Objects::nonNull)
            .anyMatch(existing -> normalizeText(existing).equals(normalizeText(solution)));
        if (!exists) {
            learning.addSolution(solution);
        }
    }

    private void mergeContexts(SystemLearning learning, Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return;
        }
        if (learning.getContext() == null) {
            learning.setContext(new HashMap<>());
        }
        learning.getContext().putAll(context);
    }

    private boolean normalizedContains(String left, String right) {
        return normalizeText(left).contains(normalizeText(right));
    }

    private String normalizeCategory(String category) {
        return defaultString(category, "GENERAL").trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String text) {
        return defaultString(text, "")
            .trim()
            .replaceAll("\\s+", " ")
            .toLowerCase(Locale.ROOT);
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String abbreviate(String text, int maxLen) {
        String normalized = defaultString(text, "");
        if (normalized.length() <= maxLen) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxLen - 3)) + "...";
    }

    private boolean isTechnique(SystemLearning learning) {
        return learning.getContext() != null && "TECHNIQUE".equals(learning.getContext().get("kind"));
    }

    private String higherSeverity(String current, String candidate) {
        List<String> order = Arrays.asList("LOW", "MEDIUM", "HIGH", "CRITICAL");
        String safeCurrent = defaultString(current, "LOW").toUpperCase(Locale.ROOT);
        String safeCandidate = defaultString(candidate, "LOW").toUpperCase(Locale.ROOT);
        return order.indexOf(safeCandidate) > order.indexOf(safeCurrent) ? safeCandidate : safeCurrent;
    }
}
