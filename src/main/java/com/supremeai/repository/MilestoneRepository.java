package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.Milestone;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface MilestoneRepository extends FirestoreReactiveRepository<Milestone> {
    Flux<Milestone> findAllByOrderByOrderAsc();
}
