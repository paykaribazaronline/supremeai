package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.UserSimulatorProfile;
import com.supremeai.model.UserTier;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive Firestore repository for simulator profiles.
 *
 * <p>Collection: simulator_profiles Document ID: Firebase user UID
 */
@org.springframework.stereotype.Repository
public interface UserSimulatorProfileRepository
    extends FirestoreReactiveRepository<UserSimulatorProfile> {

  /** Find by Firebase UID ( equivalent to findById ) */
  Mono<UserSimulatorProfile> findByUserId(String userId);

  /** Find profiles with at least N active installs */
  Flux<UserSimulatorProfile> findByActiveInstallsGreaterThan(int minInstalls);

  /**
   * Count profiles that have an active session. Firestore does not support IsNotNull. Implement in
   * service layer.
   */
  // Mono<Long> countByCurrentSessionIsNotNull();

  /**
   * Find profiles that have been inactive before cutoff (for cleanup job) Firestore does not
   * support Before. Implement in service layer.
   */
  // Flux<UserSimulatorProfile> findByLastActiveAtBefore(LocalDateTime cutoffTime);

  /** Find all profiles for a specific user tier */
  Flux<UserSimulatorProfile> findByUserTier(UserTier userTier);
}
