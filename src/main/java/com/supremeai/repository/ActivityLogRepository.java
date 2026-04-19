package com.supremeai.repository;

import com.supremeai.model.ActivityLog;
import com.google.cloud.spring.data.firestore.FirestoreRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ActivityLogRepository extends FirestoreRepository<ActivityLog> {
    Flux<ActivityLog> findByCategoryOrderByTimestampDesc(String category);
    Flux<ActivityLog> findBySeverityOrderByTimestampDesc(String severity);
}