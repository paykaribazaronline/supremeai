package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.ChatPlan;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ChatPlanRepository extends FirestoreReactiveRepository<ChatPlan> {
    Flux<ChatPlan> findByActive(boolean active);
    Mono<ChatPlan> findByIdAndActive(String id, boolean active);
}
