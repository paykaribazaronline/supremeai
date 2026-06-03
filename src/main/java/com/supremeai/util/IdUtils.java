package com.supremeai.util;

import java.util.UUID;

/**
 * Minimal utility class for common ID operations.
 * Centralizes ID generation to avoid duplicated logic across controllers.
 */
public final class IdUtils {

    private IdUtils() { /* static only */ }

    /**
     * Generate a random UUID string.
     * Consider migrating to ULID for sorting/performance if needed.
     */
    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate a short random ID (12 chars) for sessions or temporary identifiers.
     * Useful for WebSocket session IDs, etc.
     */
    public static String shortId() {
        return UUID.randomUUID().toString().substring(0, 12);
    }

    /**
     * Ensure an entity has an ID; generate if null or empty.
     * Useful in controller create operations.
     */
    public static String ensureId(String currentId) {
        return (currentId == null || currentId.trim().isEmpty()) ? randomUUID() : currentId;
    }
}
