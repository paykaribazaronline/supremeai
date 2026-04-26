package com.supremeai.learning;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.supremeai.learning.knowledge.GlobalKnowledgeBase;
import com.supremeai.model.SystemLearning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * Service to learn from user-edited code and store patterns in the knowledge base.
 * This enables SupremeAI to improve over time based on user corrections and improvements.
 */
@Service
public class UserCodeLearningService {

    private static final Logger log = LoggerFactory.getLogger(UserCodeLearningService.class);

    @Autowired(required = false)
    private Firestore firestore;

    @Autowired
    private GlobalKnowledgeBase globalKnowledgeBase;

    @Value("${firestore.collection.system-learning:system_learning}")
    private String systemLearningCollection;

    // In-memory cache for frequently accessed patterns
    private final Map<String, SystemLearning> patternCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (firestore == null) {
            log.warn("Firestore not available, UserCodeLearningService will use in-memory storage only");
        } else {
            loadExistingPatterns();
        }
    }

    /**
     * Load existing learning patterns from Firestore
     */
    private void loadExistingPatterns() {
        try {
            log.info("Loading existing learning patterns from Firestore collection: {}", systemLearningCollection);
            ApiFuture<QuerySnapshot> future = firestore.collection(systemLearningCollection).get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            int loadedCount = 0;
            for (QueryDocumentSnapshot doc : documents) {
                try {
                    SystemLearning pattern = docToSystemLearning(doc);
                    if (pattern != null) {
                        patternCache.put(pattern.getId(), pattern);
                        loadedCount++;
                    }
                } catch (Exception e) {
                    log.error("Error processing document: {}", e.getMessage());
                }
            }

            log.info("Loaded {} learning patterns from Firestore", loadedCount);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to load learning patterns from Firestore: {}", e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Unexpected error loading learning patterns: {}", e.getMessage());
        }
    }

    /**
     * Learn from user-edited code by comparing it with the original AI-generated code
     * 
     * @param taskId The task ID for which code was generated
     * @param originalCode The original AI-generated code
     * @param editedCode The user-edited code
     * @param context Additional context about the task
     */
    public void learnFromUserEdit(String taskId, String originalCode, String editedCode, String context) {
        if (originalCode.equals(editedCode)) {
            log.debug("No changes detected in user edit for task: {}", taskId);
            return;
        }

        try {
            // Analyze differences between original and edited code
            CodeDiffAnalysis diffAnalysis = analyzeCodeDiff(originalCode, editedCode);

            // Create a learning pattern from the differences
            SystemLearning pattern = createLearningPattern(taskId, diffAnalysis, context);

            // Save to knowledge base
            saveLearningPattern(pattern);

            log.info("Successfully learned from user edit for task: {}", taskId);
        } catch (Exception e) {
            log.error("Failed to learn from user edit for task {}: {}", taskId, e.getMessage());
        }
    }

    /**
     * Analyze differences between original and edited code
     */
    private CodeDiffAnalysis analyzeCodeDiff(String originalCode, String editedCode) {
        CodeDiffAnalysis analysis = new CodeDiffAnalysis();

        // Simple line-by-line diff (can be enhanced with more sophisticated diff algorithms)
        String[] originalLines = originalCode.split("\n");
        String[] editedLines = editedCode.split("\n");

        // Find added lines
        Set<String> addedLines = new HashSet<>(Arrays.asList(editedLines));
        addedLines.removeAll(Arrays.asList(originalLines));
        analysis.setAddedLines(new ArrayList<>(addedLines));

        // Find removed lines
        Set<String> removedLines = new HashSet<>(Arrays.asList(originalLines));
        removedLines.removeAll(Arrays.asList(editedLines));
        analysis.setRemovedLines(new ArrayList<>(removedLines));

        // Calculate similarity percentage
        int maxLines = Math.max(originalLines.length, editedLines.length);
        int commonLines = originalLines.length + editedLines.length - addedLines.size() - removedLines.size();
        double similarity = maxLines > 0 ? (double) commonLines / maxLines * 100 : 0;
        analysis.setSimilarityPercentage(similarity);

        return analysis;
    }

    /**
     * Create a learning pattern from the code diff analysis
     */
    private SystemLearning createLearningPattern(String taskId, CodeDiffAnalysis diffAnalysis, String context) {
        SystemLearning pattern = new SystemLearning();

        // Generate a unique ID for this pattern
        String patternId = "pattern_" + taskId + "_" + System.currentTimeMillis();
        pattern.setId(patternId);

        // Set pattern type based on the nature of changes
        if (diffAnalysis.getSimilarityPercentage() > 80) {
            pattern.setLearningType("MINOR_CORRECTION");
        } else if (diffAnalysis.getSimilarityPercentage() > 50) {
            pattern.setLearningType("MODIFICATION");
        } else {
            pattern.setLearningType("MAJOR_REFACTOR");
        }

        // Set category based on context
        pattern.setCategory(determineCategory(context, diffAnalysis));

        // Set content as a summary of changes
        StringBuilder content = new StringBuilder();
        content.append("User made ").append(diffAnalysis.getAddedLines().size())
              .append(" additions and ").append(diffAnalysis.getRemovedLines().size())
              .append(" removals. ");

        if (!diffAnalysis.getAddedLines().isEmpty()) {
            content.append("Key additions: ")
                  .append(String.join(", ", diffAnalysis.getAddedLines().subList(0, 
                          Math.min(3, diffAnalysis.getAddedLines().size()))));
        }

        pattern.setContent(content.toString());

        // Set learnedAt
        pattern.setLearnedAt(java.time.LocalDateTime.now());

        // Set metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("taskId", taskId);
        metadata.put("similarity", diffAnalysis.getSimilarityPercentage());
        metadata.put("context", context);
        pattern.setMetadata(metadata);

        return pattern;
    }

    /**
     * Determine the category of the learning pattern based on context and diff analysis
     */
    private String determineCategory(String context, CodeDiffAnalysis diffAnalysis) {
        if (context == null || context.isEmpty()) {
            return "GENERAL";
        }

        String lowerContext = context.toLowerCase();

        if (lowerContext.contains("error") || lowerContext.contains("fix") || lowerContext.contains("bug")) {
            return "BUG_FIX";
        } else if (lowerContext.contains("security") || lowerContext.contains("auth") || lowerContext.contains("permission")) {
            return "SECURITY";
        } else if (lowerContext.contains("performance") || lowerContext.contains("optimize") || lowerContext.contains("speed")) {
            return "PERFORMANCE";
        } else if (lowerContext.contains("database") || lowerContext.contains("query") || lowerContext.contains("sql")) {
            return "DATABASE";
        } else if (lowerContext.contains("api") || lowerContext.contains("endpoint") || lowerContext.contains("controller")) {
            return "API";
        } else if (lowerContext.contains("ui") || lowerContext.contains("frontend") || lowerContext.contains("component")) {
            return "UI_UX";
        } else {
            return "GENERAL";
        }
    }

    /**
     * Save a learning pattern to Firestore and local cache
     */
    private void saveLearningPattern(SystemLearning pattern) {
        // Save to local cache
        patternCache.put(pattern.getId(), pattern);

        // Save to Firestore if available
        if (firestore == null) {
            log.warn("Firestore not available, pattern saved to in-memory cache only");
            return;
        }

        try {
            Map<String, Object> patternData = systemLearningToMap(pattern);
            firestore.collection(systemLearningCollection).document(pattern.getId()).set(patternData);
            log.debug("Saved learning pattern to Firestore: {}", pattern.getId());
        } catch (Exception e) {
            log.error("Failed to save learning pattern to Firestore: {}", e.getMessage());
        }
    }

    /**
     * Get a learning pattern by ID
     */
    public SystemLearning getPattern(String patternId) {
        // First check local cache
        SystemLearning pattern = patternCache.get(patternId);
        if (pattern != null) {
            return pattern;
        }

        // If not in cache and Firestore is available, try to fetch from Firestore
        if (firestore != null) {
            try {
                DocumentSnapshot doc = firestore.collection(systemLearningCollection).document(patternId).get().get();
                if (doc.exists()) {
                    pattern = docToSystemLearning(doc);
                    if (pattern != null) {
                        patternCache.put(patternId, pattern);
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                log.error("Failed to fetch pattern from Firestore: {}", e.getMessage());
                Thread.currentThread().interrupt();
            }
        }

        return pattern;
    }

    /**
     * Get all patterns of a specific category
     */
    public List<SystemLearning> getPatternsByCategory(String category) {
        List<SystemLearning> result = new ArrayList<>();

        // First check local cache
        for (SystemLearning pattern : patternCache.values()) {
            if (category.equals(pattern.getCategory())) {
                result.add(pattern);
            }
        }

        // If Firestore is available, also check there
        if (firestore != null) {
            try {
                ApiFuture<QuerySnapshot> future = firestore.collection(systemLearningCollection)
                        .whereEqualTo("category", category)
                        .get();
                List<QueryDocumentSnapshot> documents = future.get().getDocuments();

                for (QueryDocumentSnapshot doc : documents) {
                    SystemLearning pattern = docToSystemLearning(doc);
                    if (pattern != null && !patternCache.containsKey(pattern.getId())) {
                        result.add(pattern);
                        patternCache.put(pattern.getId(), pattern);
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                log.error("Failed to fetch patterns by category from Firestore: {}", e.getMessage());
                Thread.currentThread().interrupt();
            }
        }

        return result;
    }

    /**
     * Convert SystemLearning to Map for Firestore storage
     */
    private Map<String, Object> systemLearningToMap(SystemLearning pattern) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", pattern.getId());
        map.put("type", pattern.getLearningType());
        map.put("category", pattern.getCategory());
        map.put("content", pattern.getContent());
        map.put("timestamp", pattern.getLearnedAt() != null ? pattern.getLearnedAt().toString() : "");
        map.put("metadata", pattern.getMetadata() != null ? pattern.getMetadata() : new HashMap<>());
        return map;
    }

    /**
     * Convert Firestore DocumentSnapshot to SystemLearning
     */
    private SystemLearning docToSystemLearning(DocumentSnapshot doc) {
        try {
            SystemLearning pattern = new SystemLearning();
            pattern.setId(doc.getString("id"));
            pattern.setLearningType(doc.getString("type"));
            pattern.setCategory(doc.getString("category"));
            pattern.setContent(doc.getString("content"));
            String ts = doc.getString("timestamp");
            if (ts != null && !ts.isEmpty()) {
                try {
                    pattern.setLearnedAt(java.time.LocalDateTime.parse(ts));
                } catch (Exception e) {
                    pattern.setLearnedAt(java.time.LocalDateTime.now());
                }
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) doc.get("metadata");
            pattern.setMetadata(metadata != null ? metadata : new HashMap<>());

            return pattern;
        } catch (Exception e) {
            log.error("Error converting document to SystemLearning: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Inner class to represent code diff analysis
     */
    private static class CodeDiffAnalysis {
        private List<String> addedLines = new ArrayList<>();
        private List<String> removedLines = new ArrayList<>();
        private double similarityPercentage = 0.0;

        public List<String> getAddedLines() {
            return addedLines;
        }

        public void setAddedLines(List<String> addedLines) {
            this.addedLines = addedLines;
        }

        public List<String> getRemovedLines() {
            return removedLines;
        }

        public void setRemovedLines(List<String> removedLines) {
            this.removedLines = removedLines;
        }

        public double getSimilarityPercentage() {
            return similarityPercentage;
        }

        public void setSimilarityPercentage(double similarityPercentage) {
            this.similarityPercentage = similarityPercentage;
        }
    }
}
