package org.example.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RateLimitingService Tests")
public class RateLimitingServiceTest {
    
    private RateLimitingService rateLimiter;
    
    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimitingService(10, 100); // 10 per minute, 100 per hour
    }
    
    @Test
    @DisplayName("Should allow requests within limit")
    void testAllowRequestsWithinLimit() {
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimiter.allowUserRequest("user1"));
        }
    }
    
    @Test
    @DisplayName("Should deny requests exceeding limit")
    void testDenyRequestsExceedingLimit() {
        // Use up all tokens
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimiter.allowUserRequest("user1"));
        }
        
        // Next request should be denied
        assertFalse(rateLimiter.allowUserRequest("user1"));
    }
    
    @Test
    @DisplayName("Should track remaining tokens")
    void testTrackRemainingTokens() {
        assertEquals(10, rateLimiter.getUserRemainingTokens("user2"));
        
        rateLimiter.allowUserRequest("user2");
        assertEquals(9, rateLimiter.getUserRemainingTokens("user2"));
        
        rateLimiter.allowUserRequest("user2");
        assertEquals(8, rateLimiter.getUserRemainingTokens("user2"));
    }
    
    @Test
    @DisplayName("Should allow reset of user limits")
    void testResetUserLimit() {
        // Use up all tokens
        for (int i = 0; i < 10; i++) {
            rateLimiter.allowUserRequest("user3");
        }
        assertFalse(rateLimiter.allowUserRequest("user3"));
        
        // Reset limit
        rateLimiter.resetUserLimit("user3");
        assertTrue(rateLimiter.allowUserRequest("user3"));
    }
    
    @Test
    @DisplayName("Should isolate limits per user")
    void testIsolateLimitsPerUser() {
        rateLimiter.allowUserRequest("userA");
        
        // userB should not be affected
        assertEquals(10, rateLimiter.getUserRemainingTokens("userB"));
    }
}
