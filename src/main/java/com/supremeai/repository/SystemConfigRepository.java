package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.SystemConfig;
import org.springframework.stereotype.Repository;

/**
 * Repository for global system configuration.
 */
@Repository
public interface SystemConfigRepository extends FirestoreReactiveRepository<SystemConfig> {
}
