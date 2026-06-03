package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.SystemLearning;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface SystemLearningRepository extends FirestoreReactiveRepository<SystemLearning> {
    Flux<SystemLearning> findByCategory(String category);
    // Note: Firestore does not support 'After' predicate. Use custom query or filter in service.
    Flux<SystemLearning> findByConfidenceScoreGreaterThanEqual(double confidenceScore);
}
