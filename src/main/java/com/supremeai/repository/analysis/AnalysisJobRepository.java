package com.supremeai.repository.analysis;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.analysis.AnalysisJob;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Firestore repository for analysis jobs.
 */
@Repository
public interface AnalysisJobRepository extends FirestoreReactiveRepository<AnalysisJob> {
    Flux<AnalysisJob> findByStatus(String status);
    Mono<AnalysisJob> findById(String id);
}
