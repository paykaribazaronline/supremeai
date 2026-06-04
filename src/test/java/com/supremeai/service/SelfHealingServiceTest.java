package com.supremeai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SelfHealingServiceTest {

    @Mock
    private AIReasoningService reasoningService;

    private SelfHealingService selfHealingService;

    @BeforeEach
    void setUp() {
        selfHealingService = new SelfHealingService();
        setReasoningService(selfHealingService, reasoningService);
    }

    private void setReasoningService(SelfHealingService svc, AIReasoningService service) {
        try {
            java.lang.reflect.Field field = SelfHealingService.class.getDeclaredField("reasoningService");
            field.setAccessible(true);
            field.set(svc, service);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void executeWithRetry_shouldSucceedOnFirstAttempt() {
        Supplier<Mono<String>> taskSupplier = () -> Mono.just("success");

        StepVerifier.create(selfHealingService.executeWithRetry(taskSupplier, 3, 100))
                .expectNext("success")
                .verifyComplete();

        verifyNoInteractions(reasoningService);
    }

    @Test
    void executeWithRetry_shouldRetryOnFailureAndEventuallySucceed() {
        Supplier<Mono<String>> taskSupplier = new Supplier<Mono<String>>() {
            private int attempts = 0;

            @Override
            public Mono<String> get() {
                attempts++;
                if (attempts < 3) {
                    return Mono.error(new RuntimeException("Temporary failure"));
                }
                return Mono.just("success");
            }
        };

        StepVerifier.create(selfHealingService.executeWithRetry(taskSupplier, 3, 100))
                .expectNext("success")
                .verifyComplete();

        verifyNoInteractions(reasoningService);
    }

    @Test
    void executeWithRetry_shouldLogReasoningOnFinalFailure() {
        Supplier<Mono<String>> taskSupplier = () -> Mono.error(new RuntimeException("Persistent failure"));

        StepVerifier.create(selfHealingService.executeWithRetry(taskSupplier, 2, 100))
                .expectError(RuntimeException.class)
                .verify();

        verify(reasoningService).logReasoning(
                anyString(),
                eq("Execution Attempt Failed"),
                anyString(),
                eq("SelfHealingService")
        );
    }

    @Test
    void handleWorkflowFailure_shouldLogReasoningWithTruncatedError() {
        String longError = "A".repeat(200);

        selfHealingService.handleWorkflowFailure("test-repo", "workflow-123", longError);

        verify(reasoningService).logReasoning(
                eq("workflow-123"),
                eq("Self-Healing Triggered"),
                contains("..."),
                eq("SupremeAI-SelfHealer")
        );
    }

    @Test
    void handleWorkflowFailure_shouldLogReasoningWithFullErrorWhenShort() {
        String shortError = "Short error message";

        selfHealingService.handleWorkflowFailure("test-repo", "workflow-123", shortError);

        verify(reasoningService).logReasoning(
                eq("workflow-123"),
                eq("Self-Healing Triggered"),
                contains(shortError),
                eq("SupremeAI-SelfHealer")
        );
    }

    @Test
    void analyzeError_shouldReturnCorrectActionForDependencyError() {
        SelfHealingService service = new SelfHealingService();
        setReasoningService(service, reasoningService);

        // Use reflection to test private method
        try {
            java.lang.reflect.Method method = SelfHealingService.class.getDeclaredMethod("analyzeError", String.class);
            method.setAccessible(true);

            String result = (String) method.invoke(service, "Dependency resolution failed for some reason");
            assert result.equals("CHECK_DEPENDENCIES");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void analyzeError_shouldReturnCorrectActionForTestError() {
        SelfHealingService service = new SelfHealingService();
        setReasoningService(service, reasoningService);

        try {
            java.lang.reflect.Method method = SelfHealingService.class.getDeclaredMethod("analyzeError", String.class);
            method.setAccessible(true);

            String result = (String) method.invoke(service, "Tests failed with assertion error");
            assert result.equals("FIX_TESTS");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void analyzeError_shouldReturnCorrectActionForAuthError() {
        SelfHealingService service = new SelfHealingService();
        setReasoningService(service, reasoningService);

        try {
            java.lang.reflect.Method method = SelfHealingService.class.getDeclaredMethod("analyzeError", String.class);
            method.setAccessible(true);

            String result = (String) method.invoke(service, "Unauthorized access 401");
            assert result.equals("CHECK_AUTH_TOKENS");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void analyzeError_shouldReturnCorrectActionForQuotaError() {
        SelfHealingService service = new SelfHealingService();
        setReasoningService(service, reasoningService);

        try {
            java.lang.reflect.Method method = SelfHealingService.class.getDeclaredMethod("analyzeError", String.class);
            method.setAccessible(true);

            String result = (String) method.invoke(service, "Quota exceeded 429");
            assert result.equals("ROTATE_API_KEYS");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void analyzeError_shouldReturnGeneralCheckForUnknownError() {
        SelfHealingService service = new SelfHealingService();
        setReasoningService(service, reasoningService);

        try {
            java.lang.reflect.Method method = SelfHealingService.class.getDeclaredMethod("analyzeError", String.class);
            method.setAccessible(true);

            String result = (String) method.invoke(service, "Some unknown error occurred");
            assert result.equals("GENERAL_SYSTEM_CHECK");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void analyzeError_shouldReturnUnknownForNullInput() {
        SelfHealingService service = new SelfHealingService();
        setReasoningService(service, reasoningService);

        try {
            java.lang.reflect.Method method = SelfHealingService.class.getDeclaredMethod("analyzeError", String.class);
            method.setAccessible(true);

            String result = (String) method.invoke(service, (String) null);
            assert result.equals("UNKNOWN");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}