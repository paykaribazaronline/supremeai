package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.UserLanguagePreference;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface UserLanguagePreferenceRepository extends FirestoreReactiveRepository<UserLanguagePreference> {
    Mono<UserLanguagePreference> findByUserId(String userId);

    Flux<UserLanguagePreference> findAll();
}
