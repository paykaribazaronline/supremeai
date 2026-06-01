package com.supremeai.service;

import com.supremeai.learning.SupremeLearningOrchestrator;
import com.supremeai.learning.active.ActiveInternetScraper;
import com.supremeai.learning.active.QueryClassifier;
import com.supremeai.provider.StubLocalProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.junit.jupiter.api.Assertions;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NeuralChatServiceTest {

    @Mock
    private SupremeLearningOrchestrator learningOrchestrator;

    @Mock
    private ActiveInternetScraper internetScraper;

    @Mock
    private QueryClassifier queryClassifier;

    @Mock
    private StubLocalProvider stubLocalProvider;

    @Mock
    private UnifiedOfflineKnowledgeService unifiedOfflineKnowledgeService;

    private NeuralChatService neuralChatService;

    @BeforeEach
    void setUp() {
        WebClient.Builder webClientBuilder = WebClient.builder();
        neuralChatService = new NeuralChatService(
                learningOrchestrator,
                internetScraper,
                queryClassifier,
                stubLocalProvider,
                unifiedOfflineKnowledgeService,
                webClientBuilder
        );
    }

    @Test
    void generateIntelligentResponse_CoreKnowledgeDirectQuery_SkipsExternalScraping() {
        String userMessage = "What is llm?";
        when(learningOrchestrator.findCoreKnowledgeSolution(userMessage))
                .thenReturn("LLM is a large language model.");

        Mono<NeuralChatService.NeuralResponse> responseMono = neuralChatService.generateIntelligentResponse(userMessage);

        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    Assertions.assertEquals("LLM is a large language model.", response.getAnswer());
                    Assertions.assertEquals("CORE_ONLY", response.getTier());
                    Assertions.assertEquals("core_knowledge", response.getPipeline());
                    Assertions.assertEquals(1, response.getSources().size());
                    Assertions.assertEquals("Core Knowledge", response.getSources().get(0));
                })
                .verifyComplete();

        verify(internetScraper, never()).scrapeKnowledge(anyString(), anyList());
    }
}
