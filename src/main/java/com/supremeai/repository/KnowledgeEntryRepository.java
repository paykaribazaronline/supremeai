package com.supremeai.repository;

import com.supremeai.model.KnowledgeEntry;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * Repository for KnowledgeEntry Firestore collection.
 * Collection: knowledge_entries
 * Mapped from: M-04 / DATABASE_LINKAGE_MAP.md
 */
@Repository
public interface KnowledgeEntryRepository extends ReactiveCrudRepository<KnowledgeEntry, String> {

    /**
     * Find entries by source provider.
     */
    Flux<KnowledgeEntry> findBySourceProvider(String sourceProvider);

    /**
     * Find entries by topic keyword (case-insensitive).
     */
    Flux<KnowledgeEntry> findByTopicContainingIgnoreCase(String topicKeyword);
}
