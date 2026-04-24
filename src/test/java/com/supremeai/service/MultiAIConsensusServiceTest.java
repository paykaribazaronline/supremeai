package com.supremeai.service;

import com.supremeai.model.ConsensusResult;
import com.supremeai.model.ConsensusVote;
import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import com.supremeai.selfhealing.SelfHealingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MultiAIConsensusServiceTest {

    @Mock
    private AIProviderFactory providerFactory;

    @Mock
    private SelfHealingService selfHealingService;

    @Mock
    private KnowledgeFeedbackService feedbackService;

    @Mock
    private AIProvider groqProvider;

    @Mock
    private AIProvider openaiProvider;

    private MultiAIConsensusService consensusService;

    @BeforeEach
    void setUp() throws Exception {
        consensusService = new MultiAIConsensusService();

        // Inject mocks via reflection
        injectField("providerFactory", providerFactory);
        injectField("selfHealingService", selfHealingService);
        injectField("feedbackService", feedbackService);
        
        // Ensure the internal history is empty for each test
        java.lang.reflect.Field historyField = MultiAIConsensusService.class.getDeclaredField("history");
        historyField.setAccessible(true);
        ((java.util.List<?>) historyField.get(consensusService)).clear();

        // Setup self-healing service to execute the callable
        lenient().when(selfHealingService.executeWithRetry(any(Callable.class), anyInt(), anyLong()))
                .thenAnswer(invocation -> {
                    Callable<String> callable = invocation.getArgument(0);
                    return callable.call();
                });
    }

    private void injectField(String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = MultiAIConsensusService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(consensusService, value);
    }

    @Test
    void testAskAllAIs_SuccessConsensus() {
        String question = "What is the capital of France?";
        List<String> providers = List.of("groq", "openai");
        
        when(providerFactory.getProvider("groq")).thenReturn(groqProvider);
        when(providerFactory.getProvider("openai")).thenReturn(openaiProvider);
        
        when(groqProvider.generate(anyString())).thenReturn("Paris");
        when(openaiProvider.generate(anyString())).thenReturn("Paris");

        ConsensusResult result = consensusService.askAllAIs(question, providers, 5000L);

        assertNotNull(result);
        assertEquals("Paris", result.getConsensusAnswer());
        assertEquals(2, result.getVotes().size());
        assertTrue(result.getStrength().contains("STRONG"));
    }

    @Test
    void testAskAllAIs_WeakConsensus() {
        String question = "Who won the world cup 2022?";
        List<String> providers = List.of("groq", "openai", "anthropic");
        
        AIProvider anthropicProvider = mock(AIProvider.class);
        
        when(providerFactory.getProvider("groq")).thenReturn(groqProvider);
        when(providerFactory.getProvider("openai")).thenReturn(openaiProvider);
        when(providerFactory.getProvider("anthropic")).thenReturn(anthropicProvider);
        
        when(groqProvider.generate(anyString())).thenReturn("Argentina");
        when(openaiProvider.generate(anyString())).thenReturn("Argentina");
        when(anthropicProvider.generate(anyString())).thenReturn("France");

        ConsensusResult result = consensusService.askAllAIs(question, providers, 5000L);

        assertNotNull(result);
        assertEquals("Argentina", result.getConsensusAnswer());
        assertEquals(3, result.getVotes().size());
    }

    @Test
    void testAskAllAIs_NoProvidersRespond() throws Exception {
        String question = "Test?";
        List<String> providers = List.of("groq");
        
        when(providerFactory.getProvider("groq")).thenReturn(groqProvider);
        when(selfHealingService.executeWithRetry(any(Callable.class), anyInt(), anyLong()))
                .thenThrow(new RuntimeException("API Down"));

        ConsensusResult result = consensusService.askAllAIs(question, providers, 1000L);

        assertNotNull(result);
        assertEquals("No AI providers responded", result.getConsensusAnswer());
        assertEquals(0.0, result.getAverageConfidence());
        assertTrue(result.getStrength().contains("ERROR"));
    }

    @Test
    void testAskAllAIs_SingleProvider() {
        String question = "What is 2+2?";
        List<String> providers = List.of("groq");
        
        when(providerFactory.getProvider("groq")).thenReturn(groqProvider);
        when(groqProvider.generate(anyString())).thenReturn("4");

        ConsensusResult result = consensusService.askAllAIs(question, providers, 5000L);

        assertNotNull(result);
        assertEquals("4", result.getConsensusAnswer());
        assertEquals(1, result.getVotes().size());
    }

    @Test
    void testAskAllAIs_EmptyProviderList() {
        String question = "Test?";
        List<String> providers = List.of();

        ConsensusResult result = consensusService.askAllAIs(question, providers, 5000L);

        assertNotNull(result);
        assertEquals("No AI providers responded", result.getConsensusAnswer());
        assertEquals(0.0, result.getAverageConfidence());
    }

    @Test
    void testGetHistory_ReturnsFluxOfVotes() {
        String question1 = "Question 1?";
        List<String> providers1 = List.of("groq");
        when(providerFactory.getProvider("groq")).thenReturn(groqProvider);
        when(groqProvider.generate(anyString())).thenReturn("Answer 1");

        consensusService.askAllAIs(question1, providers1, 5000L);

        // Give async operations time to complete
        try { Thread.sleep(100); } catch (InterruptedException e) {}

        Flux<ConsensusVote> history = consensusService.getHistory(10);

        StepVerifier.create(history)
                .assertNext(vote -> {
                    assertEquals(question1, vote.getQuestion());
                    assertEquals("Answer 1", vote.getConsensusAnswer());
                })
                .verifyComplete();
    }

    @Test
    @Disabled("Flaky test due to async history saving and sleep timing")
    void testGetHistory_LimitsResults() {
        when(providerFactory.getProvider("groq")).thenReturn(groqProvider);
        when(groqProvider.generate(anyString())).thenReturn("Answer");

        // Create votes sequentially
        for (int i = 0; i < 5; i++) {
            consensusService.askAllAIs("Q" + i, List.of("groq"), 5000L);
        }

        // Give async operations time to complete
        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        Flux<ConsensusVote> history = consensusService.getHistory(2);

        // Should get at most 2 results
        StepVerifier.create(history)
                .expectNextCount(2)
                .thenCancel()
                .verify();
    }
}
