package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.Agent;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentRepository extends FirestoreReactiveRepository<Agent> {
}
