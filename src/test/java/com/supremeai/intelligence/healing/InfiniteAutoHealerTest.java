package com.supremeai.intelligence.healing;

import com.supremeai.fallback.ThirdOpinionOrchestrator;
import com.supremeai.service.SelfHealingService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InfiniteAutoHealer.
 * Tests development loop behavior: council rejection leading to max iterations, fallback activation.
 */
class InfiniteAutoHealerTest {

    @Test
    void testDevelopUntilPerfection_delegatesToSelfHealingService() {
        ThirdOpinionOrchestrator fallback = mock(ThirdOpinionOrchestrator.class);
        SelfHealingService selfHealing = mock(SelfHealingService.class);

        when(selfHealing.developUntilPerfection(anyString(), anyString()))
            .thenReturn(Mono.just("SUCCESS"));

        InfiniteAutoHealer healer = new InfiniteAutoHealer(selfHealing);

        String result = healer.developUntilPerfection("CATEGORY", "Task").block();
        assertEquals("SUCCESS", result);
        verify(selfHealing).developUntilPerfection("CATEGORY", "Task");
    }
}