package com.supremeai.repository;

import com.supremeai.model.ReasoningLog;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * Repository for ReasoningLog Firestore collection. Collection: reasoning_logs Mapped from: M-05 /
 * DATABASE_LINKAGE_MAP.md
 */
@Repository
public interface ReasoningLogRepository extends ReactiveCrudRepository<ReasoningLog, String> {

  /** Find all logs for a specific task. */
  Flux<ReasoningLog> findByTaskId(String taskId);

  /** Find all logs for a specific model. */
  Flux<ReasoningLog> findByModelName(String modelName);
}
