package com.supremeai.repository;

import com.supremeai.model.HealingEvent;
import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface HealingEventRepository extends FirestoreReactiveRepository<HealingEvent> {
    Flux<HealingEvent> findAllByOrderByTimestampDesc();
    Flux<HealingEvent> findByErrorTypeOrderByTimestampDesc(String errorType);
    Flux<HealingEvent> findBySuccessOrderByTimestampDesc(boolean success);
}
