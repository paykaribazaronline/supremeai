package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.ConsensusVote;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsensusVoteRepository extends FirestoreReactiveRepository<ConsensusVote> {
}
