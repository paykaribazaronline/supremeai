package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.ProtocolRule;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ProtocolRuleRepository extends FirestoreReactiveRepository<ProtocolRule> {
    Flux<ProtocolRule> findByActive(boolean active);
}