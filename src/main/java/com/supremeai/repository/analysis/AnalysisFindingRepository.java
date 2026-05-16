package com.supremeai.repository.analysis;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.analysis.AnalysisFinding;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * Firestore repository for analysis findings.
 */
@Repository
public interface AnalysisFindingRepository extends FirestoreReactiveRepository<AnalysisFinding> {
    Flux<AnalysisFinding> findByJobId(String jobId);
    Flux<AnalysisFinding> findBySeverity(String severity);
    Flux<AnalysisFinding> findByCategory(String category);
}
