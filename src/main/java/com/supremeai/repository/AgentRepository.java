package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.dto.AgentStatus;
import com.supremeai.model.Agent;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface AgentRepository extends FirestoreReactiveRepository<Agent> {

    default Mono<AgentStatus> findStatusById(String agentId) {
        return findById(agentId)
                .map(agent -> AgentStatus.builder()
                        .agentId(agent.getId())
                        .status(agent.getStatus() != null ? agent.getStatus() : "UNKNOWN")
                        .lastUpdated(System.currentTimeMillis())
                        .build());
    }

    default Mono<Void> updateStatus(String agentId, AgentStatus status) {
        return findById(agentId)
                .flatMap(agent -> {
                    agent.setStatus(status.getStatus());
                    return save(agent);
                })
                .then();
    }
}
