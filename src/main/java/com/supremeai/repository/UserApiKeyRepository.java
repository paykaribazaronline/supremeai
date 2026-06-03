package com.supremeai.repository;

import com.supremeai.model.UserApiKey;
import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface UserApiKeyRepository extends FirestoreReactiveRepository<UserApiKey> {
    Mono<UserApiKey> findByApiKey(String apiKey);
    Flux<UserApiKey> findByUserId(String userId);
    Flux<UserApiKey> findByUserIdAndProvider(String userId, String provider);
    Flux<UserApiKey> findByUserIdAndStatus(String userId, String status);
    Mono<Long> countByUserId(String userId);
    Mono<Long> countByUserIdAndStatus(String userId, String status);
}
