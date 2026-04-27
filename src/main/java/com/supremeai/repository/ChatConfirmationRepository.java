package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.ChatConfirmation;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface ChatConfirmationRepository extends FirestoreReactiveRepository<ChatConfirmation> {
    Flux<ChatConfirmation> findByItemId(String itemId);
    Flux<ChatConfirmation> findByChatId(String chatId);
    Flux<ChatConfirmation> findByItemTypeAndItemId(String itemType, String itemId);
}
