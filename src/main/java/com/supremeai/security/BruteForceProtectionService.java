package com.supremeai.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to prevent brute force login attacks.
 * Tracks failed login attempts and locks accounts after exceeding threshold.
 */
@Service
public class BruteForceProtectionService {

    private static final Logger log = LoggerFactory.getLogger(BruteForceProtectionService.class);

    @Value("${bruteforce.max-attempts:5}")
    private int maxAttempts;

    @Value("${bruteforce.lock-duration-minutes:15}")
    private int lockDurationMinutes;

    // In-memory store: key = email or IP, value = AttemptRecord
    private final Map<String, AttemptRecord> attemptsCache = new ConcurrentHashMap<>();

    private static class AttemptRecord {
        int count;
        long firstAttemptTime; // epoch millis
        long lockUntil; // epoch millis, 0 if not locked

        AttemptRecord() {
            this.count = 1;
            this.firstAttemptTime = Instant.now().toEpochMilli();
            this.lockUntil = 0;
        }

        void increment() {
            count++;
        }

        boolean isLocked() {
            return lockUntil > Instant.now().toEpochMilli();
        }

        void lock(long durationMillis) {
            this.lockUntil = Instant.now().toEpochMilli() + durationMillis;
        }

        void reset() {
            this.count = 0;
            this.firstAttemptTime = 0;
            this.lockUntil = 0;
        }
    }

    /**
     * Check if an email or IP is locked out.
     * @param identifier Email or IP address
     * @return true if locked out
     */
    public boolean isLocked(String identifier) {
        AttemptRecord record = attemptsCache.get(identifier);
        if (record == null) return false;

        if (record.isLocked()) {
            log.warn("Account/IP {} is locked until {}", identifier, Instant.ofEpochMilli(record.lockUntil));
            return true;
        }

        // Clean up expired records
        if (!record.isLocked() && record.count == 0) {
            attemptsCache.remove(identifier);
        }

        return false;
    }

    /**
     * Record a failed login attempt.
     * @param identifier Email or IP address
     */
    public void recordFailedAttempt(String identifier) {
        AttemptRecord record = attemptsCache.computeIfAbsent(identifier, k -> new AttemptRecord());

        if (record.isLocked()) {
            // Already locked, extend lock
            record.lockUntil = Instant.now().toEpochMilli() + (lockDurationMinutes * 60 * 1000L);
            log.warn("Extended lock for {} due to additional failed attempts", identifier);
            return;
        }

        record.increment();

        if (record.count >= maxAttempts) {
            record.lock(lockDurationMinutes * 60 * 1000L);
            log.warn("Account/IP {} locked for {} minutes after {} failed attempts", identifier, lockDurationMinutes, record.count);
        }
    }

    /**
     * Reset failed attempts on successful login.
     * @param identifier Email or IP address
     */
    public void resetAttempts(String identifier) {
        AttemptRecord record = attemptsCache.get(identifier);
        if (record != null) {
            record.reset();
            attemptsCache.remove(identifier);
            log.debug("Reset failed attempts for {}", identifier);
        }
    }

    /**
     * Get remaining lock time in seconds.
     */
    public long getRemainingLockTimeSeconds(String identifier) {
        AttemptRecord record = attemptsCache.get(identifier);
        if (record == null || !record.isLocked()) return 0;

        return (record.lockUntil - Instant.now().toEpochMilli()) / 1000;
    }
}
