package com.supremeai.repository.analysis;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.analysis.DependencyGraph;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface DependencyGraphRepository extends FirestoreReactiveRepository<DependencyGraph> {
    Flux<DependencyGraph> findByProjectId(String projectId);
    Mono<DependencyGraph> findByProjectIdAndFile(String projectId, String file);
}
