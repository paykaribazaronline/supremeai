package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.StorageMetadata;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface StorageMetadataRepository extends FirestoreReactiveRepository<StorageMetadata> {
  Flux<StorageMetadata> findByCategory(String category);

  Flux<StorageMetadata> findByUserId(String userId);

  Flux<StorageMetadata> findByStorageProvider(String storageProvider);
}
