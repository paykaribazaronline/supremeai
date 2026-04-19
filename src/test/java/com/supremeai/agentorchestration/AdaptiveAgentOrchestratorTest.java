package com.supremeai.agentorchestration;

import com.supremeai.model.ConsensusResult;
import com.supremeai.model.ProviderVote;
import com.supremeai.service.MultiAIConsensusService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdaptiveAgentOrchestratorTest {

    @Mock
    MultiAIConsensusService consensusService;

    @Test
    void testOrchestrationProducesDecisionsAndContext() {
        // Arrange
        AdaptiveAgentOrchestrator orchestrator = new AdaptiveAgentOrchestrator(consensusService);

        // Mock consensus result
        ConsensusResult mockResult = new ConsensusResult(
            "dummy question",
            "PostgreSQL",
            List.of(),
            0.95,
            "STRONG"
        );

        when(consensusService.askAllAIs(anyString(), anyList(), anyLong()))
            .thenReturn(mockResult);

        // Act
        OrchesResultContext result = orchestrator.orchestrate("Build a REST API");

        // Assert
        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        assertNotNull(result.getCompletedAt());

        @SuppressWarnings("unchecked")
        var decisions = (List<VotingDecision>) result.getContext().get("decisions");
        assertNotNull(decisions);
        assertFalse(decisions.isEmpty(), "Should contain decisions");

        for (VotingDecision d : decisions) {
            assertEquals("PostgreSQL", d.getAiConsensus());
            assertEquals(0.95, d.getConfidence());
            assertEquals("STRONG", d.getStrength());
        }

        var genCtx = result.getGenerationContext();
        assertNotNull(genCtx);
        assertTrue(genCtx.size() > 0, "Generation context should contain decisions");
        assertEquals("PostgreSQL", genCtx.get("database"));
    }
}
