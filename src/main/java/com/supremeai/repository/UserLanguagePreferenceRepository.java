package com.supremeai.repository;

import com.supremeai.model.UserLanguagePreference;
import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserLanguagePreferenceRepository extends FirestoreReactiveRepository<UserLanguagePreference> {
    // ব্যবহারকারী আইডি দিয়ে ভাষা পছন্দ খুঁজে বের করা
    Mono<UserLanguagePreference> findByUserId(String userId);

    // সব ব্যবহারকারীর ভাষা পছন্দ পান
    Flux<UserLanguagePreference> findAll();
}