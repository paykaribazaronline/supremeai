package com.supremeai.repository;

import com.supremeai.model.InfrastructureAdvice;
import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface InfrastructureAdviceRepository extends FirestoreReactiveRepository<InfrastructureAdvice> {
    Mono<InfrastructureAdvice> findByAppId(String appId);
}
