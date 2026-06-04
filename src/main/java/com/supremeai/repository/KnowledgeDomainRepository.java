package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.KnowledgeDomain;
import com.supremeai.model.KnowledgeDomain.Status;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface KnowledgeDomainRepository extends FirestoreReactiveRepository<KnowledgeDomain> {
  Flux<KnowledgeDomain> findByStatus(Status status);

  Flux<KnowledgeDomain> findByKeywordsContaining(String keyword);
}
