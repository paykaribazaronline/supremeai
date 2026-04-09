package org.example.ml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * FIXED: Lightweight Vector Database for Semantic Learning
 *
 * Issue #10: Phase 5 ML was placeholder with hardcoded features
 * Solution: Real semantic vector DB with:
 * - Text-to-vector embedding (using simple hash-based approach)
 * - Similarity search with cosine distance
 * - Self-learning from solution patterns
 * - Firebase export ready
 *
 * Why lightweight approach:
 * - No external dependencies (no external API calls for embeddings)
 * - Scales locally first, can sync to Firebase
 * - Start simple, add real embeddings (sentence-transformers) later
 * - Zero cost, open-source
 *
 * Use Cases:
 * 1. Error pattern matching: "Have we seen this error before?"
 * 2. Solution retrieval: "What fixed similar errors?"
 * 3. Duplicate detection: "Is this a known issue?"
 * 4. Learning across time: "System gets smarter each day"
 */
public class SemanticVectorDatabase {

    private static final Logger logger = LoggerFactory.getLogger(SemanticVectorDatabase.class);

    // Vector storage: category -> list of vectors
    private final Map<String, List<VectorEntry>> vectorStore = new ConcurrentHashMap<>();

    // Metadata for each vector
    private final Map<String, VectorMetadata> metadata = new ConcurrentHashMap<>();

    // Simple word embeddings cache
    private final Map<String, double[]> embeddingCache = new ConcurrentHashMap<>();

    // Statistics
    private long totalInserts = 0;
    private long totalSearches = 0;

