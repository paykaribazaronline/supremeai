package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.SystemWorkRule;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * Repository for system_work_rules collection in Firestore. The admin panel reads from and writes
 * to this collection to define authoritative rules for how the system carries out work in
 * production.
 */
@Repository
public interface SystemWorkRuleRepository extends FirestoreReactiveRepository<SystemWorkRule> {
  Flux<SystemWorkRule> findByCategory(String category);

  Flux<SystemWorkRule> findByEnabled(boolean enabled);

  Flux<SystemWorkRule> findByTargetDoc(String targetDoc);
}
