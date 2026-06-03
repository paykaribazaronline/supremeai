package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.ChatMessage;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

/**
 * Repository for chat history stored in Firestore.
 */
@Repository
public interface ChatHistoryRepository extends FirestoreReactiveRepository<ChatMessage> {
    
    Flux<ChatMessage> findByUserId(String userId);

    Flux<ChatMessage> findByUserIdOrderByTimestampAsc(String userId);
}
