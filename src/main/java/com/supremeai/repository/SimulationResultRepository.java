package com.supremeai.repository;

import com.supremeai.model.SimulationResult;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/** Reactive Firestore repository for simulation execution results. */
@Repository
public interface SimulationResultRepository
    extends ReactiveCrudRepository<SimulationResult, String> {

  /** Find results by scenario ID */
  Flux<SimulationResult> findByScenarioId(String scenarioId);

  /** Find results by user ID */
  Flux<SimulationResult> findByUserId(String userId);

  /** Find results by app ID */
  Flux<SimulationResult> findByAppId(String appId);
}
