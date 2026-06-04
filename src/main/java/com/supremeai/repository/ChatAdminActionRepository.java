package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.ChatAdminAction;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ChatAdminActionRepository extends FirestoreReactiveRepository<ChatAdminAction> {
  Flux<ChatAdminAction> findByActive(boolean active);
}
