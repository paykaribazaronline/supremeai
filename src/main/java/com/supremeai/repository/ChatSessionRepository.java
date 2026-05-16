package com.supremeai.repository;

import com.supremeai.model.ChatSession;
import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ChatSessionRepository extends FirestoreReactiveRepository<ChatSession> {
    Flux<ChatSession> findAllByUserId(String userId);
    Mono<Void> deleteAllByUserId(String userId);
}
