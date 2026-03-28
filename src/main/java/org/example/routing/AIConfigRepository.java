package org.example.routing;

import java.util.Optional;

/**
 * Repository for admin-controlled AI routing configuration.
 *
 * <p>Implementations persist/retrieve the current provider priority sequence
 * so an admin can change routing order at runtime via the Dashboard UI without
 * a redeploy.  The production implementation will delegate to Firebase Firestore;
 * the stub is used until Firebase credentials are provisioned.
 */
public interface AIConfigRepository {

    /**
     * Return the persisted priority order (comma-separated provider names), if any.
     *
     * @return the stored sequence, or empty if none is saved yet
     */
    Optional<String> loadPriorityOrder();

    /**
     * Persist the given priority order so it survives restarts.
     *
     * @param priorityOrder comma-separated provider names, e.g. {@code "deepseek,kimi,gemini"}
     */
    void savePriorityOrder(String priorityOrder);
}
