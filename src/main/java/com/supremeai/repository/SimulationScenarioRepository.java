package com.supremeai.repository;

import com.supremeai.model.SimulationScenario;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive Firestore repository for simulation scenarios.
 */
@Repository
public interface SimulationScenarioRepository extends ReactiveCrudRepository<SimulationScenario, String> {

    /**
     * Find scenarios by user ID
     */
    Flux<SimulationScenario> findByUserId(String userId);

    /**
     * Find scenarios by application ID
     */
    Flux<SimulationScenario> findByAppId(String appId);
}
