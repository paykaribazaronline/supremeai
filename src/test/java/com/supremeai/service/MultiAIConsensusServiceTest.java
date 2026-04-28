package com.supremeai.service;

import com.supremeai.model.ConsensusResult;
import com.supremeai.model.ConsensusVote;
import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MultiAIConsensusServiceTest {

    @Mock
    private AIProviderFactory providerFactory;

    @Mock
    private SelfHealingService selfHealingService;

    @Mock
    private KnowledgeFeedbackService feedbackService;

    @Mock
    private ContextualAIRankingService contextualRankingService;

    @Mock
    private AIProvider groqProvider;

    @Mock
    private AIProvider openaiProvider;

    private MultiAIConsensusService consensusService;

    @BeforeEach
    void setUp() throws Exception {
        consensusService = new MultiAIConsensusService();
        injectField("providerFactory", providerFactory);
        injectField("selfHealingService", selfHealingService);
        injectField("feedbackService", feedbackService);
        injectField("contextualRankingService", contextualRankingService);

        lenient().when(selfHealingService.executeWithRetry(any(), anyInt(), anyLong()))
            .thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                Supplier<Mono<String>> supplier = invocation.getArgument(0);
                return supplier.get();
            });
        lenient().when(contextualRankingService.detectTaskType(any()))
            .thenReturn(ContextualAIRankingService.TaskType.QUESTION_ANSWERING);
    }

    @Test
    void askAllAIsReturnsStrongConsensusForMatchingResponses() {
        when(providerFactory.getProvider("groq")).thenReturn(groqProvider);
        when(providerFactory.getProvider("openai")).thenReturn(openaiProvider);
        when(groqProvider.generate("What is the capital of France?")).thenReturn(Mono.just("Paris"));
        when(openaiProvider.generate("What is the capital of France?")).thenReturn(Mono.just("Paris"));

        StepVerifier.create(consensusService.askAllAIs(
                "What is the capital of France?",
                List.of("groq", "openai"),
                5_000L))
            .assertNext(result -> {
                assertEquals("Paris", result.getConsensusAnswer());
                assertEquals(2, result.getVotes().size());
                assertTrue(result.getStrength().contains("STRONG"));
            })
            .verifyComplete();
    }

    @Test
    void askAllAIsReturnsErrorWhenAllProvidersFail() {
        when(providerFactory.getProvider("groq")).thenReturn(groqProvider);
        when(groqProvider.generate(anyString())).thenReturn(Mono.error(new RuntimeException("API down")));

        StepVerifier.create(consensusService.askAllAIs("Test", List.of("groq"), 1_000L))
            .assertNext(result -> {
                assertEquals("No AI providers responded", result.getConsensusAnswer());
                assertEquals("ERROR", result.getStrength());
                assertEquals(0, result.getVotes().size());
            })
            .verifyComplete();
    }

    @Test
    void askAllAIsReturnsTimeoutResultWhenProviderNeverCompletes() {
        when(providerFactory.getProvider("groq")).thenReturn(groqProvider);
        when(groqProvider.generate(anyString())).thenReturn(Mono.never());

        StepVerifier.create(consensusService.askAllAIs("Slow question", List.of("groq"), 20L))
            .assertNext(result -> {
                assertEquals("Timeout reached", result.getConsensusAnswer());
                assertEquals("TIMEOUT", result.getStrength());
            })
            .verifyComplete();
    }

    @Test
    void getHistoryReturnsMostRecentVotesFirst() {
        when(providerFactory.getProvider("groq")).thenReturn(groqProvider);
        when(groqProvider.generate("Q1")).thenReturn(Mono.just("A1"));
        when(groqProvider.generate("Q2")).thenReturn(Mono.just("A2"));

        consensusService.askAllAIs("Q1", List.of("groq"), 5_000L).block();
        consensusService.askAllAIs("Q2", List.of("groq"), 5_000L).block();

        Flux<ConsensusVote> history = consensusService.getHistory(2);

        StepVerifier.create(history)
            .assertNext(vote -> assertEquals("Q2", vote.getQuestion()))
            .assertNext(vote -> assertEquals("Q1", vote.getQuestion()))
            .verifyComplete();
    }

    @Test
    void askContextualAIsUsesRankedProviderThenFallbacks() {
        ContextualAIRankingService.ProviderSelection selection =
            new ContextualAIRankingService.ProviderSelection(
                "openai",
                ContextualAIRankingService.TaskType.CODE_GENERATION,
                0.9,
                "best");
        List<ContextualAIRankingService.ProviderRanking> rankings = List.of(
            new ContextualAIRankingService.ProviderRanking("openai", "CODE_GENERATION", 0.9, 100, 4.0, 10),
            new ContextualAIRankingService.ProviderRanking("groq", "CODE_GENERATION", 0.8, 200, 4.0, 10)
        );

        when(contextualRankingService.selectBestProvider("Build a login page", null)).thenReturn(selection);
        when(contextualRankingService.getRankingsForTask(ContextualAIRankingService.TaskType.CODE_GENERATION))
            .thenReturn(rankings);
        when(providerFactory.getAllProviderNames()).thenReturn(new String[] {"openai", "groq", "anthropic"});
        when(providerFactory.getProvider("openai")).thenReturn(openaiProvider);
        when(providerFactory.getProvider("groq")).thenReturn(groqProvider);
        AIProvider anthropicProvider = mock(AIProvider.class);
        when(providerFactory.getProvider("anthropic")).thenReturn(anthropicProvider);
        when(openaiProvider.generate("Build a login page")).thenReturn(Mono.just("Use React"));
        when(groqProvider.generate("Build a login page")).thenReturn(Mono.just("Use React"));
        when(anthropicProvider.generate("Build a login page")).thenReturn(Mono.just("Use Vue"));

        StepVerifier.create(consensusService.askContextualAIs("Build a login page", 3, 5_000L))
            .assertNext(result -> {
                assertEquals("Use React", result.getConsensusAnswer());
                assertEquals(3, result.getVotes().size());
            })
            .verifyComplete();
    }

    private void injectField(String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = MultiAIConsensusService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(consensusService, value);
    }
}
