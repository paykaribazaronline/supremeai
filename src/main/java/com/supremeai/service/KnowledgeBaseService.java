package com.supremeai.service;

import com.supremeai.model.KnowledgeEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);
    private final Map<String, KnowledgeEntry> knowledgeStore = new ConcurrentHashMap<>();
    
    // Store embeddings for True Vector Search
    private final Map<String, double[]> embeddingStore = new ConcurrentHashMap<>();

    public void learn(String topic, String pattern, String solution, String provider, double score) {
        String id = UUID.randomUUID().toString();
        KnowledgeEntry entry = KnowledgeEntry.builder()
                .id(id)
                .topic(topic)
                .pattern(pattern)
                .solution(solution)
                .sourceProvider(provider)
                .confidenceScore(score)
                .createdAt(LocalDateTime.now())
                .build();

        knowledgeStore.put(id, entry);
        
        // Generate and store embedding for the content
        double[] embedding = generateEmbedding(topic + " " + pattern + " " + solution);
        embeddingStore.put(id, embedding);
        
        log.info("SupremeAI Learned: [{}] from provider: {}", topic, provider);
    }

    public List<KnowledgeEntry> searchKnowledge(String query) {
        // TRUE VECTOR SEARCH IMPLEMENTATION
        double[] queryEmbedding = generateEmbedding(query);

        return knowledgeStore.values().stream()
                .map(entry -> {
                    double[] entryEmbedding = embeddingStore.get(entry.getId());
                    double similarity = cosineSimilarity(queryEmbedding, entryEmbedding);
                    return new AbstractMap.SimpleEntry<>(entry, similarity);
                })
                .filter(e -> e.getValue() > 0.70) // Vector Cosine Similarity Threshold
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue())) // Sort by highest match
                .map(Map.Entry::getKey)
                .limit(10) // Return top 10 most semantically relevant results
                .toList();
    }

    /**
     * MOCK EMBEDDING GENERATOR
     * In production, replace this with an API call to an embedding model 
     * (e.g., OpenAI text-embedding-3-small, Gemini embedding-001, or local ONNX model).
     */
    private double[] generateEmbedding(String text) {
        double[] vec = new double[256];
        if (text != null) {
            for (char c : text.toLowerCase().toCharArray()) {
                if (c < 256) vec[c]++;
            }
        }
        // Normalize vector
        double norm = 0;
        for (double v : vec) norm += v * v;
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vec.length; i++) vec[i] /= norm;
        }
        return vec;
    }

    private double cosineSimilarity(double[] vecA, double[] vecB) {
        if (vecA == null || vecB == null || vecA.length != vecB.length) return 0.0;
        double dotProduct = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < vecA.length; i++) {
            dotProduct += vecA[i] * vecB[i];
            normA += Math.pow(vecA[i], 2);
            normB += Math.pow(vecB[i], 2);
        }
        if (normA == 0 || normB == 0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public List<KnowledgeEntry> getAllKnowledge() {
        return new ArrayList<>(knowledgeStore.values());
    }
}
