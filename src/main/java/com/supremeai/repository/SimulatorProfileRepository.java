package com.supremeai.repository;

import com.supremeai.model.UserSimulatorProfile;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Firestore repository for UserSimulatorProfile documents.
 * Collection: "simulator_profiles"
 * Document ID: Firebase user ID
 *
 * All operations are reactive (non-blocking).
 */
public interface SimulatorProfileRepository extends ReactiveCrudRepository<UserSimulatorProfile, String> {

    /**
     * Find simulator profile by Firebase user ID
     *
     * @param userId Firebase authentication UID
     * @return Mono of UserSimulatorProfile (empty if not found)
     */
    Mono<UserSimulatorProfile> findByUserId(String userId);

    /**
     * Find all profiles with at least N active installs
     * Useful for admin dashboard "active users" view
     *
     * @param minInstalls minimum active installs threshold
     * @return Flux of matching profiles
     */
    Flux<UserSimulatorProfile> findByActiveInstallsGreaterThan(int minInstalls);

    /**
     * Count total number of active simulator sessions across all users
     *
     * @return count of profiles with active sessions
     */
    Mono<Long> countByCurrentSessionIsNotNull();

    /**
     * Find profiles last active before a specific time (for cleanup job)
     *
     * @param cutoffTime Stale threshold timestamp
     * @return Flux of stale profiles
     */
    Flux<UserSimulatorProfile> findByLastActiveAtBefore(LocalDateTime cutoffTime);
}
