package com.supremeai.selfhealing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;

import com.supremeai.service.AIReasoningService;
import com.supremeai.service.SelfHealingService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SelfHealingServiceTest {

    private final SelfHealingService service = new SelfHealingService();

    @Test
    void testExecuteWithRetry_SuccessOnFirstTry() throws Exception {
        // Given
        Callable<String> task = () -> "success";

        // When
        String result = service.executeWithRetry(task, 3, 10);

        // Then
        assertEquals("success", result);
    }

    @Test
    void testExecuteWithRetry_SucceedsAfterFailure() throws Exception {
        // Given
        java.util.concurrent.atomic.AtomicInteger attempts = new java.util.concurrent.atomic.AtomicInteger(0);

        // When
        String result = service.executeWithRetry(() -> {
            attempts.incrementAndGet();
            if (attempts.get() < 3) {
                throw new RuntimeException("fail");
            }
            return "ok";
        }, 3, 1);

        // Then
        assertEquals("ok", result);
        assertEquals(3, attempts.get());
    }

    @Test
    void testExecuteWithRetry_ThrowsAfterMaxAttempts() {
        // Given
        java.util.concurrent.atomic.AtomicInteger attempts = new java.util.concurrent.atomic.AtomicInteger(0);

        // When / Then
        assertThrows(RuntimeException.class, () -> {
            service.executeWithRetry(() -> {
                attempts.incrementAndGet();
                throw new RuntimeException("always fail");
            }, 2, 1);
        });
        assertEquals(2, attempts.get());
    }

    @Test
    void testRunWithRetry_Simple() throws Exception {
        // Given
        java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(0);

        // When
        service.runWithRetry(() -> count.incrementAndGet(), 3, 1);

        // Then
        assertEquals(1, count.get());
    }
}
