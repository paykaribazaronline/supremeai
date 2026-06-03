package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.WorkflowExecution;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface WorkflowExecutionRepository extends FirestoreReactiveRepository<WorkflowExecution> {
    Mono<WorkflowExecution> findByExecutionId(String executionId);
}
