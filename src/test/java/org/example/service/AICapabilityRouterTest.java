package org.example.service;

import org.example.service.AICapabilityRouter.TaskType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AICapabilityRouterTest {

    @Mock
    private AIAPIService aiApiService;

    @Mock
    private QuotaService quotaService;

    @InjectMocks
    private AICapabilityRouter router;

    @Test
    void routePrefersCapabilityOrderWhenAvailable() {
        when(quotaService.getAvailableProviders()).thenReturn(
            List.of("anthropic-claude", "openai-gpt4", "deepseek")
        );

        AICapabilityRouter.RoutingDecision decision = router.route(TaskType.CODE_GENERATION, "Build API endpoint");

        assertTrue(decision.isSuccess());
        assertEquals("openai-gpt4", decision.getProvider());
    }

    @Test
    void routeFallsBackToAvailableProviderWhenPreferredMissing() {
        when(quotaService.getAvailableProviders()).thenReturn(
            List.of("cohere", "perplexity")
        );

        AICapabilityRouter.RoutingDecision decision = router.route(TaskType.SECURITY_REVIEW, "Review auth flow");

        assertTrue(decision.isSuccess());
        assertEquals("cohere", decision.getProvider());
    }

    @Test
    void executeReturnsErrorWhenNoProviderAvailable() {
        when(quotaService.getAvailableProviders()).thenReturn(List.of());

        String response = router.execute(TaskType.CODE_REVIEW, "Review this patch");

        assertTrue(response.startsWith("[ERROR]"));
        assertTrue(response.contains("No AI providers available"));
    }

    @Test
    void inferTaskTypeDetectsSecurityPrompt() {
        TaskType taskType = router.inferTaskType("Run OWASP security scan for vulnerabilities");
        assertEquals(TaskType.SECURITY_REVIEW, taskType);
    }

    @Test
    void prioritizeAvailableProvidersPreservesCapabilityOrderWithoutDuplicates() {
        List<String> prioritized = router.prioritizeAvailableProviders(
            TaskType.CODE_GENERATION,
            List.of("deepseek", "openai-gpt4", "openai-gpt4", "cohere")
        );

        assertEquals("openai-gpt4", prioritized.get(0));
        assertEquals("deepseek", prioritized.get(1));
        assertEquals(3, prioritized.size());
        assertEquals("cohere", prioritized.get(2));
    }

    @Test
    void routeSkipsProviderAfterFailureMarksItUnhealthy() {
        when(quotaService.getAvailableProviders()).thenReturn(
            List.of("openai-gpt4", "deepseek")
        );
        when(aiApiService.callProvider(eq("openai-gpt4"), anyString()))
            .thenThrow(new RuntimeException("provider failed"));

        String first = router.execute(TaskType.CODE_GENERATION, "Generate service class");
        assertTrue(first.startsWith("[ERROR]"));

        AICapabilityRouter.RoutingDecision next =
            router.route(TaskType.CODE_GENERATION, "Generate endpoint");
        assertEquals("deepseek", next.getProvider());
    }
}
