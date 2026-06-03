package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.MonitoringLog;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface MonitoringLogRepository extends FirestoreReactiveRepository<MonitoringLog> {
    Flux<MonitoringLog> findByOrderByTimestampDesc();
    Flux<MonitoringLog> findByComponentOrderByTimestampDesc(String component);
}
