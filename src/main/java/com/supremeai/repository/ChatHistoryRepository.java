package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.ChatMessage;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

/**
 * Repository for chat history stored in Firestore.
 */
@Repository
public interface ChatHistoryRepository extends FirestoreReactiveRepository<ChatMessage> {
    
    /**
     * Delete messages created before a certain timestamp.
     */
    Mono<Long> deleteByTimestampBefore(LocalDateTime cutoff);
    
    /**
     * Delete guest messages (placeholder logic for specific cleanups).
     */
    default Mono<Long> deleteByIsGuestTrueAndCreatedAtBefore(LocalDateTime cutoff) {
        // Since ChatMessage doesn't have isGuest field yet, we use timestamp cutoff
        return deleteByTimestampBefore(cutoff);
    }

    /**
     * Delete all records before a certain date.
     */
    default Mono<Long> deleteByCreatedAtBefore(LocalDateTime cutoff) {
        return deleteByTimestampBefore(cutoff);
    }
}
