package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.KnowledgeRecommendation;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface KnowledgeRecommendationRepository
    extends FirestoreReactiveRepository<KnowledgeRecommendation> {
  Flux<KnowledgeRecommendation> findByStatus(KnowledgeRecommendation.Status status);
}
