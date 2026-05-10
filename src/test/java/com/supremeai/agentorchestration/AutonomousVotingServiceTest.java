package com.supremeai.agentorchestration;

import com.supremeai.model.ProviderVote;
import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MultiAIVotingServiceTest {

    @Mock
    private AIProviderFactory providerFactory;

    @Mock
    private AIProvider provider1;

    @Mock
    private AIProvider provider2;

    private com.supremeai.service.MultiAIVotingService votingService;
    private ThreadPoolTaskExecutor executor;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        
        executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.initialize();
        
        votingService = new com.supremeai.service.MultiAIVotingService(executor);
        
        // Inject dependencies manually or via reflection if needed
        Field factoryField = com.supremeai.service.MultiAIVotingService.class.getDeclaredField("providerFactory");
        factoryField.setAccessible(true);
        factoryField.set(votingService, providerFactory);
        
        Field providersField = com.supremeai.service.MultiAIVotingService.class.getDeclaredField("activeProviders");
        providersField.setAccessible(true);
        providersField.set(votingService, "p1,p2");
    }

    @Test
    void testConductDecisionVote_Consensus() {
        when(providerFactory.getProvider("p1")).thenReturn(provider1);
        when(providerFactory.getProvider("p2")).thenReturn(provider2);
        
        when(provider1.generate(anyString())).thenReturn(Mono.just("Agree"));
        when(provider2.generate(anyString())).thenReturn(Mono.just("Agree"));
        
        VotingDecision decision = votingService.conductDecisionVote("Test Question", "Test Context");
        
        assertNotNull(decision);
        assertEquals("STRONG", decision.getStrength());
        assertEquals(1.0, decision.getConfidence());
        assertEquals("Agree", decision.getAiConsensus());
        assertEquals(2, decision.getProviderVotes().size());
    }

    @Test
    void testConductDecisionVote_Split() {
        when(providerFactory.getProvider("p1")).thenReturn(provider1);
        when(providerFactory.getProvider("p2")).thenReturn(provider2);
        
        when(provider1.generate(anyString())).thenReturn(Mono.just("Yes"));
        when(provider2.generate(anyString())).thenReturn(Mono.just("No"));
        
        VotingDecision decision = votingService.conductDecisionVote("Test Question", "Test Context");
        
        assertNotNull(decision);
        assertEquals("WEAK", decision.getStrength());
        assertEquals(0.5, decision.getConfidence());
    }

    @Test
    void testConductDecisionVote_AllFail() {
        when(providerFactory.getProvider(anyString())).thenThrow(new RuntimeException("Provider failure"));
        
        VotingDecision decision = votingService.conductDecisionVote("Test Question", "Test Context");
        
        assertEquals("ERROR", decision.getStrength());
        assertEquals(0.0, decision.getConfidence());
        assertTrue(decision.getAiConsensus().contains("failed"));
    }
}
