package com.supremeai.selfhealing;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;

import com.supremeai.service.AIReasoningService;
import com.supremeai.service.SelfHealingService;
import java.time.Duration;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class SelfHealingServiceTest {

  @Mock private AIReasoningService reasoningService;

  @InjectMocks
  private SelfHealingService service;

  @Test
  void testExecuteWithRetry_SuccessOnFirstTry() {
    // Given
    lenient()
        .doNothing()
        .when(reasoningService)
        .logReasoning(anyString(), anyString(), anyString(), anyString());
    Supplier<Mono<String>> task = () -> Mono.just("success");
    // Use lenient to avoid UnnecessaryStubbingException since doOnError won't be called on success
    // path
    lenient()
        .doNothing()
        .when(reasoningService)
        .logReasoning(anyString(), anyString(), anyString(), anyString());

    // When

    // When
    Mono<String> result = service.executeWithRetry(task, 3, 10);

    // Then
    StepVerifier.create(result).expectNext("success").verifyComplete();
  }

  @Test
  void testExecuteWithRetry_SucceedsAfterFailure() {
    // Given

    // Use a simple flag that fails once then succeeds
    // With maxAttempts=2, we have 1 retry (2 total attempts)
    boolean[] firstAttempt = {true};
    Supplier<Mono<String>> task =
        () -> {
          if (firstAttempt[0]) {
            firstAttempt[0] = false;
            return Mono.error(new RuntimeException("fail"));
          }
          return Mono.just("ok");
        };

    // When - maxAttempts=2 means 1 retry (2 total attempts)
    Mono<String> result = service.executeWithRetry(task, 2, 10);

    // Then
    StepVerifier.create(result).expectNext("ok").verifyComplete();
  }

  @Test
  void testExecuteWithRetry_ThrowsAfterMaxAttempts() {
    // Given
    doNothing()
        .when(reasoningService)
        .logReasoning(anyString(), anyString(), anyString(), anyString());

    Supplier<Mono<String>> task = () -> Mono.error(new RuntimeException("always fail"));

    // When - maxAttempts=3 means 2 retries (3 total attempts)
    Mono<String> result = service.executeWithRetry(task, 3, 1);

    // Then
    StepVerifier.create(result).expectError(RuntimeException.class).verify(Duration.ofSeconds(5));
  }
}
