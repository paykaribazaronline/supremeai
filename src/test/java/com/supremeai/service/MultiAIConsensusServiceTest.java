package com.supremeai.service;

import com.supremeai.model.ConsensusResult;
import com.supremeai.model.ProviderVote;
import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import com.supremeai.selfhealing.SelfHealingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.function.Supplier;

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
    private AIProvider groqProvider;

    @Mock
    private AIProvider openaiProvider;

    @InjectMocks
    private MultiAIConsensusService consensusService;

    @BeforeEach
    void setUp() {
        // Setup default behavior for self-healing service to just execute the supplier
        lenient().when(selfHealingService.executeWithRetry(any(), anyInt(), anyLong()))
                .thenAnswer(invocation -> {
                    Supplier<String> supplier = invocation.getArgument(0);
                    return supplier.get();
                });
    }

    @Test
    void testAskAllAIs_SuccessConsensus() {
        // Arrange
        String question = "What is the capital of France?";
        List<String> providers = List.of("groq", "openai");
        
        when(providerFactory.getProvider("groq")).thenReturn(groqProvider);
        when(providerFactory.getProvider("openai")).thenReturn(openaiProvider);
        
        when(groqProvider.generate(anyString())).thenReturn("Paris");
        when(openaiProvider.generate(anyString())).thenReturn("Paris");

        // Act
        ConsensusResult result = consensusService.askAllAIs(question, providers, 5000L);

        // Assert
        assertNotNull(result);
        assertEquals("Paris", result.getAnswer());
        assertEquals(100.0, result.getConsensusPercentage());
        assertEquals(2, result.getVotes().size());
        assertTrue(result.getStatus().contains("STRONG"));
    }

    @Test
    void testAskAllAIs_WeakConsensus() {
        // Arrange
        String question = "Who won the world cup 2022?";
        List<String> providers = List.of("groq", "openai", "anthropic");
        
        AIProvider anthropicProvider = mock(AIProvider.class);
        
        when(providerFactory.getProvider("groq")).thenReturn(groqProvider);
        when(providerFactory.getProvider("openai")).thenReturn(openaiProvider);
        when(providerFactory.getProvider("anthropic")).thenReturn(anthropicProvider);
        
        when(groqProvider.generate(anyString())).thenReturn("Argentina");
        when(openaiProvider.generate(anyString())).thenReturn("Argentina");
        when(anthropicProvider.generate(anyString())).thenReturn("France");

        // Act
        ConsensusResult result = consensusService.askAllAIs(question, providers, 5000L);

        // Assert
        assertNotNull(result);
        assertEquals("Argentina", result.getAnswer());
        assertEquals(2.0/3.0 * 100.0, result.getConsensusPercentage(), 0.01);
        assertTrue(result.getStatus().contains("WEAK"));
    }

    @Test
    void testAskAllAIs_NoProvidersRespond() {
        // Arrange
        String question = "Test?";
        List<String> providers = List.of("groq");
        
        when(providerFactory.getProvider("groq")).thenReturn(groqProvider);
        when(selfHealingService.executeWithRetry(any(), anyInt(), anyLong()))
                .thenThrow(new RuntimeException("API Down"));

        // Act
        ConsensusResult result = consensusService.askAllAIs(question, providers, 1000L);

        // Assert
        assertNotNull(result);
        assertEquals("ERROR", result.getStatus());
        assertEquals("No AI providers responded", result.getAnswer());
    }
}
