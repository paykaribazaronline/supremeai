package com.supremeai.agent;

import com.supremeai.agentorchestration.Question;
import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class DiOSAgentTest {

    @Mock
    private AIProviderFactory providerFactory;

    @Mock
    private AIProvider aiProvider;

    @Mock
    private AgentRuleService ruleService;

    @InjectMocks
    private DiOSAgent diosAgent;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(ruleService.wrapWithRules(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(providerFactory.getDefaultProvider()).thenReturn(aiProvider);
    }

    @Test
    void testAnalyzeIOSRequirements_returnsQuestions() {
        String jsonResponse = "[{\"key\":\"test\",\"text\":\"Test question\",\"priority\":\"HIGH\"}]";
        when(providerFactory.getProvider("groq")).thenReturn(aiProvider);
        when(aiProvider.generate(anyString())).thenReturn(Mono.just(jsonResponse));

        List<Question> result = diosAgent.analyzeIOSRequirements("Test requirement");

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("test", result.get(0).getKey());
    }

    @Test
    void testAnalyzeIOSRequirements_returnsDefaultsOnException() {
        when(providerFactory.getProvider("groq")).thenReturn(aiProvider);
        when(aiProvider.generate(anyString())).thenReturn(Mono.error(new RuntimeException("API error")));

        List<Question> result = diosAgent.analyzeIOSRequirements("Test requirement");

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testAnalyzeIOSRequirements_returnsDefaultsOnInvalidJson() {
        when(providerFactory.getProvider("groq")).thenReturn(aiProvider);
        when(aiProvider.generate(anyString())).thenReturn(Mono.just("Invalid JSON"));

        List<Question> result = diosAgent.analyzeIOSRequirements("Test requirement");

        assertNotNull(result);
    }

    @Test
    void testAnalyzeDesktopRequirements_returnsQuestions() {
        String jsonResponse = "[{\"key\":\"desktop\",\"text\":\"Test question\",\"priority\":\"CRITICAL\"}]";
        when(providerFactory.getProvider("groq")).thenReturn(aiProvider);
        when(aiProvider.generate(anyString())).thenReturn(Mono.just(jsonResponse));

        List<Question> result = diosAgent.analyzeDesktopRequirements("Test requirement");

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("desktop", result.get(0).getKey());
    }

    @Test
    void testAnalyzeDesktopRequirements_returnsDefaultsOnException() {
        when(providerFactory.getProvider("groq")).thenReturn(aiProvider);
        when(aiProvider.generate(anyString())).thenReturn(Mono.error(new RuntimeException("API error")));

        List<Question> result = diosAgent.analyzeDesktopRequirements("Test requirement");

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testAnalyzeIOSRequirements_includesBengaliPrompt() {
        when(providerFactory.getProvider("groq")).thenReturn(aiProvider);
        when(aiProvider.generate(anyString())).thenReturn(Mono.just("[]"));

        diosAgent.analyzeIOSRequirements("Requirement");

        verify(aiProvider).generate(argThat(prompt -> 
            prompt.contains("iOS") && prompt.contains("একটি")));
    }

    @Test
    void testAnalyzeDesktopRequirements_includesBengaliPrompt() {
        when(providerFactory.getProvider("groq")).thenReturn(aiProvider);
        when(aiProvider.generate(anyString())).thenReturn(Mono.just("[]"));

        diosAgent.analyzeDesktopRequirements("Requirement");

        verify(aiProvider).generate(argThat(prompt -> 
            prompt.contains("ডেস্কটপ") && prompt.contains("একটি")));
    }
}