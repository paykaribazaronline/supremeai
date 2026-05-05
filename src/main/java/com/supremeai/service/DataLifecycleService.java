package com.supremeai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Plan 17: Intelligent Data Lifecycle Management.
 *
 * Responsibilities:
 * - Track data with TTL (time-to-live) expiry
 * - Soft delete → grace period → hard delete pipeline
 * - Scheduled cleanup jobs to prune expired entries
 * - Recovery: restore soft-deleted items within grace period
 */
@Service
public class DataLifecycleService {

    private static final Logger logger = LoggerFactory.getLogger(DataLifecycleService.class);

    // Default TTL in days before soft-delete
    @Value("${lifecycle.default.ttl.days:30}")
    private int defaultTtlDays;

    // Grace period in days between soft-delete and hard-delete
    @Value("${lifecycle.grace.period.days:7}")
    private int gracePeriodDays;

    // In-memory registry; in production, back with Firestore collection "data_lifecycle"
    private final Map<String, LifecycleEntry> registry = new ConcurrentHashMap<>();

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Register a data item for lifecycle tracking.
     *
     * @param dataId   Unique identifier of the data item
     * @param dataType Category of data (e.g., "chat_history", "simulator_session")
     * @param ttlDays  Custom TTL in days; 0 = use default
     */
    public void register(String dataId, String dataType, int ttlDays) {
        int effectiveTtl = ttlDays > 0 ? ttlDays : defaultTtlDays;
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(effectiveTtl);
        LifecycleEntry entry = new LifecycleEntry(dataId, dataType, expiresAt);
        registry.put(dataId, entry);
        logger.debug("[LIFECYCLE] Registered {} type={} expiresAt={}", dataId, dataType, expiresAt);
    }

    /**
     * Convenience method using default TTL.
     */
    public void register(String dataId, String dataType) {
        register(dataId, dataType, 0);
    }

    /**
     * Soft-delete a data item immediately (marks as deleted, starts grace period).
     */
    public void softDelete(String dataId) {
        LifecycleEntry entry = registry.get(dataId);
        if (entry == null) {
            logger.warn("[LIFECYCLE] softDelete: dataId not found: {}", dataId);
            return;
        }
        entry.setSoftDeleted(true);
        entry.setSoftDeletedAt(LocalDateTime.now());
        entry.setHardDeleteAfter(LocalDateTime.now().plusDays(gracePeriodDays));
        logger.info("[LIFECYCLE] Soft-deleted {} hardDeleteAfter={}", dataId, entry.getHardDeleteAfter());
    }

    /**
     * Restore a soft-deleted item within the grace period.
     *
     * @return true if successfully restored, false if item not found or past grace period
     */
    public boolean restore(String dataId) {
        LifecycleEntry entry = registry.get(dataId);
        if (entry == null || !entry.isSoftDeleted()) {
            logger.warn("[LIFECYCLE] restore: item not found or not soft-deleted: {}", dataId);
            return false;
        }
        if (LocalDateTime.now().isAfter(entry.getHardDeleteAfter())) {
            logger.warn("[LIFECYCLE] restore: grace period expired for {}", dataId);
            return false;
        }
        entry.setSoftDeleted(false);
        entry.setSoftDeletedAt(null);
        entry.setHardDeleteAfter(null);
        // Re-register with original TTL from expiresAt (remaining days)
        logger.info("[LIFECYCLE] Restored {}", dataId);
        return true;
    }

    /**
     * Check if a data item is still alive (not soft-deleted and not expired).
     */
    public boolean isActive(String dataId) {
        LifecycleEntry entry = registry.get(dataId);
        if (entry == null) return false;
        if (entry.isSoftDeleted()) return false;
        return LocalDateTime.now().isBefore(entry.getExpiresAt());
    }

    /**
     * Get all entries currently tracked (for admin review).
     */
    public List<LifecycleEntry> getAllEntries() {
        return new ArrayList<>(registry.values());
    }

    /**
     * Get summary stats for admin dashboard.
     */
    public Map<String, Object> getStats() {
        long total = registry.size();
        long active = registry.values().stream().filter(e -> !e.isSoftDeleted() && LocalDateTime.now().isBefore(e.getExpiresAt())).count();
        long softDeleted = registry.values().stream().filter(LifecycleEntry::isSoftDeleted).count();
        long expired = registry.values().stream().filter(e -> !e.isSoftDeleted() && LocalDateTime.now().isAfter(e.getExpiresAt())).count();
        return Map.of(
            "total", total,
            "active", active,
            "softDeleted", softDeleted,
            "expired", expired
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scheduled Cleanup Jobs
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Runs every hour. Soft-deletes items past their TTL expiry.
     */
    @Scheduled(fixedRateString = "${lifecycle.cleanup.interval.ms:3600000}")
    public void runExpiryJob() {
        LocalDateTime now = LocalDateTime.now();
        int expired = 0;
        for (LifecycleEntry entry : registry.values()) {
            if (!entry.isSoftDeleted() && now.isAfter(entry.getExpiresAt())) {
                softDelete(entry.getDataId());
                expired++;
            }
        }
        if (expired > 0) {
            logger.info("[LIFECYCLE] Expiry job: soft-deleted {} items", expired);
        }
    }

    /**
     * Runs every 6 hours. Hard-deletes items past their grace period.
     */
    @Scheduled(fixedRateString = "${lifecycle.harddelete.interval.ms:21600000}")
    public void runHardDeleteJob() {
        LocalDateTime now = LocalDateTime.now();
        List<String> toRemove = new ArrayList<>();
        for (LifecycleEntry entry : registry.values()) {
            if (entry.isSoftDeleted() && entry.getHardDeleteAfter() != null
                    && now.isAfter(entry.getHardDeleteAfter())) {
                toRemove.add(entry.getDataId());
            }
        }
        toRemove.forEach(id -> {
            registry.remove(id);
            logger.info("[LIFECYCLE] Hard-deleted {}", id);
        });
        if (!toRemove.isEmpty()) {
            logger.info("[LIFECYCLE] Hard-delete job: removed {} items", toRemove.size());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inner Model
    // ─────────────────────────────────────────────────────────────────────────

    public static class LifecycleEntry {
        private final String dataId;
        private final String dataType;
        private final LocalDateTime registeredAt;
        private LocalDateTime expiresAt;
        private boolean softDeleted;
        private LocalDateTime softDeletedAt;
        private LocalDateTime hardDeleteAfter;

        public LifecycleEntry(String dataId, String dataType, LocalDateTime expiresAt) {
            this.dataId = dataId;
            this.dataType = dataType;
            this.registeredAt = LocalDateTime.now();
            this.expiresAt = expiresAt;
            this.softDeleted = false;
        }

        public String getDataId() { return dataId; }
        public String getDataType() { return dataType; }
        public LocalDateTime getRegisteredAt() { return registeredAt; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
        public boolean isSoftDeleted() { return softDeleted; }
        public void setSoftDeleted(boolean softDeleted) { this.softDeleted = softDeleted; }
        public LocalDateTime getSoftDeletedAt() { return softDeletedAt; }
        public void setSoftDeletedAt(LocalDateTime softDeletedAt) { this.softDeletedAt = softDeletedAt; }
        public LocalDateTime getHardDeleteAfter() { return hardDeleteAfter; }
        public void setHardDeleteAfter(LocalDateTime hardDeleteAfter) { this.hardDeleteAfter = hardDeleteAfter; }
    }
}
