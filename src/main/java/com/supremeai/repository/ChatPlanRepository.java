package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.ChatPlan;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ChatPlanRepository extends FirestoreReactiveRepository<ChatPlan> {
    Flux<ChatPlan> findByActiveTrue();
    Mono<ChatPlan> findByIdAndActiveTrue(String id);
}
