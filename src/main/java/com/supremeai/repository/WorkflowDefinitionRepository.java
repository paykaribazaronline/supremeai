package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.WorkflowDefinition;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface WorkflowDefinitionRepository extends FirestoreReactiveRepository<WorkflowDefinition> {
    Mono<WorkflowDefinition> findByName(String name);
}
