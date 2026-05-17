package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.ImprovementProposal;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ImprovementProposalRepository extends FirestoreReactiveRepository<ImprovementProposal> {
    Flux<ImprovementProposal> findByApproved(boolean approved);
}
