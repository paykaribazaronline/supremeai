package com.supremeai.simulator;

import com.supremeai.model.SimulationResult;
import com.supremeai.model.SimulationScenario;
import com.supremeai.repository.SimulationResultRepository;
import com.supremeai.repository.SimulationScenarioRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Handles simulation environment orchestration, scenario management, and resource allocation. */
@Service
public class SimulationManager {

  private static final Logger logger = LoggerFactory.getLogger(SimulationManager.class);

  @Autowired private SimulationScenarioRepository scenarioRepository;

  @Autowired private SimulationResultRepository resultRepository;

  @Autowired private ControllerEngine controllerEngine;

  @Autowired private ResultAnalyzer resultAnalyzer;

  /** Create a new simulation scenario */
  public Mono<SimulationScenario> createScenario(
      String userId,
      String appId,
      String name,
      String description,
      java.util.Map<String, Object> parameters) {
    logger.info("Creating simulation scenario '{}' for user {} on app {}", name, userId, appId);

    String scenarioId = "scen_" + UUID.randomUUID().toString().substring(0, 12);
    SimulationScenario scenario =
        new SimulationScenario(scenarioId, userId, appId, name, description);
    if (parameters != null) {
      scenario.setParameters(parameters);
    }

    return scenarioRepository.save(scenario);
  }

  /** Get scenario details */
  public Mono<SimulationScenario> getScenario(String scenarioId) {
    return scenarioRepository.findById(scenarioId);
  }

  /** Get scenarios by user ID */
  public Flux<SimulationScenario> getScenariosByUserId(String userId) {
    return scenarioRepository.findByUserId(userId);
  }

  /** Get scenarios by application ID */
  public Flux<SimulationScenario> getScenariosByAppId(String appId) {
    return scenarioRepository.findByAppId(appId);
  }

  /** Execute simulation for a given scenario */
  public Mono<SimulationResult> executeSimulation(String userId, String scenarioId) {
    logger.info("Executing simulation for scenario {} by user {}", scenarioId, userId);

    return scenarioRepository
        .findById(scenarioId)
        .switchIfEmpty(Mono.error(new IllegalArgumentException("Scenario not found")))
        .flatMap(
            scenario -> {
              String resultId = "res_" + UUID.randomUUID().toString().substring(0, 12);
              SimulationResult result =
                  new SimulationResult(resultId, scenarioId, userId, scenario.getAppId());
              result.setStatus("RUNNING");
              result.getLogs().add("Simulation initiated at " + LocalDateTime.now());
              result.getLogs().add("Orchestrating simulation environment...");

              return resultRepository
                  .save(result)
                  .flatMap(
                      savedResult -> {
                        // Launch simulation asynchronously in background via ControllerEngine
                        // Then analyze the results via ResultAnalyzer and update record
                        Mono.defer(
                                () ->
                                    controllerEngine
                                        .runSimulation(scenario, savedResult)
                                        .flatMap(
                                            executedResult ->
                                                resultAnalyzer.analyze(executedResult))
                                        .flatMap(
                                            analyzedResult -> {
                                              analyzedResult.setStatus("SUCCESS");
                                              analyzedResult
                                                  .getLogs()
                                                  .add(
                                                      "Simulation completed successfully at "
                                                          + LocalDateTime.now());
                                              return resultRepository.save(analyzedResult);
                                            })
                                        .onErrorResume(
                                            e -> {
                                              logger.error(
                                                  "Simulation failed for result {}",
                                                  savedResult.getResultId(),
                                                  e);
                                              savedResult.setStatus("FAILED");
                                              savedResult
                                                  .getLogs()
                                                  .add("Simulation failed: " + e.getMessage());
                                              return resultRepository.save(savedResult);
                                            }))
                            .subscribe(); // Run in the background reactive context

                        return Mono.just(savedResult);
                      });
            });
  }
}
