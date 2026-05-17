package com.supremeai.agentorchestration;

import com.supremeai.model.APIProvider;
import com.supremeai.model.ProviderVote;
import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import com.supremeai.repository.ProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import reactor.core.publisher.Flux;
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
    private ProviderRepository providerRepository;

    @Mock
    private AIProvider provider1;

    @Mock
    private AIProvider provider2;

    private com.supremeai.service.MultiAIVotingService votingService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        
        votingService = new com.supremeai.service.MultiAIVotingService();
        
        // Inject dependencies manually via reflection
        setField(votingService, "providerFactory", providerFactory);
        setField(votingService, "providerRepository", providerRepository);
        
        // Mock provider list for findAll
        APIProvider ap1 = new APIProvider("p1", "p1", "type", "active");
        ap1.setCanParticipateInVoting(true);
        APIProvider ap2 = new APIProvider("p2", "p2", "type", "active");
        ap2.setCanParticipateInVoting(true);
        when(providerRepository.findAll()).thenReturn(Flux.just(ap1, ap2));
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = com.supremeai.service.MultiAIVotingService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void testConductDecisionVote_Consensus() {
        when(providerFactory.getEnforcedProvider("p1")).thenReturn(provider1);
        when(providerFactory.getEnforcedProvider("p2")).thenReturn(provider2);
        
        when(provider1.generate(anyString())).thenReturn(Mono.just("Agree"));
        when(provider2.generate(anyString())).thenReturn(Mono.just("Agree"));
        
        VotingDecision decision = votingService.conductDecisionVote("Test Question", "Test Context").block();
        
        assertNotNull(decision);
        assertEquals("STRONG", decision.getStrength());
        assertEquals(1.0, decision.getConfidence());
        assertEquals("Agree", decision.getAiConsensus());
        assertEquals(2, decision.getProviderVotes().size());
    }

    @Test
    void testConductDecisionVote_Split() {
        when(providerFactory.getEnforcedProvider("p1")).thenReturn(provider1);
        when(providerFactory.getEnforcedProvider("p2")).thenReturn(provider2);
        
        when(provider1.generate(anyString())).thenReturn(Mono.just("Yes"));
        when(provider2.generate(anyString())).thenReturn(Mono.just("No"));
        
        VotingDecision decision = votingService.conductDecisionVote("Test Question", "Test Context").block();
        
        assertNotNull(decision);
        assertEquals("WEAK", decision.getStrength());
        assertEquals(0.5, decision.getConfidence());
    }

    @Test
    void testConductDecisionVote_AllFail() {
        // Mock provider list to ensure we don't return early
        APIProvider p1 = new APIProvider("p1", "p1", "type", "active");
        p1.setCanParticipateInVoting(true);
        when(providerRepository.findAll()).thenReturn(Flux.just(p1));
        
        when(providerFactory.getEnforcedProvider(anyString())).thenThrow(new RuntimeException("Provider failure"));
        
        VotingDecision decision = votingService.conductDecisionVote("Test Question", "Test Context").block();
        
        assertEquals("ERROR", decision.getStrength());
        assertEquals(0.0, decision.getConfidence());
        assertTrue(decision.getAiConsensus().contains("ব্যর্থ হয়েছে"));
    }
}

