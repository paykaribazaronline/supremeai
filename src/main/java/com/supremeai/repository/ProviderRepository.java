package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.APIProvider;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ProviderRepository extends FirestoreReactiveRepository<APIProvider> {
    Flux<APIProvider> findByStatus(String status);
}
