package com.supremeai.agentorchestration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.supremeai.model.APIProvider;
import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import com.supremeai.repository.ProviderRepository;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class MultiAIVotingServiceTest {

  @Mock private AIProviderFactory providerFactory;

  @Mock private ProviderRepository providerRepository;

  @Mock private AIProvider provider1;

  @Mock private AIProvider provider2;

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
    ap1.setWeight(1.5); // Admin defined higher weight for specialized model
    APIProvider ap2 = new APIProvider("p2", "p2", "type", "active");
    ap2.setCanParticipateInVoting(true);
    ap2.setWeight(1.0);
    APIProvider ap3 = new APIProvider("p3", "p3", "type", "active");
    ap3.setCanParticipateInVoting(false); // Disabled by Admin
    when(providerRepository.findAll()).thenReturn(Flux.just(ap1, ap2, ap3));
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

    VotingDecision decision =
        votingService.conductDecisionVote("Test Question", "Test Context").block();

    assertNotNull(decision);
    assertEquals("STRONG", decision.getStrength());
    assertEquals(1.0, decision.getConfidence());
    assertEquals("Agree", decision.getAiConsensus());
    assertEquals(2, decision.getProviderVotes().size());
  }

  @Test
  void testConductDecisionVote_WeightedConsensus() {
    // p1 has weight 1.5, p2 has weight 1.0
    when(providerFactory.getEnforcedProvider("p1")).thenReturn(provider1);
    when(providerFactory.getEnforcedProvider("p2")).thenReturn(provider2);

    // p1 (higher weight) says "Option A", p2 (lower weight) says "Option B"
    when(provider1.generate(anyString())).thenReturn(Mono.just("Option A"));
    when(provider2.generate(anyString())).thenReturn(Mono.just("Option B"));

    VotingDecision decision =
        votingService.conductDecisionVote("Which is better?", "Context").block();

    assertNotNull(decision);
    // Option A should win because of weight (1.5 > 1.0)
    assertEquals("Option A", decision.getAiConsensus());
    assertEquals(2, decision.getProviderVotes().size());

    // Ensure ap3 (disabled) was never called
    verify(providerFactory, never()).getEnforcedProvider("p3");
  }

  @Test
  void testConductDecisionVote_Split() {
    when(providerFactory.getEnforcedProvider("p1")).thenReturn(provider1);
    when(providerFactory.getEnforcedProvider("p2")).thenReturn(provider2);

    when(provider1.generate(anyString())).thenReturn(Mono.just("Yes"));
    when(provider2.generate(anyString())).thenReturn(Mono.just("No"));

    VotingDecision decision =
        votingService.conductDecisionVote("Test Question", "Test Context").block();

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

    when(providerFactory.getEnforcedProvider(anyString()))
        .thenThrow(new RuntimeException("Provider failure"));

    VotingDecision decision =
        votingService.conductDecisionVote("Test Question", "Test Context").block();

    assertEquals("ERROR", decision.getStrength());
    assertEquals(0.0, decision.getConfidence());
    assertTrue(decision.getAiConsensus().contains("ব্যর্থ হয়েছে"));
  }
}
