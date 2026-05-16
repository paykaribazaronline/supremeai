package com.supremeai.repository;

import com.supremeai.model.TaskProviderAssignment;
import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repository for task-to-provider assignments.
 * Supports dynamic 0 to ∞ provider mappings per task.
 */
public interface TaskProviderAssignmentRepository
        extends FirestoreReactiveRepository<TaskProviderAssignment> {

    Flux<TaskProviderAssignment> findByTaskTypeAndIsActive(String taskType, boolean isActive);

    Flux<TaskProviderAssignment> findAllByIsActive(boolean isActive);

    Mono<TaskProviderAssignment> findByTaskType(String taskType);

    Mono<Long> countByIsActive(boolean isActive);
}