package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.learning.knowledge.SolutionMemory;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public interface SolutionMemoryRepository extends FirestoreReactiveRepository<SolutionMemory> {

    /**
     * Find all solutions for a specific error signature.
     * Note: Firestore doesn't natively support custom query methods on non-indexed fields.
     * We'll need to either create a composite index or fetch all and filter in memory.
     * For now, we'll use in-memory filtering via custom implementation.
     */
    default Flux<SolutionMemory> findByTriggerError(String errorSignature) {
        return findAll()
                .filter(solution -> errorSignature.equals(solution.getTriggerError()));
    }

    /**
     * Find top solutions for an error based on supreme score.
     * Returns at most 'limit' solutions sorted by score descending.
     */
    default Flux<SolutionMemory> findTopSolutionsByError(String errorSignature, int limit) {
        return findByTriggerError(errorSignature)
                .sort((a, b) -> Double.compare(b.calculateSupremeScore(), a.calculateSupremeScore()))
                .take(limit);
    }
}
