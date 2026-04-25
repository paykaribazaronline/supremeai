package com.supremeai.service;

import com.supremeai.cost.QuotaManager;
import com.supremeai.model.UserApiKey;
import com.supremeai.repository.UserApiKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuotaServiceTest {

    @Mock
    private UserApiKeyRepository userApiKeyRepository;

    @Mock
    private QuotaManager quotaManager;

    @InjectMocks
    private QuotaService quotaService;

    private UserApiKey activeApiKey;
    private UserApiKey inactiveApiKey;
    private UserApiKey exhaustedApiKey;

    @BeforeEach
    void setUp() {
        activeApiKey = new UserApiKey("user1", "OpenAI", "Active", "key-1");
        activeApiKey.setStatus("active");
        activeApiKey.setRequestCount(500L);

        inactiveApiKey = new UserApiKey("user1", "OpenAI", "Inactive", "key-2");
        inactiveApiKey.setStatus("inactive");
        inactiveApiKey.setRequestCount(100L);

        exhaustedApiKey = new UserApiKey("user2", "Anthropic", "Exhausted", "key-3");
        exhaustedApiKey.setStatus("active");
        exhaustedApiKey.setRequestCount(1000L);
    }

    @Test
    void hasQuotaRemainingReturnsTrueForActiveKeyBelowLimit() {
        when(userApiKeyRepository.findByApiKey("valid-key")).thenReturn(Mono.just(activeApiKey));

        StepVerifier.create(quotaService.hasQuotaRemaining("valid-key"))
            .expectNext(true)
            .verifyComplete();
    }

    @Test
    void incrementUsagePersistsUpdatedCounter() {
        when(userApiKeyRepository.findByApiKey("valid-key")).thenReturn(Mono.just(activeApiKey));
        when(userApiKeyRepository.save(any(UserApiKey.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(quotaService.incrementUsage("valid-key"))
            .expectNext(true)
            .verifyComplete();

        assertEquals(501L, activeApiKey.getRequestCount());
        verify(userApiKeyRepository).save(activeApiKey);
    }

    @Test
    void incrementUsageReturnsFalseForInactiveKey() {
        when(userApiKeyRepository.findByApiKey("inactive-key")).thenReturn(Mono.just(inactiveApiKey));

        StepVerifier.create(quotaService.incrementUsage("inactive-key"))
            .expectNext(false)
            .verifyComplete();

        verify(userApiKeyRepository, never()).save(any(UserApiKey.class));
    }

    @Test
    void validateAndIncrementFailsForMissingKey() {
        when(userApiKeyRepository.findByApiKey("missing")).thenReturn(Mono.empty());

        StepVerifier.create(quotaService.validateAndIncrement("missing"))
            .expectErrorMatches(error ->
                error instanceof IllegalArgumentException &&
                    "Invalid API key".equals(error.getMessage()))
            .verify();
    }

    @Test
    void validateAndIncrementFailsWhenQuotaExceeded() {
        when(userApiKeyRepository.findByApiKey("full")).thenReturn(Mono.just(exhaustedApiKey));

        StepVerifier.create(quotaService.validateAndIncrement("full"))
            .expectErrorMatches(error ->
                error instanceof RuntimeException &&
                    "Quota exceeded".equals(error.getMessage()))
            .verify();
    }

    @Test
    void getUsageStatsReflectsQuotaState() {
        when(userApiKeyRepository.findByApiKey("valid-key")).thenReturn(Mono.just(activeApiKey));

        StepVerifier.create(quotaService.getUsageStats("valid-key"))
            .assertNext(stats -> {
                assertEquals(500L, stats.getCurrentUsage());
                assertEquals(1000L, stats.getMonthlyQuota());
                assertTrue(stats.isHasQuotaRemaining());
                assertEquals(50.0, stats.getUsagePercentage());
            })
            .verifyComplete();
    }

    @Test
    void resetApiUsageReturnsFalseWhenKeyMissing() {
        when(userApiKeyRepository.findByApiKey("missing")).thenReturn(Mono.empty());

        StepVerifier.create(quotaService.resetApiUsage("missing"))
            .expectNext(false)
            .verifyComplete();
    }

    @Test
    void resetUserUsageSavesEachMatchingKey() {
        UserApiKey keyA = new UserApiKey("user1", "OpenAI", "A", "a");
        keyA.setRequestCount(10L);
        UserApiKey keyB = new UserApiKey("user1", "Groq", "B", "b");
        keyB.setRequestCount(20L);

        when(userApiKeyRepository.findByUserId("user1")).thenReturn(Flux.just(keyA, keyB));
        when(userApiKeyRepository.save(any(UserApiKey.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(quotaService.resetUserUsage("user1"))
            .verifyComplete();

        assertEquals(0L, keyA.getRequestCount());
        assertEquals(0L, keyB.getRequestCount());
    }

    @Test
    void hasQuotaRemainingReturnsFalseForInactiveOrMissingKey() {
        when(userApiKeyRepository.findByApiKey("inactive")).thenReturn(Mono.just(inactiveApiKey));
        when(userApiKeyRepository.findByApiKey("missing")).thenReturn(Mono.empty());

        Boolean inactive = quotaService.hasQuotaRemaining("inactive").block();
        Boolean missing = quotaService.hasQuotaRemaining("missing").block();

        assertFalse(Boolean.TRUE.equals(inactive));
        assertFalse(Boolean.TRUE.equals(missing));
    }
}
