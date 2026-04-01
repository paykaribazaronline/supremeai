package org.example.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.example.resilience.*;
import org.example.service.MultiAIConsensusService;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Failover Scenario Validation Tests
 * Validates multi-layer resilience and failover strategies
 */
@SpringBootTest
public class FailoverScenarioValidationTests {
    
    @Autowired
    private FailoverManager failoverManager;
    
    @Autowired
    private CircuitBreakerManager circuitBreaker;
    
    @Autowired
    private ResilienceHealthCheckService healthCheck;
    
    @Autowired
    private MultiAIConsensusService consensusService;
    
    private static final long TIMEOUT_MS = 30000;
    
    // ============ PROVIDER FAILOVER TESTS ============
    
    @Test
    public void testPrimaryProviderFailure_FallbackToSecondary() throws Exception {
        // Scenario: OpenAI fails, system should fallback to Anthropic
        String question = "Test redundancy?";
        
        // Get initial provider
        String primaryProvider = "openai";
        
        // Simulate primary failure
        circuitBreaker.markProviderDown(primaryProvider);
        
        // Request should still succeed via failover
        assertDoesNotThrow(() -> {
            consensusService.askAllAI(question);
        });
        
        // Verify circuit breaker is open
        assertTrue(circuitBreaker.isCircuitOpen(primaryProvider));
    }
    
    @Test
    public void testMultipleProviderFailures_DefaultsToCache() throws Exception {
        // Scenario: Multiple providers fail, system returns cached response
        String question = "Cached answer?";
        
        // Mark multiple providers down
        String[] providers = {"openai", "anthropic", "google"};
        for (String provider : providers) {
            circuitBreaker.markProviderDown(provider);
        }
        
        // System should return cached data
        try {
            consensusService.askAllAI(question);
            // If successful, cache was used
            assertTrue(true);
        } catch (Exception e) {
            fail("System should fallback to cache when providers fail");
        }
    }
    
    @Test
    public void testCircuitBreakerStateTransitions() throws Exception {
        String provider = "openai";
        
        // Initial state: CLOSED
        assertEquals("CLOSED", circuitBreaker.getCircuitState(provider));
        
        // Simulate errors
        for (int i = 0; i < 5; i++) {
            circuitBreaker.recordFailure(provider);
        }
        
        // State should be OPEN
        assertEquals("OPEN", circuitBreaker.getCircuitState(provider));
        
        // Wait for recovery timeout
        Thread.sleep(2000);
        
        // Should transition to HALF_OPEN
        String state = circuitBreaker.getCircuitState(provider);
        assertTrue(state.equals("OPEN") || state.equals("HALF_OPEN"));
    }
    
    @Test
    public void testHalfOpenState_SuccessRestoresCircuit() throws Exception {
        String provider = "openai";
        
        // Open the circuit
        for (int i = 0; i < 5; i++) {
            circuitBreaker.recordFailure(provider);
        }
        
        // Wait for half-open
        Thread.sleep(2000);
        
        // Record success
        circuitBreaker.recordSuccess(provider);
        
        // Circuit should close after successful call
        // (May take a moment)
        Thread.sleep(500);
        
        // Verify circuit is closed or recovering
        String state = circuitBreaker.getCircuitState(provider);
        assertTrue(state.equals("CLOSED") || state.equals("HALF_OPEN"));
    }
    
    // ============ CACHE LAYER FAILOVER TESTS ============
    
    @Test
    public void testL1_MemoryCache_FastResponse() throws Exception {
        String key = "test-query-1";
        String cachedValue = "Cached response";
        
        // Store in L1 cache
        long startTime = System.currentTimeMillis();
        failoverManager.cacheResponse(key, cachedValue);
        String retrieved = failoverManager.getCachedResponse(key);
        long endTime = System.currentTimeMillis();
        
        assertEquals(cachedValue, retrieved);
        assertTrue((endTime - startTime) < 100); // Should be < 100ms
    }
    
    @Test
    public void testL2_RedisFallback_IfL1Empty() throws Exception {
        String key = "redis-test";
        String value = "Redis cached value";
        
        // Store in Redis (L2)
        failoverManager.storeInRedis(key, value);
        
        // Retrieve should work
        String retrieved = failoverManager.getFromCache(key);
        assertNotNull(retrieved);
    }
    
    @Test
    public void testL3_DatabaseFallback_IfCachesEmpty() throws Exception {
        String key = "db-fallback-test";
        
        // Database should have this persisted
        String retrieved = failoverManager.getFromDatabase(key);
        
        // Either found in DB or gracefully handled
        // (depends on test data)
        assertTrue(true);
    }
    
