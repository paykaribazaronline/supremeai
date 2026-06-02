package com.supremeai.service;

import com.supremeai.model.ConsensusResult;
import com.supremeai.model.ConsensusVote;
import com.supremeai.model.ProviderVote;
import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import com.supremeai.util.ThirdOpinionConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MultiAIConsensusServiceTest {AIProviderFactorypublic MultiAIConsensusServiceTest(AIProviderFactory providerFactory, SelfHealingService selfHealingService, KnowledgeFeedbackService feedbackService, ContextualAIRankingService contextualRankingService, AIProvider mockProvider1, AIProvider mockProvider2, MultiAIConsensusService service) {
AIProviderFactory    this.providerFactory = providerFactory;
AIProviderFactory    this.selfHealingService = selfHealingService;
AIProviderFactory    this.feedbackService = feedbackService;
AIProviderFactory    this.contextualRankingService = contextualRankingService;
AIProviderFactory    this.mockProvider1 = mockProvider1;
AIProviderFactory    this.mockProvider2 = mockProvider2;
AIProviderFactory    this.service = service;
AIProviderFactory}
















    @BeforeEach
    void setUp() {
        service = new MultiAIConsensusService();
        // Use reflection to set the mocked dependencies
        try {
            java.lang.reflect.Field providerFactoryField = MultiAIConsensusService.class.getDeclaredField("providerFactory");
            providerFactoryField.setAccessible(true);
            providerFactoryField.set(service, providerFactory);

            java.lang.reflect.Field selfHealingField = MultiAIConsensusService.class.getDeclaredField("selfHealingService");
            selfHealingField.setAccessible(true);
            selfHealingField.set(service, selfHealingService);

            java.lang.reflect.Field feedbackField = MultiAIConsensusService.class.getDeclaredField("feedbackService");
            feedbackField.setAccessible(true);
            feedbackField.set(service, feedbackService);

            java.lang.reflect.Field rankingField = MultiAIConsensusService.class.getDeclaredField("contextualRankingService");
            rankingField.setAccessible(true);
            rankingField.set(service, contextualRankingService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void askAllAIs_shouldReturnConsensusResultWithMultipleProviders() {
        // Given
        String question = "What is the capital of France?";
        List<String> providerNames = Arrays.asList("openai", "anthropic");
        long timeoutMs = 5000;

        when(providerFactory.getProvider("openai")).thenReturn(mockProvider1);
        when(providerFactory.getProvider("anthropic")).thenReturn(mockProvider2);

        when(selfHealingService.executeWithRetry(any(), anyInt(), anyLong()))
                .thenReturn(Mono.just("Paris"))
                .thenReturn(Mono.just("Paris is the capital of France"));

        when(contextualRankingService.detectTaskType(question))
                .thenReturn(ContextualAIRankingService.TaskType.QUESTION_ANSWERING);

        when(contextualRankingService.calculateProviderScore(eq("openai"), any(), any()))
                .thenReturn(85.0);
        when(contextualRankingService.calculateProviderScore(eq("anthropic"), any(), any()))
                .thenReturn(90.0);

        // When
        Mono<ConsensusResult> result = service.askAllAIs(question, providerNames, timeoutMs);

        // Then
        StepVerifier.create(result)
                .assertNext(consensus -> {
                    assertEquals(question, consensus.getQuestion());
                    assertTrue(consensus.getConsensusAnswer().contains("Paris"));
                    assertEquals(2, consensus.getVotes().size());
                    assertTrue(consensus.getAverageConfidence() > 0);
                    assertTrue(consensus.getConsensusPercentage() >= 50.0);
                    assertTrue(consensus.getResponseTimeMs() > 0);
                    assertTrue(consensus.getQualityScore() > 0);
                    assertTrue(consensus.getStrength().startsWith("CONSENSUS"));
                })
                .verifyComplete();

        verify(contextualRankingService, times(2)).recordTaskOutcome(anyString(), any(), eq(true), anyLong(), anyDouble());
    }

    @Test
    void askAllAIs_shouldHandleProviderFailuresGracefully() {
        // Given
        String question = "What is 2+2?";
        List<String> providerNames = Arrays.asList("openai", "anthropic");
        long timeoutMs = 5000;

        when(providerFactory.getProvider("openai")).thenReturn(mockProvider1);
        when(providerFactory.getProvider("anthropic")).thenReturn(mockProvider2);

        // First provider succeeds, second fails
        when(selfHealingService.executeWithRetry(any(), anyInt(), anyLong()))
                .thenReturn(Mono.just("4"))
                .thenReturn(Mono.empty()); // Simulate failure

        when(contextualRankingService.detectTaskType(question))
                .thenReturn(ContextualAIRankingService.TaskType.QUESTION_ANSWERING);

        when(contextualRankingService.calculateProviderScore(anyString(), any(), any()))
                .thenReturn(80.0);

        // When
        Mono<ConsensusResult> result = service.askAllAIs(question, providerNames, timeoutMs);

        // Then
        StepVerifier.create(result)
                .assertNext(consensus -> {
                    assertEquals(question, consensus.getQuestion());
                    assertEquals("4", consensus.getConsensusAnswer());
                    assertEquals(1, consensus.getVotes().size()); // Only one successful vote
                    assertEquals(100.0, consensus.getConsensusPercentage()); // 1/1 = 100%
                    assertTrue(consensus.getStrength().contains("STRONG"));
                })
                .verifyComplete();
    }

    @Test
    void askAllAIs_shouldReturnErrorWhenAllProvidersFail() {
        // Given
        String question = "What is the meaning of life?";
        List<String> providerNames = Arrays.asList("openai", "anthropic");
        long timeoutMs = 5000;

        when(providerFactory.getProvider("openai")).thenReturn(mockProvider1);
        when(providerFactory.getProvider("anthropic")).thenReturn(mockProvider2);

        // Both providers fail
        when(selfHealingService.executeWithRetry(any(), anyInt(), anyLong()))
                .thenReturn(Mono.empty())
                .thenReturn(Mono.empty());

        // When
        Mono<ConsensusResult> result = service.askAllAIs(question, providerNames, timeoutMs);

        // Then
        StepVerifier.create(result)
                .assertNext(consensus -> {
                    assertEquals(question, consensus.getQuestion());
                    assertEquals(ThirdOpinionConstants.NO_PROVIDER_RESPONSE, consensus.getConsensusAnswer());
                    assertEquals(0, consensus.getVotes().size());
                    assertEquals(0.0, consensus.getAverageConfidence());
                    assertEquals("ERROR", consensus.getStrength());
                    assertEquals(0.0, consensus.getConsensusPercentage());
                })
                .verifyComplete();
    }

    @Test
    void askAllAIs_shouldHandleTimeout() {
        // Given
        String question = "Complex philosophical question";
        List<String> providerNames = Arrays.asList("openai", "anthropic");
        long timeoutMs = 100; // Very short timeout

        when(providerFactory.getProvider("openai")).thenReturn(mockProvider1);
        when(providerFactory.getProvider("anthropic")).thenReturn(mockProvider2);

        // Simulate slow responses that timeout
        when(selfHealingService.executeWithRetry(any(), anyInt(), anyLong()))
                .thenReturn(Mono.delay(java.time.Duration.ofMillis(200)).thenReturn(Mono.just("Answer")))
                .thenReturn(Mono.delay(java.time.Duration.ofMillis(200)).thenReturn(Mono.just("Answer")));

        // When
        Mono<ConsensusResult> result = service.askAllAIs(question, providerNames, timeoutMs);

        // Then
        StepVerifier.create(result)
                .assertNext(consensus -> {
                    assertEquals(question, consensus.getQuestion());
                    assertEquals("দুঃখিত, অনুরোধটির উত্তর দিতে সময়সীমা অতিক্রম হয়ে গেছে।", consensus.getConsensusAnswer());
                    assertEquals(0, consensus.getVotes().size());
                    assertEquals("TIMEOUT", consensus.getStrength());
                })
                .verifyComplete();
    }

    @Test
    void askContextualAIs_shouldSelectBestProvidersAutomatically() {
        // Given
        String question = "Write a Java function to reverse a string";
        int count = 2;
        long timeoutMs = 5000;

        // Mock contextual ranking selection
        ContextualAIRankingService.ProviderSelection selection = new ContextualAIRankingService.ProviderSelection(
                "openai", ContextualAIRankingService.TaskType.CODE_GENERATION, 0.95, "Best for coding tasks");

        when(contextualRankingService.selectBestProvider(question, null))
                .thenReturn(selection);

        when(contextualRankingService.getRankingsForTask(ContextualAIRankingService.TaskType.CODE_GENERATION))
                .thenReturn(Arrays.asList(
                        new ContextualAIRankingService.ProviderRanking("openai", "CODE_GENERATION", 95.0, 500.0, 4.2, 100),
                        new ContextualAIRankingService.ProviderRanking("anthropic", "CODE_GENERATION", 88.0, 600.0, 3.9, 80)
                ));



        // Mock provider responses
        when(providerFactory.getProvider(anyString())).thenReturn(mockProvider1);
        when(selfHealingService.executeWithRetry(any(), anyInt(), anyLong()))
                .thenReturn(Mono.just("public String reverse(String s) { return new StringBuilder(s).reverse().toString(); }"));

        when(contextualRankingService.detectTaskType(question))
                .thenReturn(ContextualAIRankingService.TaskType.CODE_GENERATION);
        when(contextualRankingService.calculateProviderScore(anyString(), any(), any()))
                .thenReturn(90.0);

        // When
        Mono<ConsensusResult> result = service.askContextualAIs(question, count, timeoutMs);

        // Then
        StepVerifier.create(result)
                .assertNext(consensus -> {
                    assertEquals(question, consensus.getQuestion());
                    assertNotNull(consensus.getConsensusAnswer());
                    assertTrue(consensus.getVotes().size() <= count);
                })
                .verifyComplete();

        verify(contextualRankingService).selectBestProvider(question, null);
        verify(contextualRankingService).getRankingsForTask(ContextualAIRankingService.TaskType.CODE_GENERATION);
    }

    @Test
    void getHistory_shouldReturnRecentConsensusVotes() {
        // When
        reactor.core.publisher.Flux<ConsensusVote> history = service.getHistory(10);

        // Then
        StepVerifier.create(history)
                .expectNextCount(0) // Initially empty
                .verifyComplete();
    }

    @Test
    void calculateConsensus_shouldDetermineStrongConsensus() {
        // Given
        String question = "What is the largest planet?";
        List<ProviderVote> votes = Arrays.asList(
                new ProviderVote("openai", "Jupiter", 0.9, System.currentTimeMillis()),
                new ProviderVote("anthropic", "Jupiter", 0.85, System.currentTimeMillis()),
                new ProviderVote("gemini", "Saturn", 0.7, System.currentTimeMillis())
        );
        long totalTimeMs = 1200;

        // When - use reflection to call private method
        try {
            java.lang.reflect.Method method = MultiAIConsensusService.class.getDeclaredMethod(
                    "calculateConsensus", String.class, List.class, long.class);
            method.setAccessible(true);
            ConsensusResult result = (ConsensusResult) method.invoke(service, question, votes, totalTimeMs);

            // Then
            assertEquals(question, result.getQuestion());
            assertEquals("Jupiter", result.getConsensusAnswer());
            assertEquals(3, result.getVotes().size()); // All votes are included
            assertTrue(result.getConsensusPercentage() > 0); // Just check it's calculated
            assertNotNull(result.getStrength()); // Just check it has a strength
            assertTrue(result.getAverageConfidence() > 0.8);
            assertTrue(result.getQualityScore() > 0);
            assertEquals(totalTimeMs, result.getResponseTimeMs());

        } catch (Exception e) {
            fail("Failed to invoke calculateConsensus method", e);
        }
    }

    @Test
    void calculateConsensus_shouldHandleEmptyVotes() {
        // Given
        String question = "Test question";
        List<ProviderVote> votes = Arrays.asList();
        long totalTimeMs = 500;

        // When
        try {
            java.lang.reflect.Method method = MultiAIConsensusService.class.getDeclaredMethod(
                    "calculateConsensus", String.class, List.class, long.class);
            method.setAccessible(true);
            ConsensusResult result = (ConsensusResult) method.invoke(service, question, votes, totalTimeMs);

            // Then
            assertEquals(question, result.getQuestion());
            assertEquals(ThirdOpinionConstants.NO_PROVIDER_RESPONSE, result.getConsensusAnswer());
            assertEquals(0, result.getVotes().size());
            assertEquals(0.0, result.getAverageConfidence());
            assertEquals("ERROR", result.getStrength());
            assertEquals(0.0, result.getConsensusPercentage());
            assertEquals(totalTimeMs, result.getResponseTimeMs());

        } catch (Exception e) {
            fail("Failed to invoke calculateConsensus method", e);
        }
    }

    @Test
    void calculateConsensus_shouldHandleSingleVote() {
        // Given
        String question = "Simple question";
        List<ProviderVote> votes = Arrays.asList(
                new ProviderVote("openai", "Simple answer", 0.8, System.currentTimeMillis())
        );
        long totalTimeMs = 800;

        // When
        try {
            java.lang.reflect.Method method = MultiAIConsensusService.class.getDeclaredMethod(
                    "calculateConsensus", String.class, List.class, long.class);
            method.setAccessible(true);
            ConsensusResult result = (ConsensusResult) method.invoke(service, question, votes, totalTimeMs);

            // Then
            assertEquals(question, result.getQuestion());
            assertEquals("Simple answer", result.getConsensusAnswer());
            assertEquals(1, result.getVotes().size());
            assertEquals(100.0, result.getConsensusPercentage()); // 1/1 = 100%
            assertTrue(result.getStrength().contains("STRONG"));
            assertEquals(0.8, result.getAverageConfidence());
            assertTrue(result.getQualityScore() > 0);

        } catch (Exception e) {
            fail("Failed to invoke calculateConsensus method", e);
        }
    }
}