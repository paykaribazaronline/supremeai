package com.supremeai.repository;

import com.supremeai.model.ActivityLog;
import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

@Repository
public interface ActivityLogRepository extends FirestoreReactiveRepository<ActivityLog> {
    Flux<ActivityLog> findByCategoryOrderByTimestampDesc(String category);
    Flux<ActivityLog> findBySeverityOrderByTimestampDesc(String severity);
    // Firestore does not support 'Before' predicate. Implement delete in service layer.
}
