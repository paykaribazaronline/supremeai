package com.supremeai.repository;

import com.supremeai.model.UserSimulatorProfile;
import com.supremeai.model.UserTier;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Reactive Firestore repository for simulator profiles.
 *
 * Collection: simulator_profiles
 * Document ID: Firebase user UID
 */
@org.springframework.stereotype.Repository
public interface UserSimulatorProfileRepository extends ReactiveCrudRepository<UserSimulatorProfile, String> {

    /**
     * Find by Firebase UID ( equivalent to findById )
     */
    Mono<UserSimulatorProfile> findByUserId(String userId);

    /**
     * Find profiles with at least N active installs
     */
    Flux<UserSimulatorProfile> findByActiveInstallsGreaterThan(int minInstalls);

    /**
     * Count profiles that have an active session
     */
    Mono<Long> countByCurrentSessionIsNotNull();

    /**
     * Find profiles that have been inactive before cutoff (for cleanup job)
     */
    Flux<UserSimulatorProfile> findByLastActiveAtBefore(LocalDateTime cutoffTime);

    /**
     * Find all profiles for a specific user tier
     */
    Flux<UserSimulatorProfile> findByUserTier(UserTier userTier);
}
