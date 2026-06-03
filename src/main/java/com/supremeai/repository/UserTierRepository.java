package com.supremeai.repository;

import com.supremeai.model.UserTierConfig;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * Repository for UserTierConfig Firestore collection.
 * Collection: user_tiers (one document per tier, keyed by UserTier enum name)
 * Mapped from: M-02 / DATABASE_LINKAGE_MAP.md
 */
@Repository
public interface UserTierRepository extends ReactiveCrudRepository<UserTierConfig, String> {

    /**
     * Find a tier configuration by its enum name.
     */
    Mono<UserTierConfig> findById(String tierName);

    /**
     * Find all premium tiers.
     */
    reactor.core.publisher.Flux<UserTierConfig> findByPremium(boolean premium);

    default reactor.core.publisher.Flux<UserTierConfig> findByPremiumTrue() {
        return findByPremium(true);
    }
}
