package com.supremeai.repository;

import com.supremeai.model.ProviderTaskPerformance;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * Repository for ProviderTaskPerformance Firestore collection. Collection:
 * provider_task_performance Mapped from: M-07 / DATABASE_LINKAGE_MAP.md
 */
@Repository
public interface ProviderTaskPerformanceRepository
    extends ReactiveCrudRepository<ProviderTaskPerformance, String> {

  /** Find all performance records for a specific provider. */
  Flux<ProviderTaskPerformance> findByProvider(String provider);

  /** Find all records for a specific task type across all providers. */
  Flux<ProviderTaskPerformance> findByTaskType(String taskType);
}
