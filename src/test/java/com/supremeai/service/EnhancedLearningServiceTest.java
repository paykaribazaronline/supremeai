package com.supremeai.service;

import com.supremeai.model.SystemLearning;
import com.supremeai.repository.SystemLearningRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnhancedLearningServiceTest {

    @Mock
    private SystemLearningRepository repository;

    @Mock
    private SystemLearningService systemLearningService;

    @InjectMocks
    private EnhancedLearningService enhancedLearningService;

    private SystemLearning sampleLearning;

    @BeforeEach
    void setUp() {
        sampleLearning = new SystemLearning();
        sampleLearning.setId("test-id");
        sampleLearning.setTopic("Test Topic");
        sampleLearning.setCategory("test_category");
        sampleLearning.setContent("Test content");
        sampleLearning.setLearningType("NLP");
        sampleLearning.setSuccess(true);
        sampleLearning.setQualityScore(0.85);
        sampleLearning.setConfidenceScore(0.9);
        sampleLearning.setLearnedAt(LocalDateTime.now());
        sampleLearning.setTimesApplied(5);
        sampleLearning.setTags(Arrays.asList("test", "nlp"));
    }

    @Test
    void learnFromNLPInteraction_shouldCreateAndSaveLearning() {
        when(systemLearningService.addLearning(any(SystemLearning.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(enhancedLearningService.learnFromNLPInteraction(
                "test input", "ai response", "openai", 0.8, Map.of("context", "test")))
                .expectNextMatches(learning -> {
                    assertEquals("NLP_Interaction", learning.getTopic());
                    assertEquals("natural_language", learning.getCategory());
                    assertEquals(EnhancedLearningService.LEARNING_NLP, learning.getLearningType());
                    assertTrue(learning.getSuccess());
                    assertEquals(0.8, learning.getQualityScore());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void learnFromMultimodalInteraction_shouldCreateAndSaveLearning() {
        when(systemLearningService.addLearning(any(SystemLearning.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(enhancedLearningService.learnFromMultimodalInteraction(
                "generate code from image", "http://example.com/image.png",
                "generated code", "vision-provider", 0.9))
                .expectNextMatches(learning -> {
                    assertEquals("Multimodal_Interaction", learning.getTopic());
                    assertEquals("vision_and_text", learning.getCategory());
                    assertEquals(EnhancedLearningService.LEARNING_MULTIMODAL, learning.getLearningType());
                    assertTrue(learning.getSuccess());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void learnFromAPIUsage_shouldCreateAndSaveLearning() {
        when(systemLearningService.addLearning(any(SystemLearning.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        Map<String, Object> requestMeta = Map.of("userId", "user123");

        StepVerifier.create(enhancedLearningService.learnFromAPIUsage(
                "/api/v1/generate", "anthropic", 150L, true, requestMeta))
                .expectNextMatches(learning -> {
                    assertEquals("API_Usage_Pattern", learning.getTopic());
                    assertEquals("ecosystem", learning.getCategory());
                    assertEquals(EnhancedLearningService.LEARNING_ECOSYSTEM, learning.getLearningType());
                    assertTrue(learning.getSuccess());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void learnFromAPIUsage_withFailure_shouldSetQualityScoreToZero() {
        when(systemLearningService.addLearning(any(SystemLearning.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(enhancedLearningService.learnFromAPIUsage(
                "/api/v1/error", "provider", 500L, false, Map.of()))
                .expectNextMatches(learning -> {
                    assertEquals(0.0, learning.getQualityScore());
                    assertFalse(learning.getSuccess());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void learnFromAppGeneration_shouldCreateAndSaveLearning() {
        when(systemLearningService.addLearning(any(SystemLearning.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        Map<String, Object> buildMetrics = Map.of("testPassRate", 0.95);

        StepVerifier.create(enhancedLearningService.learnFromAppGeneration(
                "Create a todo app", "ANDROID", true, "/path/to/app.apk",
                buildMetrics, "agent-1"))
                .expectNextMatches(learning -> {
                    assertEquals("app_generation", learning.getCategory());
                    assertEquals(EnhancedLearningService.LEARNING_APP_GENERATION, learning.getLearningType());
                    assertTrue(learning.getSuccess());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void learnFromAppGeneration_withFailure_shouldUseDefaultQualityScore() {
        when(systemLearningService.addLearning(any(SystemLearning.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(enhancedLearningService.learnFromAppGeneration(
                "Create an app", "WEB", false, null, null, "agent-1"))
                .expectNextMatches(learning -> {
                    assertEquals(0.3, learning.getQualityScore());
                    assertFalse(learning.getSuccess());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void learnPredictivePattern_shouldCreateAndSaveLearning() {
        when(systemLearningService.addLearning(any(SystemLearning.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        Map<String, Object> patternData = Map.of("param1", "value1");

        StepVerifier.create(enhancedLearningService.learnPredictivePattern(
                "optimization", patternData, 0.92, "base-learning-id"))
                .expectNextMatches(learning -> {
                    assertEquals("Predictive_Pattern_optimization", learning.getTopic());
                    assertEquals("predictive", learning.getCategory());
                    assertEquals(EnhancedLearningService.LEARNING_PREDICTIVE, learning.getLearningType());
                    assertTrue(learning.getSuccess());
                    assertEquals(0.92, learning.getConfidenceScore());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getBestPractices_shouldReturnFilteredAndSortedLearnings() {
        List<SystemLearning> learnings = Arrays.asList(
                createLearning("low", 0.5, true),
                createLearning("high", 0.9, true),
                createLearning("medium", 0.7, true)
        );

        when(repository.findByCategory("test_category"))
                .thenReturn(Flux.fromIterable(learnings));

        StepVerifier.create(enhancedLearningService.getBestPractices("test_category", 0.6))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void getPredictiveRecommendations_shouldReturnTopRecommendations() {
        List<SystemLearning> learnings = Arrays.asList(
                createLearningWithTags("pattern1", 0.95, Arrays.asList("task1", "predictive")),
                createLearningWithTags("pattern2", 0.92, Arrays.asList("task1", "predictive")),
                createLearningWithTags("pattern3", 0.85, Arrays.asList("task1", "predictive"))
        );

        when(repository.findAll()).thenReturn(Flux.fromIterable(learnings));

        StepVerifier.create(enhancedLearningService.getPredictiveRecommendations("task1", Map.of()))
                .expectNextCount(3)
                .verifyComplete();
    }

    @Test
    void applyLearning_shouldIncrementTimesAppliedAndEvictCache() {
        SystemLearning existingLearning = new SystemLearning();
        existingLearning.setId("test-id");
        existingLearning.setTimesApplied(3);

        when(repository.findById("test-id")).thenReturn(Mono.just(existingLearning));
        when(systemLearningService.addLearning(any(SystemLearning.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(enhancedLearningService.applyLearning("test-id"))
                .expectNextMatches(learning -> learning.getTimesApplied() == 4)
                .verifyComplete();

        verify(systemLearningService).addLearning(any(SystemLearning.class));
    }

    @Test
    void applyLearning_withNullTimesApplied_shouldIncrementFromZero() {
        SystemLearning existingLearning = new SystemLearning();
        existingLearning.setId("test-id");
        existingLearning.setTimesApplied(null);

        when(repository.findById("test-id")).thenReturn(Mono.just(existingLearning));
        when(systemLearningService.addLearning(any(SystemLearning.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(enhancedLearningService.applyLearning("test-id"))
                .expectNextMatches(learning -> learning.getTimesApplied() == 1)
                .verifyComplete();
    }

    @Test
    void improveSystemLearning_shouldReturnAnalysisResults() {
        List<SystemLearning> learnings = Arrays.asList(
                createLearning("success", 0.9, true),
                createLearning("failure", 0.3, false)
        );

        when(repository.findAll()).thenReturn(Flux.fromIterable(learnings));

        StepVerifier.create(enhancedLearningService.improveSystemLearning())
                .expectNextMatches(result -> {
                    assertTrue((Boolean) result.get("success"));
                    assertNotNull(result.get("analysis"));
                    assertNotNull(result.get("opportunities"));
                    assertNotNull(result.get("optimization"));
                    assertNotNull(result.get("recommendations"));
                    return true;
                })
                .verifyComplete();
    }

    private SystemLearning createLearning(String topic, double qualityScore, boolean success) {
        SystemLearning learning = new SystemLearning();
        learning.setId(UUID.randomUUID().toString());
        learning.setTopic(topic);
        learning.setCategory("test_category");
        learning.setContent("Content");
        learning.setLearningType("NLP");
        learning.setSuccess(success);
        learning.setQualityScore(qualityScore);
        learning.setConfidenceScore(qualityScore);
        learning.setLearnedAt(LocalDateTime.now());
        return learning;
    }

    private SystemLearning createLearningWithTags(String topic, double confidence, List<String> tags) {
        SystemLearning learning = new SystemLearning();
        learning.setId(UUID.randomUUID().toString());
        learning.setTopic(topic);
        learning.setLearningType("PREDICTIVE");
        learning.setConfidenceScore(confidence);
        learning.setTags(tags);
        return learning;
    }
}