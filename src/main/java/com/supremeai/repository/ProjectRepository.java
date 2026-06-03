package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.ExistingProject;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ProjectRepository extends FirestoreReactiveRepository<ExistingProject> {
    Flux<ExistingProject> findByOwnerId(String ownerId);
    Flux<ExistingProject> findByStatus(String status);
}
