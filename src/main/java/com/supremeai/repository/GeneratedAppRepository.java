package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.GeneratedApp;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Firestore repository for GeneratedApp entities.
 * Collection: "generated_apps"
 */
@Repository
public interface GeneratedAppRepository extends FirestoreReactiveRepository<GeneratedApp> {

    Mono<GeneratedApp> findByAppId(String appId);

    Mono<GeneratedApp> findByUserIdAndPlatform(String userId, String platform);

    Mono<Void> deleteByAppId(String appId);
}
