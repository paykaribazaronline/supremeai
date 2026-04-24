package com.supremeai.service;

import com.supremeai.provider.AIProviderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ContextualAIRankingServiceTest {

    @Mock
    private AIProviderFactory providerFactory;

    @InjectMocks
    private ContextualAIRankingService contextualRankingService;

    @BeforeEach
    void setUp() {
        when(providerFactory.getAllProviderNames()).thenReturn(new String[]{"openai", "anthropic", "groq"});
    }

    @Test
    void testDetectTaskType_CodeGeneration() {
        ContextualAIRankingService.TaskType type = contextualRankingService.detectTaskType("Please build a new feature in Java");
        assertEquals(ContextualAIRankingService.TaskType.CODE_GENERATION, type);
    }

    @Test
    void testDetectTaskType_Debugging() {
        ContextualAIRankingService.TaskType type = contextualRankingService.detectTaskType("Fix this error in my script");
        assertEquals(ContextualAIRankingService.TaskType.DEBUGGING, type);
    }

    @Test
    void testDetectTaskType_Default() {
        ContextualAIRankingService.TaskType type = contextualRankingService.detectTaskType("Hello world");
        assertEquals(ContextualAIRankingService.TaskType.QUESTION_ANSWERING, type);
    }

    @Test
    void testSelectBestProvider_DefaultScore() {
        ContextualAIRankingService.ProviderSelection selection = contextualRankingService.selectBestProvider("Write some code", null);
        
        assertNotNull(selection);
        assertEquals("openai", selection.providerName); // GPT-4 has higher default for code
        assertEquals(ContextualAIRankingService.TaskType.CODE_GENERATION, selection.taskType);
    }

    @Test
    void testRecordTaskOutcome_UpdatesPerformance() {
        contextualRankingService.recordTaskOutcome("groq", ContextualAIRankingService.TaskType.DEBUGGING, true, 500, 5.0);
        
        List<ContextualAIRankingService.ProviderRanking> rankings = contextualRankingService.getRankingsForTask(ContextualAIRankingService.TaskType.DEBUGGING);
        
        assertFalse(rankings.isEmpty());
        assertEquals("groq", rankings.get(0).provider);
        assertTrue(rankings.get(0).successRate > 50.0);
    }

    @Test
    void testGetStatistics() {
        contextualRankingService.recordTaskOutcome("openai", ContextualAIRankingService.TaskType.CODE_GENERATION, true, 200, 4.5);
        
        Map<String, Object> stats = contextualRankingService.getStatistics();
        
        assertNotNull(stats);
        assertTrue((Integer) stats.get("totalTaskRecords") >= 1);
    }
}
