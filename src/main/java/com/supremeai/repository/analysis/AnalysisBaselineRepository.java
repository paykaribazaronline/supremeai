package com.supremeai.repository.analysis;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.analysis.AnalysisBaseline;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface AnalysisBaselineRepository extends FirestoreReactiveRepository<AnalysisBaseline> {
    Flux<AnalysisBaseline> findByProjectId(String projectId);
    Mono<AnalysisBaseline> findByProjectIdAndCommitHash(String projectId, String commitHash);
    Mono<AnalysisBaseline> findFirstByProjectIdOrderByCreatedAtAsc(String projectId);
}