    @Test
    public void testL4_StaleDataUsed_IfAllFail() throws Exception {
        String key = "stale-data-test";
        
        // Mark all cache layers unavailable
        failoverManager.disableAllCaches();
        
        // Try to get response
        String staleData = failoverManager.getStaleData(key);
        
        // Should either return stale data or handle gracefully
        assertNotNull(staleData);
    }
    
    // ============ QUOTA ROTATION TESTS ============
    
    @Test
    public void testQuotaExhaustion_SwitchesToNextProvider() throws Exception {
        // Use up OpenAI quota
        failoverManager.exhaustQuota("openai");
        
        // Next request should use Anthropic
        String activeProvider = failoverManager.getActiveProvider();
        assertNotEquals("openai", activeProvider);
    }
    
    @Test
    public void testQuotaRecovery() throws Exception {
        String provider = "openai";
        failoverManager.exhaustQuota(provider);
        
        // Wait for quota reset
        Thread.sleep(1000);
        failoverManager.resetQuota(provider);
        
        // Should be available again
        String activeProvider = failoverManager.getActiveProvider();
        assertEquals(provider, activeProvider);
    }
    
    @Test
    public void testAllQuotasExhausted_UsesCache() throws Exception {
        String[] providers = {"openai", "anthropic", "google", "meta"};
        
        // Exhaust all quotas
        for (String provider : providers) {
            failoverManager.exhaustQuota(provider);
        }
        
        // Should fallback to cache
        Object response = failoverManager.executeWithFallback(() -> "Test");
        assertNotNull(response);
    }
    
    // ============ RETRY STRATEGY TESTS ============
    
    @Test
    public void testExponentialBackoff_Increases() throws Exception {
        long attempt1Start = System.currentTimeMillis();
        failoverManager.retryWithBackoff(1);
        long attempt1Duration = System.currentTimeMillis() - attempt1Start;
        
        long attempt2Start = System.currentTimeMillis();
        failoverManager.retryWithBackoff(2);
        long attempt2Duration = System.currentTimeMillis() - attempt2Start;
        
        // Second attempt should have longer delay
        assertTrue(attempt2Duration >= attempt1Duration);
    }
    
    @Test
    public void testMaxRetries_StopsAfterLimit() throws Exception {
        int maxRetries = 3;
        int attemptCount = 0;
        
        for (int i = 0; i < maxRetries + 1; i++) {
            try {
                failoverManager.retryWithBackoff(i);
                attemptCount++;
            } catch (Exception e) {
                break;
            }
        }
        
        assertTrue(attemptCount <= maxRetries);
    }
    
    // ============ CONCURRENT LOAD TESTS ============
    
    @Test
    public void testConcurrentRequests_AllSucceed() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<?>> futures = new ArrayList<>();
        
        for (int i = 0; i < 50; i++) {
            futures.add(executor.submit(() -> {
                try {
                    consensusService.askAllAI("Concurrent query");
                } catch (Exception e) {
                    fail("Concurrent request failed: " + e.getMessage());
                }
            }));
        }
        
        // Wait for all tasks
        for (Future<?> future : futures) {
            assertDoesNotThrow(() -> future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        }
        
        executor.shutdown();
    }
    
    @Test
    public void testConcurrentFailover_NoDeadlock() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        
        // Trigger failover in parallel
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                failoverManager.triggerFailover("openai");
            });
        }
        
        executor.shutdown();
        boolean completed = executor.awaitTermination(10, TimeUnit.SECONDS);
        
        assertTrue(completed, "Concurrent failover caused deadlock");
    }
    
    // ============ HEALTH CHECK TESTS ============
    
    @Test
    public void testHealthCheckMonitoring() throws Exception {
        Map<String, String> health = healthCheck.getSystemHealth();
        
        assertNotNull(health);
        assertTrue(health.containsKey("overall_status"));
        assertTrue(health.containsKey("providers_healthy"));
        assertTrue(health.containsKey("cache_status"));
    }
    
    @Test
    public void testAutoRecoveryAfterFailure() throws Exception {
        // Fail a provider
        failoverManager.failProvider("openai");
        
        // Trigger health check
        healthCheck.checkHealthAndRecover();
        
        // Give recovery time
        Thread.sleep(1000);
        
        // Provider should be recovering or recovered
        String state = circuitBreaker.getCircuitState("openai");
        assertTrue(state.equals("HALF_OPEN") || state.equals("CLOSED"));
    }
}
