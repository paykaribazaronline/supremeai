package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.ChatRule;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ChatRuleRepository extends FirestoreReactiveRepository<ChatRule> {
    Flux<ChatRule> findByActiveTrue();
    Mono<ChatRule> findByIdAndActiveTrue(String id);
}
