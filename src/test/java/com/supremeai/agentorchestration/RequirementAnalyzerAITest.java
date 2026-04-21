package com.supremeai.agentorchestration;

import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class RequirementAnalyzerAITest {

    @Mock
    private AIProviderFactory providerFactory;

    @Mock
    private AIProvider aiProvider;

    @InjectMocks
    private RequirementAnalyzerAI requirementAnalyzer;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testAnalyzeWithAiSuccess() {
        String mockResponse = "Here are the questions:\n" +
                "[\n" +
                "  {\"key\": \"q1\", \"text\": \"Question 1?\", \"priority\": \"HIGH\"},\n" +
                "  {\"key\": \"q2\", \"text\": \"Question 2?\", \"priority\": \"CRITICAL\"}\n" +
                "]";
        
        when(providerFactory.getProvider("groq")).thenReturn(aiProvider);
        when(aiProvider.generate(anyString())).thenReturn(mockResponse);

        List<Question> questions = requirementAnalyzer.analyze("Build a web app");

        assertNotNull(questions);
        assertEquals(2, questions.size());
        assertEquals("q1", questions.get(0).getKey());
        assertEquals("Question 1?", questions.get(0).getText());
        assertEquals("HIGH", questions.get(0).getPriority());
    }

    @Test
    public void testAnalyzeWithAiFailureFallback() {
        when(providerFactory.getProvider("groq")).thenThrow(new RuntimeException("Provider failed"));

        List<Question> questions = requirementAnalyzer.analyze("Build a web app");

        assertNotNull(questions);
        assertFalse(questions.isEmpty());
        // Should contain default questions
        assertTrue(questions.stream().anyMatch(q -> q.getKey().equals("architecture")));
    }

    @Test
    public void testAnalyzeWithMalformedJsonFallback() {
        when(providerFactory.getProvider("groq")).thenReturn(aiProvider);
        when(aiProvider.generate(anyString())).thenReturn("Not a JSON response");

        List<Question> questions = requirementAnalyzer.analyze("Build a web app");

        assertNotNull(questions);
        assertFalse(questions.isEmpty());
        assertTrue(questions.stream().anyMatch(q -> q.getKey().equals("database")));
    }
}