    /**
     * Insert a new error/solution pair with semantic vector
     */
    public String insertSolution(String category, String errorText, String solutionText) {
        String vectorId = UUID.randomUUID().toString().substring(0, 12);

        try {
            // Generate embedding by combining error and solution texts
            double[] errorVector = textToVector(errorText);
            double[] solutionVector = textToVector(solutionText);

            // Combined vector = average of both
            double[] combinedVector = averageVectors(errorVector, solutionVector);

            // Normalize
            combinedVector = normalizeVector(combinedVector);

            // Store vector
            VectorEntry entry = new VectorEntry(vectorId, errorText, solutionText, combinedVector);
            vectorStore.computeIfAbsent(category, k -> new ArrayList<>()).add(entry);

            // Store metadata
            metadata.put(vectorId, new VectorMetadata(
                vectorId, category, errorText, solutionText,
                System.currentTimeMillis(), 1.0
            ));

            totalInserts++;

            logger.debug("📝 Inserted solution vector {} for category {}", vectorId, category);
            return vectorId;
        } catch (Exception e) {
            logger.error("❌ Failed to insert solution: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Find similar solutions by semantic similarity
     */
    public List<SimilarityResult> findSimilarSolutions(String errorText, String category, double minSimilarity) {
        totalSearches++;

        double[] queryVector = textToVector(errorText);
        double[] normalizedQueryVector = normalizeVector(queryVector);

        List<VectorEntry> categoryVectors = vectorStore.getOrDefault(category, new ArrayList<>());

        List<SimilarityResult> results = categoryVectors.stream()
            .map(entry -> {
                double similarity = cosineSimilarity(normalizedQueryVector, entry.vector);
                return new SimilarityResult(
                    entry.vectorId,
                    entry.errorText,
                    entry.solutionText,
                    similarity
                );
            })
            .filter(r -> r.similarity >= minSimilarity)
            .sorted(Comparator.comparingDouble((SimilarityResult r) -> r.similarity).reversed())
            .collect(Collectors.toList());

        logger.debug("🔍 Found {} similar solutions (similarity >= {:.2f})", 
            results.size(), minSimilarity);

        return results;
    }

    /**
     * Get most used solutions (ranked by frequency)
     */
    public List<FrequentSolution> getMostFrequentSolutions(String category, int limit) {
        List<VectorEntry> categoryVectors = vectorStore.getOrDefault(category, new ArrayList<>());

        return categoryVectors.stream()
            .map(entry -> {
                VectorMetadata meta = metadata.get(entry.vectorId);
                return new FrequentSolution(
                    entry.solutionText,
                    meta.usageCount,
                    meta.quality
                );
            })
            .sorted(Comparator.comparingInt((FrequentSolution s) -> s.usageCount).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Mark a solution as effective (increases quality score)
     */
    public void markSolutionEffective(String vectorId) {
        VectorMetadata meta = metadata.get(vectorId);
        if (meta != null) {
            meta.usageCount++;
            meta.quality = Math.min(1.0, meta.quality + 0.05);
            logger.debug("✅ Marked solution {} as effective (quality: {:.2f})", vectorId, meta.quality);
        }
    }

    /**
     * Export all vectors to Firebase format
     */
    public Map<String, Object> exportForFirebase() {
        Map<String, Object> export = new HashMap<>();

        Map<String, List<Object>> vectors = new HashMap<>();
        for (String category : vectorStore.keySet()) {
            List<Object> categoryVectors = vectorStore.get(category).stream()
                .map(entry -> Map.of(
                    "id", entry.vectorId,
                    "error", entry.errorText,
                    "solution", entry.solutionText,
                    "dimensions", entry.vector.length,
                    "timestamp", metadata.get(entry.vectorId).timestamp
                ))
                .collect(Collectors.toList());
            vectors.put(category, categoryVectors);
        }

        export.put("vectors", vectors);
        export.put("stats", Map.of(
            "total_vectors", metadata.size(),
            "total_categories", vectorStore.size(),
            "total_inserts", totalInserts,
            "total_searches", totalSearches
        ));

        return export;
    }

    /**
     * Get database statistics
     */
    public Map<String, Object> getStats() {
        return Map.of(
            "total_vectors", metadata.size(),
            "total_categories", vectorStore.size(),
            "total_inserts", totalInserts,
            "total_searches", totalSearches,
            "categories", vectorStore.keySet(),
            "avg_vectors_per_category", 
                metadata.size() / Math.max(1, vectorStore.size())
        );
    }

    // ============== VECTOR OPERATIONS ==============

    /**
     * Convert text to vector using simple hash-based embedding
     * Real production would use sentence-transformers or OpenAI embeddings
     */
    private double[] textToVector(String text) {
        if (text == null || text.isEmpty()) {
            return new double[128]; // Zero vector
        }

        String key = text.toLowerCase().trim();
        if (embeddingCache.containsKey(key)) {
            return embeddingCache.get(key);
        }

        // Simple embedding: hash-based word representation
        double[] vector = new double[128];

        // Tokenize
        String[] words = text.toLowerCase().split("[^a-z0-9]+");

        for (String word : words) {
            if (word.length() > 0) {
                // Hash word to get indices
                int hash1 = word.hashCode();
                int hash2 = 31 * hash1;

                for (int i = 0; i < 4; i++) {
                    int index = Math.abs((hash1 + i * hash2) % vector.length);
                    vector[index] += 1.0 / (word.length() + 1);
                }
            }
        }

        embeddingCache.put(key, vector);
        return vector;
    }

    /**
     * Calculate cosine similarity between two vectors
     */
    private double cosineSimilarity(double[] v1, double[] v2) {
        if (v1.length != v2.length || v1.length == 0) {
            return 0;
        }

        double dotProduct = 0;
        double norm1 = 0;
        double norm2 = 0;

        for (int i = 0; i < v1.length; i++) {
            dotProduct += v1[i] * v2[i];
            norm1 += v1[i] * v1[i];
            norm2 += v2[i] * v2[i];
        }

        double denominator = Math.sqrt(norm1) * Math.sqrt(norm2);
        if (denominator == 0) {
            return 0;
        }

        return dotProduct / denominator;
    }

    /**
     * Normalize vector to unit length
     */
    private double[] normalizeVector(double[] v) {
        double norm = Math.sqrt(Arrays.stream(v).map(x -> x * x).sum());
        if (norm == 0) return v;

        return Arrays.stream(v).map(x -> x / norm).toArray();
    }

    /**
     * Average two vectors
     */
    private double[] averageVectors(double[] v1, double[] v2) {
        int len = Math.max(v1.length, v2.length);
        double[] result = new double[len];

        for (int i = 0; i < v1.length; i++) {
            result[i] += v1[i];
        }
        for (int i = 0; i < v2.length; i++) {
            result[i] += v2[i];
        }

        for (int i = 0; i < len; i++) {
            result[i] /= 2.0;
        }

        return result;
    }

    // ============== DATA CLASSES ==============

    public static class VectorEntry {
        public final String vectorId;
        public final String errorText;
        public final String solutionText;
        public final double[] vector;

        public VectorEntry(String vectorId, String errorText, String solutionText, double[] vector) {
            this.vectorId = vectorId;
            this.errorText = errorText;
            this.solutionText = solutionText;
            this.vector = vector;
        }
    }

    public static class VectorMetadata {
        public final String vectorId;
        public final String category;
        public final String errorText;
        public final String solutionText;
        public final long timestamp;
        public int usageCount;
        public double quality;

        public VectorMetadata(String vectorId, String category, String errorText, 
                             String solutionText, long timestamp, double quality) {
            this.vectorId = vectorId;
            this.category = category;
            this.errorText = errorText;
            this.solutionText = solutionText;
            this.timestamp = timestamp;
            this.usageCount = 1;
            this.quality = quality;
        }
    }

    public static class SimilarityResult {
        public final String vectorId;
        public final String errorText;
        public final String solutionText;
        public final double similarity;

        public SimilarityResult(String vectorId, String errorText, String solutionText, double similarity) {
            this.vectorId = vectorId;
            this.errorText = errorText;
            this.solutionText = solutionText;
            this.similarity = similarity;
        }
    }

    public static class FrequentSolution {
        public final String solutionText;
        public final int usageCount;
        public final double quality;

        public FrequentSolution(String solutionText, int usageCount, double quality) {
            this.solutionText = solutionText;
            this.usageCount = usageCount;
            this.quality = quality;
        }
    }
}
