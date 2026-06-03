package com.supremeai.repository;

import com.supremeai.dto.AgentStatus;
import com.supremeai.model.Agent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class AgentRepositoryTest {AgentRepositorypublic AgentRepositoryTest(AgentRepository agentRepository) {
AgentRepository    this.agentRepository = agentRepository;
AgentRepository}




    @Test
    void findStatusById_shouldReturnStatus_whenAgentExists() {
        AgentStatus status = AgentStatus.builder()
                .agentId("agent-1")
                .status("ACTIVE")
                .lastUpdated(System.currentTimeMillis())
                .build();
        when(agentRepository.findStatusById("agent-1")).thenReturn(Mono.just(status));

        StepVerifier.create(agentRepository.findStatusById("agent-1"))
                .expectNextMatches(s -> "agent-1".equals(s.getAgentId())
                        && "ACTIVE".equals(s.getStatus())
                        && s.getLastUpdated() > 0)
                .verifyComplete();
    }

    @Test
    void findStatusById_shouldReturnUnknownStatus_whenAgentHasNullStatus() {
        AgentStatus status = AgentStatus.builder()
                .agentId("agent-2")
                .status("UNKNOWN")
                .lastUpdated(System.currentTimeMillis())
                .build();
        when(agentRepository.findStatusById("agent-2")).thenReturn(Mono.just(status));

        StepVerifier.create(agentRepository.findStatusById("agent-2"))
                .expectNextMatches(s -> "agent-2".equals(s.getAgentId())
                        && "UNKNOWN".equals(s.getStatus()))
                .verifyComplete();
    }

    @Test
    void findStatusById_shouldReturnEmpty_whenAgentNotFound() {
        when(agentRepository.findStatusById("nonexistent")).thenReturn(Mono.empty());

        StepVerifier.create(agentRepository.findStatusById("nonexistent"))
                .verifyComplete();
    }

    @Test
    void updateStatus_shouldSaveAgentWithNewStatus() {
        AgentStatus newStatus = AgentStatus.builder()
                .agentId("agent-3")
                .status("RUNNING")
                .lastUpdated(System.currentTimeMillis())
                .build();

        when(agentRepository.updateStatus("agent-3", newStatus)).thenReturn(Mono.empty());

        StepVerifier.create(agentRepository.updateStatus("agent-3", newStatus))
                .verifyComplete();
    }

    @Test
    void updateStatus_shouldComplete_whenAgentNotFound() {
        AgentStatus status = AgentStatus.builder()
                .agentId("missing")
                .status("RUNNING")
                .lastUpdated(System.currentTimeMillis())
                .build();

        when(agentRepository.updateStatus("missing", status)).thenReturn(Mono.empty());

        StepVerifier.create(agentRepository.updateStatus("missing", status))
                .verifyComplete();

        verify(agentRepository, never()).save(any());
    }

    @Test
    void save_shouldPersistAgent() {
        Agent agent = new Agent("agent-4", "NewAgent", "analyzer", "PENDING");
        when(agentRepository.save(agent)).thenReturn(Mono.just(agent));

        StepVerifier.create(agentRepository.save(agent))
                .expectNextMatches(a -> "agent-4".equals(a.getId()) && "PENDING".equals(a.getStatus()))
                .verifyComplete();
    }
}
