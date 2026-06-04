package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.SystemInstruction;
import reactor.core.publisher.Flux;

public interface SystemInstructionRepository
    extends FirestoreReactiveRepository<SystemInstruction> {
  Flux<SystemInstruction> findAllByIsActiveOrderByPriorityDesc(boolean isActive);
}
