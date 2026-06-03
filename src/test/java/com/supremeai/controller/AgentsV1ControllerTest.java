package com.supremeai.controller;

import com.supremeai.model.Milestone;
import com.supremeai.repository.MilestoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentsV1ControllerTest {MilestoneRepositorypublic AgentsV1ControllerTest(MilestoneRepository milestoneRepository, AgentsV1Controller controller) {
MilestoneRepository    this.milestoneRepository = milestoneRepository;
MilestoneRepository    this.controller = controller;
MilestoneRepository}






    @BeforeEach
    void setUp() throws Exception {
        controller = new AgentsV1Controller();
        // Inject mock repository via reflection
        var field = AgentsV1Controller.class.getDeclaredField("milestoneRepository");
        field.setAccessible(true);
        field.set(controller, milestoneRepository);
        
        // Default stubbing to avoid NPE in allEndpoints test
        lenient().when(milestoneRepository.findAllByOrderByOrderAsc()).thenReturn(Flux.empty());
    }

    @Test
    void getAllPhases_shouldReturnPhasesWithCorrectStructure() {
        // Arrange
        Milestone m1 = new Milestone("m1", "Phase 1", "Week 1-2", 100, "green", "icon1", 1);
        Milestone m2 = new Milestone("m2", "Phase 2", "Week 3-4", 50, "yellow", "icon2", 2);
        Milestone m3 = new Milestone("m3", "Phase 3", "Week 5-6", 0, "red", "icon3", 3);

        when(milestoneRepository.findAllByOrderByOrderAsc()).thenReturn(Flux.just(m1, m2, m3));

        // Act
        Mono<Map<String, Object>> result = controller.getAllPhases();

        // Assert
        StepVerifier.create(result)
                .consumeNextWith(response -> {
                    assertEquals(3, response.get("totalPhases"));
                    assertEquals(1L, response.get("operationalCount"));
                    assertEquals("OPTIMAL", response.get("systemStatus"));
                    assertNotNull(response.get("timestamp"));
                    assertTrue(response.containsKey("phases"));

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> phases = (List<Map<String, Object>>) response.get("phases");
                    assertEquals(3, phases.size());

                    Map<String, Object> firstPhase = phases.get(0);
                    assertEquals("m1", firstPhase.get("id"));
                    assertEquals(1, firstPhase.get("phase"));
                    assertEquals("Phase 1", firstPhase.get("name"));
                    assertEquals("operational", firstPhase.get("status"));
                    assertEquals("Week 1-2", firstPhase.get("description"));
                })
                .verifyComplete();
    }

    @Test
    void getAllPhases_shouldReturnEmptyPhasesList_whenNoMilestones() {
        // Arrange
        when(milestoneRepository.findAllByOrderByOrderAsc()).thenReturn(Flux.empty());

        // Act
        Mono<Map<String, Object>> result = controller.getAllPhases();

        // Assert
        StepVerifier.create(result)
                .consumeNextWith(response -> {
                    assertEquals(0, response.get("totalPhases"));
                    assertEquals(0L, response.get("operationalCount"));
                    assertEquals("OPTIMAL", response.get("systemStatus"));
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> phases = (List<Map<String, Object>>) response.get("phases");
                    assertTrue(phases.isEmpty());
                })
                .verifyComplete();
    }

    @Test
    void getAllPhases_shouldMarkInProgressStatus_correctly() {
        // Arrange
        Milestone milestone = new Milestone("m1", "Test", "Timeline", 50, "blue", "icon", 1);
        when(milestoneRepository.findAllByOrderByOrderAsc()).thenReturn(Flux.just(milestone));

        // Act
        Mono<Map<String, Object>> result = controller.getAllPhases();

        // Assert
        StepVerifier.create(result)
                .consumeNextWith(response -> {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> phases = (List<Map<String, Object>>) response.get("phases");
                    assertEquals("in-progress", phases.get(0).get("status"));
                })
                .verifyComplete();
    }

    @Test
    void getAllPhases_shouldUseTitleAsName_andFallbackToName() {
        // Arrange
        Milestone milestoneWithTitle = new Milestone("m1", "Fallback Name", "Title Only", 0, "blue", "icon", 1);
        milestoneWithTitle.setTitle("Real Title");
        when(milestoneRepository.findAllByOrderByOrderAsc()).thenReturn(Flux.just(milestoneWithTitle));

        // Act
        Mono<Map<String, Object>> result = controller.getAllPhases();

        // Assert
        StepVerifier.create(result)
                .consumeNextWith(response -> {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> phases = (List<Map<String, Object>>) response.get("phases");
                    assertEquals("Real Title", phases.get(0).get("name"));
                })
                .verifyComplete();
    }

    @Test
    void getPhase8Summary_shouldReturnSecurityPhaseSummary() {
        // Act
        Mono<Map<String, Object>> result = controller.getPhase8Summary();

        // Assert
        StepVerifier.create(result)
                .consumeNextWith(summary -> {
                    assertEquals("operational", summary.get("status"));
                    assertEquals(3, summary.get("agentCount"));
                    assertTrue(summary.containsKey("agents"));
                    assertTrue(summary.containsKey("capabilities"));
                })
                .verifyComplete();
    }

    @Test
    void getPhase9Summary_shouldReturnCostOptimizationSummary() {
        // Act
        Mono<Map<String, Object>> result = controller.getPhase9Summary();

        // Assert
        StepVerifier.create(result)
                .consumeNextWith(summary -> {
                    assertEquals("operational", summary.get("status"));
                    assertEquals(2, summary.get("agentCount"));
                    assertTrue(summary.containsKey("agents"));
                    assertTrue(summary.containsKey("capabilities"));
                })
                .verifyComplete();
    }

    @Test
    void getPhase10Summary_shouldReturnSelfEvolutionSummary() {
        // Act
        Mono<Map<String, Object>> result = controller.getPhase10Summary();

        // Assert
        StepVerifier.create(result)
                .consumeNextWith(summary -> {
                    assertEquals("in-progress", summary.get("status"));
                    assertEquals(1, summary.get("agentCount"));
                    assertTrue(summary.containsKey("agents"));
                    assertTrue(summary.containsKey("capabilities"));
                })
                .verifyComplete();
    }

    @Test
    void getOptimizationHealth_shouldReturnHealthStatus() {
        // Act
        Mono<Map<String, Object>> result = controller.getOptimizationHealth();

        // Assert
        StepVerifier.create(result)
                .consumeNextWith(health -> {
                    assertEquals("UP", health.get("status"));
                    assertTrue(health.containsKey("services"));
                })
                .verifyComplete();
    }

    @Test
    void getPhase6Health_shouldReturnHealthStatus() {
        // Act
        Mono<Map<String, Object>> result = controller.getPhase6Health();

        // Assert
        StepVerifier.create(result)
                .consumeNextWith(health -> {
                    assertEquals("UP", health.get("status"));
                    assertEquals(4, health.get("agentCount"));
                    assertTrue(health.containsKey("capabilities"));
                })
                .verifyComplete();
    }

    @Test
    void getPhase7AgentsSummary_shouldReturnGeneratorAgentsSummary() {
        // Act
        Mono<Map<String, Object>> result = controller.getPhase7Summary();

        // Assert
        StepVerifier.create(result)
                .consumeNextWith(summary -> {
                    assertEquals("operational", summary.get("status"));
                    assertEquals(5, summary.get("agentCount"));
                    assertTrue(summary.containsKey("agents"));
                })
                .verifyComplete();
    }

    @Test
    void allEndpoints_shouldReturnMonoWithValidResponse() {
        // Test that all endpoints return a valid Mono
        assertAll(
            () -> assertNotNull(controller.getAllPhases()),
            () -> assertNotNull(controller.getPhase8Summary()),
            () -> assertNotNull(controller.getPhase9Summary()),
            () -> assertNotNull(controller.getPhase10Summary()),
            () -> assertNotNull(controller.getOptimizationHealth()),
            () -> assertNotNull(controller.getPhase6Health()),
            () -> assertNotNull(controller.getPhase7Summary())
        );
    }
}
