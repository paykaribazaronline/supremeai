package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.LearningSource;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface LearningSourceRepository extends FirestoreReactiveRepository<LearningSource> {
    Flux<LearningSource> findByEnabled(boolean enabled);
    Flux<LearningSource> findByDetectedFocus(String detectedFocus);
    Flux<LearningSource> findByManualFocus(String manualFocus);
    Flux<LearningSource> findByEnabledAndDetectedFocus(boolean enabled, String detectedFocus);
}
