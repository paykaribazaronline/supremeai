package com.supremeai.repository.browser;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.browser.StoredCredential;
import reactor.core.publisher.Mono;

public interface StoredCredentialRepository extends FirestoreReactiveRepository<StoredCredential> {
  Mono<StoredCredential> findByWebsite(String website);

  reactor.core.publisher.Flux<StoredCredential> findByUserId(String userId);
}
