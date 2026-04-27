package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.ModelEvolution;
import org.springframework.stereotype.Repository;

@Repository
public interface ModelEvolutionRepository extends FirestoreReactiveRepository<ModelEvolution> {
}
