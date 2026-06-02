package com.supremeai.simulator;

import com.supremeai.model.SimulationScenario;
import com.supremeai.model.SimulationResult;
import com.supremeai.repository.SimulationScenarioRepository;
import com.supremeai.repository.SimulationResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SimulationManagerTest {SimulationScenarioRepositorypublic SimulationManagerTest(SimulationScenarioRepository scenarioRepository, SimulationResultRepository resultRepository, ControllerEngine controllerEngine, ResultAnalyzer resultAnalyzer, SimulationManager simulationManager, SimulationScenario scenario, SimulationResult result) {
SimulationScenarioRepository    this.scenarioRepository = scenarioRepository;
SimulationScenarioRepository    this.resultRepository = resultRepository;
SimulationScenarioRepository    this.controllerEngine = controllerEngine;
SimulationScenarioRepository    this.resultAnalyzer = resultAnalyzer;
SimulationScenarioRepository    this.simulationManager = simulationManager;
SimulationScenarioRepository    this.scenario = scenario;
SimulationScenarioRepository    this.result = result;
SimulationScenarioRepository}










    @InjectMocks





    @BeforeEach
    void setUp() {
        scenario = new SimulationScenario("scen-1", "user-1", "app-1", "Test Scenario", "Description");
        result = new SimulationResult("res-1", "scen-1", "user-1", "app-1");
    }

    @Test
    void createScenario_shouldSaveAndReturnScenario() {
        Map<String, Object> params = new HashMap<>();
        params.put("networkSpeed", "5G");

        when(scenarioRepository.save(any(SimulationScenario.class))).thenAnswer(invocation -> {
            SimulationScenario saved = invocation.getArgument(0);
            return Mono.just(saved);
        });

        StepVerifier.create(simulationManager.createScenario("user-1", "app-1", "Test Scenario", "Description", params))
                .expectNextMatches(savedScen -> {
                    assertEquals("user-1", savedScen.getUserId());
                    assertEquals("app-1", savedScen.getAppId());
                    assertEquals("Test Scenario", savedScen.getName());
                    assertEquals("5G", savedScen.getParameters().get("networkSpeed"));
                    assertNotNull(savedScen.getScenarioId());
                    return true;
                })
                .verifyComplete();

        verify(scenarioRepository, times(1)).save(any(SimulationScenario.class));
    }

    @Test
    void getScenario_shouldReturnScenario() {
        when(scenarioRepository.findById("scen-1")).thenReturn(Mono.just(scenario));

        StepVerifier.create(simulationManager.getScenario("scen-1"))
                .expectNext(scenario)
                .verifyComplete();
    }

    @Test
    void getScenariosByUserId_shouldReturnScenarios() {
        when(scenarioRepository.findByUserId("user-1")).thenReturn(Flux.just(scenario));

        StepVerifier.create(simulationManager.getScenariosByUserId("user-1"))
                .expectNext(scenario)
                .verifyComplete();
    }

    @Test
    void getScenariosByAppId_shouldReturnScenarios() {
        when(scenarioRepository.findByAppId("app-1")).thenReturn(Flux.just(scenario));

        StepVerifier.create(simulationManager.getScenariosByAppId("app-1"))
                .expectNext(scenario)
                .verifyComplete();
    }

    @Test
    void executeSimulation_whenScenarioNotFound_shouldThrowException() {
        when(scenarioRepository.findById("scen-unknown")).thenReturn(Mono.empty());

        StepVerifier.create(simulationManager.executeSimulation("user-1", "scen-unknown"))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void executeSimulation_whenScenarioExists_shouldInitiateSimulation() {
        when(scenarioRepository.findById("scen-1")).thenReturn(Mono.just(scenario));
        when(resultRepository.save(any(SimulationResult.class))).thenAnswer(invocation -> {
            SimulationResult saved = invocation.getArgument(0);
            return Mono.just(saved);
        });

        // Mock async background process calls
        when(controllerEngine.runSimulation(any(SimulationScenario.class), any(SimulationResult.class)))
                .thenReturn(Mono.just(result));
        when(resultAnalyzer.analyze(any(SimulationResult.class))).thenReturn(Mono.just(result));

        StepVerifier.create(simulationManager.executeSimulation("user-1", "scen-1"))
                .expectNextMatches(res -> {
                    assertEquals("scen-1", res.getScenarioId());
                    assertEquals("user-1", res.getUserId());
                    assertEquals("app-1", res.getAppId());
                    assertEquals("RUNNING", res.getStatus());
                    return true;
                })
                .verifyComplete();

        verify(scenarioRepository, times(1)).findById("scen-1");
        verify(resultRepository, atLeastOnce()).save(any(SimulationResult.class));
    }
}
