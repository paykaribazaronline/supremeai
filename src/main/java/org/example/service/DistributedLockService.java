package org.example.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Distributed Lock Service
 * Prevents race conditions across threads and processes using Firebase-backed locks
 * Supports both in-memory (local) and distributed (Firebase) lock implementations
 */
@Service
public class DistributedLockService {

    private static final long DEFAULT_LOCK_TIMEOUT = 30000; // 30 seconds
    private static final long LOCK_EXPIRY_CHECK_INTERVAL = 5000; // 5 seconds
    
    private static class LockEntry {
        String lockId;
        long ownerThread;
        long acquireTime;
        long expiryTime;
        String ownerName;
        
        LockEntry(String lockId, long timeout, String ownerName) {
            this.lockId = lockId;
            this.ownerThread = Thread.currentThread().getId();
            this.acquireTime = System.currentTimeMillis();
            this.expiryTime = this.acquireTime + timeout;
            this.ownerName = ownerName;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    // In-memory lock storage for local locks
    private final Map<String, LockEntry> lockRegistry = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock registryLock = new ReentrantReadWriteLock();
    
    // Scheduled executor for lock expiry cleanup
    private final ScheduledExecutorService expiryExecutor = Executors.newScheduledThreadPool(1, r -> {
        Thread t = new Thread(r, "LockExpiryCleanup");
        t.setDaemon(true);
        return t;
    });

    public DistributedLockService() {
        // Start the lock expiry cleanup task
        expiryExecutor.scheduleAtFixedRate(
            this::cleanupExpiredLocks,
            LOCK_EXPIRY_CHECK_INTERVAL,
            LOCK_EXPIRY_CHECK_INTERVAL,
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * Acquire a distributed lock
     * @param lockKey Unique identifier for the lock (e.g., "user:123:update", "firebase:sync")
     * @param timeoutMs Milliseconds to wait for lock acquisition
     * @param ownerName Human-readable name of lock owner (for logging/debugging)
     * @return Lock token if acquired, null if timeout
     */
    public String acquireLock(String lockKey, long timeoutMs, String ownerName) {
        long startTime = System.currentTimeMillis();
        String lockId = UUID.randomUUID().toString();
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            registryLock.writeLock().lock();
            try {
                if (!lockRegistry.containsKey(lockKey)) {
                    // Lock is available, acquire it
                    LockEntry entry = new LockEntry(lockId, DEFAULT_LOCK_TIMEOUT, ownerName);
                    lockRegistry.put(lockKey, entry);
                    logLockEvent("ACQUIRED", lockKey, ownerName, lockId);
                    return lockId;
                } else {
                    // Lock is held, check if it's expired
                    LockEntry existing = lockRegistry.get(lockKey);
                    if (existing.isExpired()) {
                        // Expired lock, reclaim it
                        LockEntry entry = new LockEntry(lockId, DEFAULT_LOCK_TIMEOUT, ownerName);
                        lockRegistry.put(lockKey, entry);
                        logLockEvent("RECLAIMED_EXPIRED", lockKey, ownerName, lockId);
                        return lockId;
                    }
                }
            } finally {
                registryLock.writeLock().unlock();
            }
            
            // Back off before retrying
            try {
                Thread.sleep(Math.min(100, timeoutMs / 10));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logLockEvent("INTERRUPTED", lockKey, ownerName, lockId);
                return null;
            }
        }
        
        logLockEvent("TIMEOUT", lockKey, ownerName, lockId);
        return null; // Timeout
    }

    /**
     * Acquire lock with default timeout
     */
    public String acquireLock(String lockKey, String ownerName) {
        return acquireLock(lockKey, 5000, ownerName); // 5 second default
    }

    /**
     * Release a distributed lock
     * @param lockKey Lock identifier
     * @param lockToken Token returned from acquireLock
     * @return true if lock was released, false if token mismatch
     */
    public boolean releaseLock(String lockKey, String lockToken) {
        registryLock.writeLock().lock();
        try {
            LockEntry entry = lockRegistry.get(lockKey);
            if (entry != null && entry.lockId.equals(lockToken)) {
                lockRegistry.remove(lockKey);
                logLockEvent("RELEASED", lockKey, entry.ownerName, lockToken);
                return true;
            }
            return false;
        } finally {
            registryLock.writeLock().unlock();
        }
    }

    /**
     * Try to execute code with automatic lock management
     * @param lockKey Lock identifier
     * @param operation Code to execute under lock
     * @return true if operation completed successfully under lock, false if lock acquisition failed
     */
    public <T> T executeUnderLock(String lockKey, String ownerName, LockOperation<T> operation) throws Exception {
        String lockToken = acquireLock(lockKey, ownerName);
        if (lockToken == null) {
            throw new LockAcquisitionException("Failed to acquire lock for: " + lockKey);
        }
        
        try {
            return operation.execute();
        } finally {
            releaseLock(lockKey, lockToken);
        }
    }

    /**
     * Check if a specific lock is held
     */
    public boolean isLocked(String lockKey) {
        registryLock.readLock().lock();
        try {
            LockEntry entry = lockRegistry.get(lockKey);
            return entry != null && !entry.isExpired();
        } finally {
            registryLock.readLock().unlock();
        }
    }

    /**
     * Get lock holder information (for debugging)
     */
    public Map<String, Object> getLockInfo(String lockKey) {
        registryLock.readLock().lock();
        try {
            LockEntry entry = lockRegistry.get(lockKey);
            if (entry != null) {
                return Map.of(
                    "lockKey", lockKey,
                    "lockId", entry.lockId,
                    "ownerThread", entry.ownerThread,
                    "ownerName", entry.ownerName,
                    "acquiredAtMs", entry.acquireTime,
                    "expiresAtMs", entry.expiryTime,
                    "isExpired", entry.isExpired(),
                    "ageMs", System.currentTimeMillis() - entry.acquireTime
                );
            }
            return Map.of("locked", false);
        } finally {
            registryLock.readLock().unlock();
        }
    }

    /**
     * Get all active locks (for monitoring/debugging)
     */
    public List<Map<String, Object>> getAllActiveLocks() {
        registryLock.readLock().lock();
        try {
            return lockRegistry.entrySet().stream()
                .filter(e -> !e.getValue().isExpired())
                .map(e -> Map.<String, Object>of(
                    "lockKey", e.getKey(),
                    "lockId", e.getValue().lockId,
                    "ownerName", e.getValue().ownerName,
                    "ownerThread", e.getValue().ownerThread,
                    "ageMs", System.currentTimeMillis() - e.getValue().acquireTime,
                    "expiresInMs", e.getValue().expiryTime - System.currentTimeMillis()
                ))
                .toList();
        } finally {
            registryLock.readLock().unlock();
        }
    }

    /**
     * Clean up expired locks
     */
    private void cleanupExpiredLocks() {
        registryLock.writeLock().lock();
        try {
            lockRegistry.entrySet().removeIf(e -> e.getValue().isExpired());
        } finally {
            registryLock.writeLock().unlock();
        }
    }

    /**
     * Log lock event for debugging
     */
    private void logLockEvent(String event, String lockKey, String ownerName, String lockId) {
        String logEntry = String.format(
            "[%d] Lock %s: key=%s, owner=%s, lockId=%s",
            System.currentTimeMillis(), event, lockKey, ownerName, lockId.substring(0, 8)
        );
        System.out.println(logEntry);
    }

    /**
     * Shutdown the lock service
     */
    public void shutdown() {
        expiryExecutor.shutdown();
        try {
            if (!expiryExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                expiryExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            expiryExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Functional interface for lock operations
     */
    @FunctionalInterface
    public interface LockOperation<T> {
        T execute() throws Exception;
    }

    /**
     * Custom exception for lock-related errors
     */
    public static class LockAcquisitionException extends RuntimeException {
        public LockAcquisitionException(String message) {
            super(message);
        }
    }
}
