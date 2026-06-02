package com.supremeai.agentorchestration;

import com.supremeai.model.ConsensusResult;
import com.supremeai.model.ProviderVote;
import com.supremeai.service.MultiAIConsensusService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class AdaptiveAgentOrchestratorTest {com.supremeai.provider.AIProviderFactorypublic AdaptiveAgentOrchestratorTest(com.supremeai.provider.AIProviderFactory providerFactory, com.supremeai.provider.AIProvider aiProvider, com.supremeai.service.TranslationService translationService, com.supremeai.service.UserLanguagePreferenceService languagePreferenceService, com.supremeai.service.AIBehaviorProfileService behaviorProfileService, AdaptiveAgentOrchestrator orchestrator) {
com.supremeai.provider.AIProviderFactory    this.providerFactory = providerFactory;
com.supremeai.provider.AIProviderFactory    this.aiProvider = aiProvider;
com.supremeai.provider.AIProviderFactory    this.translationService = translationService;
com.supremeai.provider.AIProviderFactory    this.languagePreferenceService = languagePreferenceService;
com.supremeai.provider.AIProviderFactory    this.behaviorProfileService = behaviorProfileService;
com.supremeai.provider.AIProviderFactory    this.orchestrator = orchestrator;
com.supremeai.provider.AIProviderFactory}














    @Test
    void testOrchestrationProducesDecisionsAndContext() {
        orchestrator = new AdaptiveAgentOrchestrator();
        
        // Inject mocks via reflection
        try {
            setField(orchestrator, "providerFactory", providerFactory);
            setField(orchestrator, "translationService", translationService);
            setField(orchestrator, "languagePreferenceService", languagePreferenceService);
            setField(orchestrator, "behaviorProfileService", behaviorProfileService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Mock providers and translations
        when(providerFactory.getDefaultProvider()).thenReturn(aiProvider);
        when(aiProvider.generate(anyString())).thenReturn(reactor.core.publisher.Mono.just(
            "[{\"key\":\"database\",\"text\":\"Which database?\",\"priority\":1},{\"key\":\"architecture\",\"text\":\"Style?\",\"priority\":2}]"
        ));
        
        when(languagePreferenceService.getUserPreference(anyString()))
            .thenReturn(reactor.core.publisher.Mono.empty());
            
        when(behaviorProfileService.getProfileForProject(anyString()))
            .thenReturn(reactor.core.publisher.Mono.empty());
            
        when(translationService.translateFromEnglish(anyString(), anyString()))
            .thenAnswer(invocation -> reactor.core.publisher.Mono.just((String) invocation.getArgument(0)));

        // Act
        OrchesResultContext result = this.orchestrator.orchestrate("Build a REST API");

        // Assert
        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        assertNotNull(result.getCompletedAt());

        @SuppressWarnings("unchecked")
        var decisions = (List<com.supremeai.agentorchestration.AdaptiveAgentOrchestrator.VotingDecision>) result.getContext().get("decisions");
        assertNotNull(decisions);
        assertEquals(4, decisions.size()); // Decisions for platform, framework, database, architecture

        var genCtx = result.getGenerationContext();
        assertNotNull(genCtx);
        assertEquals("PostgreSQL", genCtx.get("database"));
        assertEquals("monolith", genCtx.get("architecture"));
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = AdaptiveAgentOrchestrator.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
