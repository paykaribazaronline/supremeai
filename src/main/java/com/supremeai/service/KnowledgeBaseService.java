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
        log.info("SupremeAI Learned: [{}] from provider: {}", topic, provider);
    }

    public List<KnowledgeEntry> searchKnowledge(String query) {
        // Basic search logic - in reality, this would use Firestore vector search or similar
        return knowledgeStore.values().stream()
                .filter(e -> e.getTopic().contains(query) || e.getPattern().contains(query))
                .toList();
    }

    public List<KnowledgeEntry> getAllKnowledge() {
        return new ArrayList<>(knowledgeStore.values());
    }
}
