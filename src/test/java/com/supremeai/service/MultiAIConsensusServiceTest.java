package com.supremeai.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import com.supremeai.repository.ConsensusVoteRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
public class MultiAIConsensusServiceTest {

    @Mock
    private AIProviderFactory providerFactory;

    @Mock
    private ConsensusVoteRepository voteRepository;

    @InjectMocks
    private MultiAIConsensusService consensusService;

    @Test
    void testAskAllAIs_ReturnsConsensusResult() {
        // Arrange
        String question = "What is 2+2?";
        List<String> providers = List.of("openai", "anthropic");

        // Create mock providers
        AIProvider mockOpenAI = mock(AIProvider.class);
        AIProvider mockAnthropic = mock(AIProvider.class);
        
        when(mockOpenAI.generate(anyString())).thenReturn("4");
        when(mockAnthropic.generate(anyString())).thenReturn("4");
        
        when(providerFactory.getProvider("openai")).thenReturn(mockOpenAI);
        when(providerFactory.getProvider("anthropic")).thenReturn(mockAnthropic);

        // Act
        ConsensusResult result = consensusService.askAllAIs(question, providers, 5000L);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getConsensusAnswer());
        assertEquals(2, result.getProviderVotes().size());
        assertEquals("4", result.getConsensusAnswer());
        assertTrue(result.getAverageConfidence() > 0);
    }

    @Test
    void testAskAllAIs_EmptyResponseWhenNoProviders() {
        // Arrange
        String question = "Test question";
        List<String> providers = List.of("unknown");

        when(providerFactory.getProvider("unknown")).thenThrow(new IllegalArgumentException("Unknown provider"));

        // Act
        ConsensusResult result = consensusService.askAllAIs(question, providers, 1000L);

        // Assert
        assertNotNull(result);
        assertEquals("No AI providers responded", result.getConsensusAnswer());
        assertEquals(0.0, result.getAverageConfidence());
    }
}
