package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.ReverseEngineeringJob;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Firestore repository for ReverseEngineeringJob entities. Collection: "reverse_engineering_jobs"
 */
@Repository
public interface ReverseEngineeringJobRepository
    extends FirestoreReactiveRepository<ReverseEngineeringJob> {
  Mono<ReverseEngineeringJob> findByJobId(String jobId);

  Mono<ReverseEngineeringJob> findByUserIdAndStatus(String userId, String status);

  java.util.List<ReverseEngineeringJob> findByUserId(String userId);

  Flux<ReverseEngineeringJob> findAllByOrderByCreatedAtDesc();
}
