package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.AIBehaviorProfile;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface AIBehaviorProfileRepository extends FirestoreReactiveRepository<AIBehaviorProfile> {
    Flux<AIBehaviorProfile> findByProjectId(String projectId);
    Mono<AIBehaviorProfile> findFirstByProjectId(String projectId);
}
