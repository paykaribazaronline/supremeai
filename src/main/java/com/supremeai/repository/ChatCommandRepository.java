package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.ChatCommand;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ChatCommandRepository extends FirestoreReactiveRepository<ChatCommand> {
    Flux<ChatCommand> findByActiveTrue();
    Mono<ChatCommand> findByIdAndActiveTrue(String id);
}
