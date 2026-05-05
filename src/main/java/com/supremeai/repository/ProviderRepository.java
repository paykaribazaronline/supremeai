package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.APIProvider;
import org.springframework.stereotype.Repository;

@Repository
public interface ProviderRepository extends FirestoreReactiveRepository<APIProvider> {
}
