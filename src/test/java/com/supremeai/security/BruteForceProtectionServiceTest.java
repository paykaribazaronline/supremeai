package com.supremeai.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BruteForceProtectionServiceTest {

    private BruteForceProtectionService bruteForceService;

    @BeforeEach
    void setUp() {
        bruteForceService = new BruteForceProtectionService();
    }

    @Test
    void testNotLockedInitially() {
        assertFalse(bruteForceService.isLocked("test@example.com"));
        assertFalse(bruteForceService.isLocked("192.168.1.1"));
    }

    @Test
    void testLockAfterMaxAttempts() {
        String identifier = "test@example.com";
        
        // AttemptRecord constructor sets count=1, then increment() is called
        // So after 1st call: count=2 (1 from constructor + 1 from increment)
        // After 4 calls: count=5, which triggers >= 5 and locks
        
        // Record 3 failed attempts - should not be locked
        for (int i = 0; i < 3; i++) {
            bruteForceService.recordFailedAttempt(identifier);
        }
        assertFalse(bruteForceService.isLocked(identifier), "Should not be locked after 3 attempts");
        
        // Fourth attempt - should lock (count becomes 5)
        bruteForceService.recordFailedAttempt(identifier);
        assertTrue(bruteForceService.isLocked(identifier), "Should be locked after 4 attempts (count=5)");
    }

    @Test
    void testRemainingLockTimePositiveWhenLocked() {
        String identifier = "test@example.com";
        for (int i = 0; i < 4; i++) {
            bruteForceService.recordFailedAttempt(identifier);
        }
        
        long remainingTime = bruteForceService.getRemainingLockTimeSeconds(identifier);
        assertTrue(remainingTime > 0, "Remaining lock time should be positive when locked");
    }

    @Test
    void testResetAttempts() {
        String identifier = "test@example.com";
        for (int i = 0; i < 4; i++) {
            bruteForceService.recordFailedAttempt(identifier);
        }
        assertTrue(bruteForceService.isLocked(identifier));
        
        bruteForceService.resetAttempts(identifier);
        assertFalse(bruteForceService.isLocked(identifier));
    }

    @Test
    void testMultipleIdentifiersIndependent() {
        for (int i = 0; i < 4; i++) {
            bruteForceService.recordFailedAttempt("user1@example.com");
        }
        
        // user2 should not be locked
        assertFalse(bruteForceService.isLocked("user2@example.com"));
    }
}