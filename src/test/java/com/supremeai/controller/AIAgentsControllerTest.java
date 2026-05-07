package com.supremeai.controller;

import com.supremeai.model.Agent;
import com.supremeai.repository.AgentRepository;
import com.supremeai.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIAgentsControllerTest {

    @Mock
    private AgentRepository agentRepository;

    private AIAgentsController controller;

    @BeforeEach
    void setUp() {
        controller = new AIAgentsController(agentRepository);
    }

    @Test
    void getAllAgents_shouldReturnListOfAgents() {
        Agent a1 = new Agent("agent-1", "Agent One", "worker", "ACTIVE");
        Agent a2 = new Agent("agent-2", "Agent Two", "analyzer", "IDLE");

        when(agentRepository.findAll()).thenReturn(Flux.just(a1, a2));

        StepVerifier.create(controller.getAllAgents())
                .expectNextMatches(response -> {
                    assertTrue(response.success());
                    assertEquals(2, response.data().size());
                    assertEquals("Agent One", response.data().get(0).getName());
                    assertEquals("Agent Two", response.data().get(1).getName());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getAllAgents_shouldReturnEmptyList_whenNoAgents() {
        when(agentRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(controller.getAllAgents())
                .expectNextMatches(response -> {
                    assertTrue(response.success());
                    assertTrue(response.data().isEmpty());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getAgentStats_shouldReturnCorrectCounts() {
        Agent a1 = new Agent("a1", "A1", "worker", "ACTIVE");
        Agent a2 = new Agent("a2", "A2", "worker", "ACTIVE");
        Agent a3 = new Agent("a3", "A3", "analyzer", "IDLE");

        when(agentRepository.findAll()).thenReturn(Flux.just(a1, a2, a3));

        StepVerifier.create(controller.getAgentStats())
                .expectNextMatches(response -> {
                    assertTrue(response.success());
                    Map<String, Object> stats = response.data();
                    assertEquals(3, stats.get("totalAgents"));
                    assertEquals(2L, stats.get("activeAgents"));
                    assertEquals(1L, stats.get("idleAgents"));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getAgentStats_shouldReturnZeroCounts_whenNoAgents() {
        when(agentRepository.findAll()).thenReturn(Flux.empty());

        StepVerifier.create(controller.getAgentStats())
                .expectNextMatches(response -> {
                    assertTrue(response.success());
                    Map<String, Object> stats = response.data();
                    assertEquals(0, stats.get("totalAgents"));
                    assertEquals(0L, stats.get("activeAgents"));
                    assertEquals(0L, stats.get("idleAgents"));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void createAgent_shouldSaveAndReturnAgent() {
        Agent input = new Agent(null, "New Agent", "worker", null);
        Agent saved = new Agent("agent-new", "New Agent", "worker", "IDLE");

        when(agentRepository.save(any(Agent.class))).thenReturn(Mono.just(saved));

        StepVerifier.create(controller.createAgent(input))
                .expectNextMatches(response -> {
                    assertTrue(response.success());
                    assertEquals("agent-new", response.data().getId());
                    assertEquals("IDLE", response.data().getStatus());
                    return true;
                })
                .verifyComplete();

        verify(agentRepository).save(any(Agent.class));
    }

    @Test
    void createAgent_shouldSetDefaultStatusToIdle() {
        Agent input = new Agent(null, "New Agent", "worker", null);
        Agent saved = new Agent("agent-new", "New Agent", "worker", "IDLE");

        when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> {
            Agent a = inv.getArgument(0);
            a.setId("agent-new");
            return Mono.just(a);
        });

        StepVerifier.create(controller.createAgent(input))
                .expectNextMatches(response -> {
                    assertEquals("IDLE", response.data().getStatus());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void updateAgentStatus_shouldUpdateAndReturnAgent() {
        Agent existing = new Agent("agent-1", "Agent One", "worker", "IDLE");

        when(agentRepository.findById("agent-1")).thenReturn(Mono.just(existing));
        when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(controller.updateAgentStatus("agent-1", "ACTIVE"))
                .expectNextMatches(response -> {
                    assertTrue(response.success());
                    assertEquals("ACTIVE", response.data().getStatus());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void updateAgentStatus_shouldReturnEmpty_whenAgentNotFound() {
        when(agentRepository.findById("missing")).thenReturn(Mono.empty());

        StepVerifier.create(controller.updateAgentStatus("missing", "ACTIVE"))
                .verifyComplete();
    }

    @Test
    void removeAgent_shouldDeleteAndReturnSuccess() {
        when(agentRepository.deleteById("agent-1")).thenReturn(Mono.empty());

        StepVerifier.create(controller.removeAgent("agent-1"))
                .expectNextMatches(response -> {
                    assertTrue(response.success());
                    assertEquals("Agent removed", response.data());
                    return true;
                })
                .verifyComplete();
    }
}
