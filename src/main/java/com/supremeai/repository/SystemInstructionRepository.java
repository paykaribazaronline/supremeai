package com.supremeai.repository;

import com.supremeai.model.SystemInstruction;
import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import reactor.core.publisher.Flux;

public interface SystemInstructionRepository extends FirestoreReactiveRepository<SystemInstruction> {
    Flux<SystemInstruction> findAllByIsActiveOrderByPriorityDesc(boolean isActive);
}
