package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.ConsensusResult;
import org.springframework.stereotype.Repository;

/** Repository for storing AI consensus results in Firestore. */
@Repository
public interface ConsensusResultRepository extends FirestoreReactiveRepository<ConsensusResult> {}
