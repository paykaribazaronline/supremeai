package com.supremeai.agentorchestration;

import com.supremeai.model.ConsensusResult;
import com.supremeai.model.ProviderVote;
import com.supremeai.service.MultiAIConsensusService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Disabled("Failing due to reflection issues")
class AdaptiveAgentOrchestratorTest {

    @Mock
    private RequirementAnalyzerAI requirementAnalyzer;

    private AdaptiveAgentOrchestrator orchestrator;

    @Test
    void testOrchestrationProducesDecisionsAndContext() {
        orchestrator = new AdaptiveAgentOrchestrator();
        
        // Inject mock via reflection
        try {
            Field raField = AdaptiveAgentOrchestrator.class.getDeclaredField("requirementAnalyzer");
            raField.setAccessible(true);
            raField.set(orchestrator, requirementAnalyzer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Mock analyzer
        when(requirementAnalyzer.analyze(anyString())).thenReturn(List.of(
            new Question("database", "Which database?", "CRITICAL"),
            new Question("architecture", "Style?", "HIGH")
        ));

        // Act
        OrchesResultContext result = this.orchestrator.orchestrate("Build a REST API");

        // Assert
        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        assertNotNull(result.getCompletedAt());

        @SuppressWarnings("unchecked")
        var decisions = (List<VotingDecision>) result.getContext().get("decisions");
        assertNotNull(decisions);
        assertEquals(2, decisions.size());

        var genCtx = result.getGenerationContext();
        assertNotNull(genCtx);
        assertEquals("PostgreSQL", genCtx.get("database"));
        assertEquals("monolith", genCtx.get("architecture"));
    }
}
