package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.SimulatorDeploymentRecord;
import org.springframework.stereotype.Repository;

/**
 * Firestore repository for SimulatorDeploymentRecord entities. Collection: "simulator_deployments"
 */
@Repository
public interface SimulatorDeploymentRepository
    extends FirestoreReactiveRepository<SimulatorDeploymentRecord> {}
