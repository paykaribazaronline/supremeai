package com.supremeai.controller;

import com.supremeai.audit.Audited;
import com.supremeai.model.SimulationResult;
import com.supremeai.model.SimulationScenario;
import com.supremeai.repository.SimulationResultRepository;
import com.supremeai.repository.SimulationScenarioRepository;
import com.supremeai.simulator.SimulationManager;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** REST API for Advanced Simulation Orchestration & Analysis. */
@RestController
@RequestMapping("/api/simulate")
public class SimulationController {

  private static final Logger logger = LoggerFactory.getLogger(SimulationController.class);

  @Autowired private SimulationManager simulationManager;

  @Autowired private SimulationScenarioRepository scenarioRepository;

  @Autowired private SimulationResultRepository resultRepository;

  /** POST /api/simulate/create Create a new simulation scenario configuration. */
  @Audited(resource = "simulation_scenario", action = "CREATE")
  @PostMapping("/create")
  @PreAuthorize("isAuthenticated()")
  public Mono<ResponseEntity<SimulationScenario>> createScenario(
      Authentication auth, @RequestBody Map<String, Object> request) {

    String userId = auth.getName();
    String appId = (String) request.get("appId");
    String name = (String) request.get("name");
    String description = (String) request.getOrDefault("description", "");

    @SuppressWarnings("unchecked")
    Map<String, Object> parameters =
        (Map<String, Object>) request.getOrDefault("parameters", new HashMap<>());

    if (appId == null || appId.trim().isEmpty() || name == null || name.trim().isEmpty()) {
      return Mono.just(ResponseEntity.badRequest().build());
    }

    return simulationManager
        .createScenario(userId, appId, name, description, parameters)
        .map(scenario -> ResponseEntity.status(HttpStatus.CREATED).body(scenario));
  }

  /** POST /api/simulate/execute Trigger execution of a simulator scenario. */
  @Audited(resource = "simulation_execution", action = "CREATE")
  @PostMapping("/execute")
  @PreAuthorize("isAuthenticated()")
  public Mono<ResponseEntity<SimulationResult>> executeSimulation(
      Authentication auth, @RequestParam String scenarioId) {

    String userId = auth.getName();
    return simulationManager
        .executeSimulation(userId, scenarioId)
        .map(ResponseEntity::ok)
        .onErrorResume(
            e -> {
              logger.error("Failed to execute simulation", e);
              return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
            });
  }

  /** GET /api/simulate/status Get the runtime status of an execution. */
  @GetMapping("/status")
  @PreAuthorize("isAuthenticated()")
  public Mono<ResponseEntity<SimulationResult>> getSimulationStatus(
      Authentication auth, @RequestParam String resultId) {

    String userId = auth.getName();
    return resultRepository
        .findById(resultId)
        .filter(result -> result.getUserId().equals(userId))
        .map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.notFound().build());
  }

  /**
   * GET /api/simulate/results Retrieve all simulation results for the authenticated user, or
   * filtered by appId.
   */
  @GetMapping("/results")
  @PreAuthorize("isAuthenticated()")
  public Mono<ResponseEntity<Flux<SimulationResult>>> getResults(
      Authentication auth, @RequestParam(required = false) String appId) {

    String userId = auth.getName();
    Flux<SimulationResult> results;

    if (appId != null && !appId.trim().isEmpty()) {
      results = resultRepository.findByAppId(appId).filter(res -> res.getUserId().equals(userId));
    } else {
      results = resultRepository.findByUserId(userId);
    }

    return Mono.just(ResponseEntity.ok(results));
  }

  /**
   * GET /api/simulate/scenarios Retrieve all simulation scenarios for the authenticated user, or
   * filtered by appId.
   */
  @GetMapping("/scenarios")
  @PreAuthorize("isAuthenticated()")
  public Mono<ResponseEntity<Flux<SimulationScenario>>> getScenarios(
      Authentication auth, @RequestParam(required = false) String appId) {

    String userId = auth.getName();
    Flux<SimulationScenario> scenarios;

    if (appId != null && !appId.trim().isEmpty()) {
      scenarios =
          scenarioRepository.findByAppId(appId).filter(scen -> scen.getUserId().equals(userId));
    } else {
      scenarios = scenarioRepository.findByUserId(userId);
    }

    return Mono.just(ResponseEntity.ok(scenarios));
  }
}
