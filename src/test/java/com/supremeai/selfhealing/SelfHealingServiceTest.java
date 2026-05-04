package com.supremeai.selfhealing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import com.supremeai.service.AIReasoningService;
import com.supremeai.service.SelfHealingService;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class SelfHealingServiceTest {

    @Mock
    private AIReasoningService reasoningService;

    private SelfHealingService service;

    @Test
    void testExecuteWithRetry_SuccessOnFirstTry() {
        // Given
        service = new SelfHealingService();
        service.setReasoningService(reasoningService);
        // Use lenient to avoid UnnecessaryStubbingException since doOnError won't be called on success path
        lenient().doNothing().when(reasoningService).logReasoning(
            anyString(), anyString(), anyString(), anyString());
        Supplier<Mono<String>> task = () -> Mono.just("success");

        // When
        Mono<String> result = service.executeWithRetry(task, 3, 10);

        // Then
        StepVerifier.create(result)
            .expectNext("success")
            .verifyComplete();
    }

    @Test
    void testExecuteWithRetry_SucceedsAfterFailure() {
        // Given
        service = new SelfHealingService();
        service.setReasoningService(reasoningService);
        lenient().doNothing().when(reasoningService).logReasoning(
            anyString(), anyString(), anyString(), anyString());
        
        AtomicInteger attempts = new AtomicInteger(0);
        Supplier<Mono<String>> task = () -> {
            int attemptNum = attempts.incrementAndGet();
            if (attemptNum < 3) {
                return Mono.error(new RuntimeException("fail"));
            }
            return Mono.just("ok");
        };

        // When
        Mono<String> result = service.executeWithRetry(task, 5, 1);

        // Then
        StepVerifier.create(result)
            .expectNext("ok")
            .verifyComplete();
    }

    @Test
    void testExecuteWithRetry_ThrowsAfterMaxAttempts() {
        // Given
        service = new SelfHealingService();
        service.setReasoningService(reasoningService);
        lenient().doNothing().when(reasoningService).logReasoning(
            anyString(), anyString(), anyString(), anyString());
        
        AtomicInteger attempts = new AtomicInteger(0);
        Supplier<Mono<String>> task = () -> {
            attempts.incrementAndGet();
            return Mono.error(new RuntimeException("always fail"));
        };

        // When
        Mono<String> result = service.executeWithRetry(task, 3, 1);

        // Then
        StepVerifier.create(result)
            .expectError(RuntimeException.class)
            .verify(Duration.ofSeconds(5));
    }

    private static <T> T anyString() {
        return (T) org.mockito.ArgumentMatchers.anyString();
    }
}
