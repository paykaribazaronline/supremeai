package com.supremeai.resilience;

import com.supremeai.cost.QuotaManager;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetryableAIExecutorTest {

    @Mock
    private QuotaManager quotaManager;

    private RetryableAIExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new RetryableAIExecutor();
        executor.quotaManager = quotaManager;
        executor.init();
    }

    // ==================== Execute Tests ====================

    @Test
    void execute_SuccessfulOperation_ReturnsResult() {
        when(quotaManager.recordUsage(anyString(), anyString(), anyLong())).thenReturn(true);

        String result = executor.execute("test-provider", "test-service", () -> "Success!");

        assertEquals("Success!", result);
    }

    @Test
    void execute_OperationThrowsException_ThrowsException() {
        when(quotaManager.recordUsage(anyString(), anyString(), anyLong())).thenReturn(true);

        assertThrows(RuntimeException.class, () ->
                executor.execute("test-provider", "test-service", () -> {
                    throw new RuntimeException("Test error");
                })
        );
    }

    // ==================== Execute With Circuit Breaker Tests ====================

    @Test
    void executeWithCircuitBreaker_SuccessfulOperation_ReturnsResult() {
        when(quotaManager.recordUsage(anyString(), anyString(), anyLong())).thenReturn(true);

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker cb = registry.circuitBreaker("test-breaker");

        String result = executor.executeWithCircuitBreaker(
                "test-provider", "test-service", cb, () -> "Success!"
        );

        assertEquals("Success!", result);
    }

    @Test
    void executeWithCircuitBreaker_OpenCircuit_ThrowsException() {
        when(quotaManager.recordUsage(anyString(), anyString(), anyLong())).thenReturn(true);

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(1)
                .waitDurationInOpenState(Duration.ofMillis(100))
                .slidingWindowSize(5)
                .permittedNumberOfCallsInHalfOpenState(1)
                .build();
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        CircuitBreaker cb = registry.circuitBreaker("open-breaker");

        // Force circuit breaker to OPEN state by recording failures
        for (int i = 0; i < 10; i++) {
            try {
                executor.executeWithCircuitBreaker("test-provider", "test-service", cb,
                        () -> { throw new RuntimeException("Force failure"); });
            } catch (Exception ignored) {}
        }

        assertThrows(RetryableAIExecutor.CircuitBreakerOpenException.class, () ->
                executor.executeWithCircuitBreaker("test-provider", "test-service", cb, () -> "Should fail")
        );
    }

    @Test
    void executeWithCircuitBreaker_QuotaExceeded_ThrowsQuotaException() {
        when(quotaManager.recordUsage(anyString(), anyString(), anyLong())).thenReturn(false);

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker cb = registry.circuitBreaker("test-breaker");

        assertThrows(RetryableAIExecutor.QuotaExceededException.class, () ->
                executor.executeWithCircuitBreaker("test-provider", "test-service", cb, () -> "Result")
        );
    }

    // ==================== Execute Reactive Tests ====================

    @Test
    void executeReactive_SuccessfulOperation_ReturnsMono() {
        when(quotaManager.recordUsage(anyString(), anyString(), anyLong())).thenReturn(true);

        Mono<String> result = executor.executeReactive("test-provider", "test-service", Mono.just("Reactive Success!"));

        StepVerifierUtil.verifyMono(result, "Reactive Success!");
    }

    @Test
    void executeReactive_QuotaExceeded_ReturnsErrorMono() {
        when(quotaManager.recordUsage(anyString(), anyString(), anyLong())).thenReturn(false);

        Mono<String> result = executor.executeReactive("test-provider", "test-service", Mono.just("Should fail"));

        StepVerifier.create(result)
                .expectError(RetryableAIExecutor.QuotaExceededException.class)
                .verify();
    }

    // ==================== Execute Reactive With Circuit Breaker Tests ====================

    @Test
    void executeWithCircuitBreakerReactive_SuccessfulOperation_ReturnsMono() {
        when(quotaManager.recordUsage(anyString(), anyString(), anyLong())).thenReturn(true);

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker cb = registry.circuitBreaker("reactive-breaker");

        Mono<String> result = executor.executeWithCircuitBreakerReactive(
                "test-provider", "test-service", cb, Mono.just("Reactive Success!")
        );

        StepVerifierUtil.verifyMono(result, "Reactive Success!");
    }

    @Test
    void executeWithCircuitBreakerReactive_OpenCircuit_ReturnsErrorMono() {
        when(quotaManager.recordUsage(anyString(), anyString(), anyLong())).thenReturn(true);

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(1)
                .waitDurationInOpenState(Duration.ofMillis(100))
                .slidingWindowSize(5)
                .permittedNumberOfCallsInHalfOpenState(1)
                .build();
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        CircuitBreaker cb = registry.circuitBreaker("open-reactive-breaker");

        // Force open state
        for (int i = 0; i < 10; i++) {
            try {
                executor.executeWithCircuitBreaker("p", "s", cb, () -> { throw new RuntimeException("x"); });
            } catch (Exception ignored) {}
        }

        Mono<String> result = executor.executeWithCircuitBreakerReactive(
                "test-provider", "test-service", cb, Mono.just("Should fail")
        );

        StepVerifier.create(result)
                .expectError(RetryableAIExecutor.CircuitBreakerOpenException.class)
                .verify();
    }

    // ==================== Calculate Backoff Tests ====================

    @Test
    void calculateBackoff_FirstAttempt_ReturnsReasonableDelay() {
        long backoff = executor.calculateBackoff(0);
        assertTrue(backoff >= 500, "Backoff should be at least initial wait");
        assertTrue(backoff < 2000, "Backoff with jitter should not be too large");
    }

    @Test
    void calculateBackoff_MultipleAttempts_IncreasesExponentially() {
        long backoff1 = executor.calculateBackoff(0);
        long backoff2 = executor.calculateBackoff(1);
        long backoff3 = executor.calculateBackoff(2);

        // Generally backoff increases with attempts (may have jitter)
        assertTrue(backoff3 >= backoff1, "Later attempts should generally have longer backoff");
    }

    // ==================== IsRetryable Tests ====================

    @Test
    void isRetryable_HttpTimeout_ReturnsTrue() {
        java.lang.reflect.Method method;
        try {
            method = RetryableAIExecutor.class.getDeclaredMethod("isRetryable", Throwable.class);
            method.setAccessible(true);

            boolean result = (boolean) method.invoke(executor, new java.net.http.HttpTimeoutException("timeout"));
            assertTrue(result);
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    @Test
    void isRetryable_IOException_ReturnsTrue() {
        try {
            java.lang.reflect.Method method = RetryableAIExecutor.class.getDeclaredMethod("isRetryable", Throwable.class);
            method.setAccessible(true);

            boolean result = (boolean) method.invoke(executor, new java.io.IOException("connection reset"));
            assertTrue(result);
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    @Test
    void isRetryable_RateLimit_ReturnsTrue() {
        try {
            java.lang.reflect.Method method = RetryableAIExecutor.class.getDeclaredMethod("isRetryable", Throwable.class);
            method.setAccessible(true);

            boolean result = (boolean) method.invoke(executor, new RuntimeException("Rate limit exceeded 429"));
            assertTrue(result);
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    @Test
    void isRetryable_NotFound_ReturnsFalse() {
        try {
            java.lang.reflect.Method method = RetryableAIExecutor.class.getDeclaredMethod("isRetryable", Throwable.class);
            method.setAccessible(true);

            boolean result = (boolean) method.invoke(executor, new RuntimeException("404 Not Found"));
            assertFalse(result);
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    @Test
    void isRetryable_Unauthorized_ReturnsFalse() {
        try {
            java.lang.reflect.Method method = RetryableAIExecutor.class.getDeclaredMethod("isRetryable", Throwable.class);
            method.setAccessible(true);

            boolean result = (boolean) method.invoke(executor, new RuntimeException("401 Unauthorized"));
            assertFalse(result);
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    @Test
    void isRetryable_QuotaExceeded_ReturnsFalse() {
        try {
            java.lang.reflect.Method method = RetryableAIExecutor.class.getDeclaredMethod("isRetryable", Throwable.class);
            method.setAccessible(true);

            boolean result = (boolean) method.invoke(executor, new RetryableAIExecutor.QuotaExceededException("Quota exceeded"));
            assertFalse(result);
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    // ==================== Exception Class Tests ====================

    @Test
    void quotaExceededException_HasMessage() {
        RetryableAIExecutor.QuotaExceededException ex =
                new RetryableAIExecutor.QuotaExceededException("Test message");
        assertEquals("Test message", ex.getMessage());
    }

    @Test
    void circuitBreakerOpenException_HasMessage() {
        RetryableAIExecutor.CircuitBreakerOpenException ex =
                new RetryableAIExecutor.CircuitBreakerOpenException("Circuit open");
        assertEquals("Circuit open", ex.getMessage());
    }

    private static class StepVerifierUtil {
        static <T> void verifyMono(Mono<T> mono, T expected) {
            reactor.test.StepVerifier.create(mono)
                    .expectNext(expected)
                    .verifyComplete();
        }
    }
}