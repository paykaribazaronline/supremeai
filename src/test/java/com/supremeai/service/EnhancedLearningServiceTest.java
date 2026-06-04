package com.supremeai.service;

import com.supremeai.model.APIProvider;
import com.supremeai.model.SystemLearning;
import com.supremeai.model.UserTier;
import com.supremeai.repository.ProviderRepository;
import com.supremeai.repository.SystemLearningRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EnhancedLearningServiceTest {

    @Mock
    private SystemLearningRepository repository;

    @Mock
    private SystemLearningService systemLearningService;

    @InjectMocks
    private EnhancedLearningService enhancedLearningService;

    @BeforeEach
    void setUp() {
        // Make systemLearningService.addLearning() return the actual learning object.
        // This preserves all fields set by EnhancedLearningService, while verifying
        // that repository.save() was still called (when tests check it).
        doAnswer(invocation -> Mono.just(invocation.getArgument(0)))
                .when(systemLearningService).addLearning(any(SystemLearning.class));
    }

    // ==================== learnFromNLPInteraction Tests ====================

    @Test
    void learnFromNLPInteraction_ValidInput_ReturnsSavedLearning() {
        SystemLearning saved = new SystemLearning();
        saved.setId("learn-new");
        saved.setTopic("NLP_Interaction");
        saved.setCategory("natural_language");
        saved.setSuccess(true);

        when(repository.save(any(SystemLearning.class))).thenReturn(Mono.just(saved));

        Mono<SystemLearning> result = enhancedLearningService.learnFromNLPInteraction(
                "Write Python code", "Python is great", "gpt4", 0.9,
                Map.of("context", "test")
        );

        StepVerifier.create(result)
                .expectNextMatches(l ->
                        "NLP_Interaction".equals(l.getTopic()) &&
                        "natural_language".equals(l.getCategory()) &&
                        "NLP".equals(l.getLearningType()))
                .verifyComplete();

        verify(systemLearningService).addLearning(any(SystemLearning.class));
    }

    @Test
    void learnFromNLPInteraction_NullQualityScore_SavesWithSuccessFalse() {
        SystemLearning saved = new SystemLearning();
        saved.setId("learn-new");

        when(repository.save(any(SystemLearning.class))).thenReturn(Mono.just(saved));

        Mono<SystemLearning> result = enhancedLearningService.learnFromNLPInteraction(
                "Short", "Response", "provider", 0.5, null
        );

        StepVerifier.create(result)
                .expectNextMatches(l -> !l.getSuccess())
                .verifyComplete();
    }

    // ==================== learnFromMultimodalInteraction Tests ====================

    @Test
    void learnFromMultimodalInteraction_ValidInput_ReturnsSavedLearning() {
        SystemLearning saved = new SystemLearning();
        saved.setId("learn-mm");
        saved.setTopic("Multimodal_Interaction");
        saved.setCategory("vision_and_text");

        when(repository.save(any(SystemLearning.class))).thenReturn(Mono.just(saved));

        Mono<SystemLearning> result = enhancedLearningService.learnFromMultimodalInteraction(
                "Generate a button", "http://example.com/image.png",
                "<button>Click me</button>", "gemini", 0.85
        );

        StepVerifier.create(result)
                .expectNextMatches(l ->
                        "Multimodal_Interaction".equals(l.getTopic()) &&
                        "vision_and_text".equals(l.getCategory()))
                .verifyComplete();

        verify(systemLearningService).addLearning(any(SystemLearning.class));
    }

    @Test
    void learnFromMultimodalInteraction_LowAccuracy_SavesWithSuccessFalse() {
        SystemLearning saved = new SystemLearning();
        saved.setId("learn-mm-2");

        when(repository.save(any(SystemLearning.class))).thenReturn(Mono.just(saved));

        Mono<SystemLearning> result = enhancedLearningService.learnFromMultimodalInteraction(
                "Complex request", "http://img.png", "bad output", "ollama", 0.5
        );

        StepVerifier.create(result)
                .expectNextMatches(l -> !l.getSuccess())
                .verifyComplete();
    }

    // ==================== learnFromAPIUsage Tests ====================

    @Test
    void learnFromAPIUsage_SuccessfulCall_ReturnsSavedLearning() {
        SystemLearning saved = new SystemLearning();
        saved.setId("learn-api");
        saved.setTopic("API_Usage_Pattern");

        when(repository.save(any(SystemLearning.class))).thenReturn(Mono.just(saved));

        Mono<SystemLearning> result = enhancedLearningService.learnFromAPIUsage(
                "/api/generate", "openai", 150L, true,
                Map.of("model", "gpt4")
        );

        StepVerifier.create(result)
                .expectNextMatches(l -> {
                    assertEquals("API_Usage_Pattern", l.getTopic());
                    assertEquals("ecosystem", l.getCategory());
                    assertTrue(l.getSuccess());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void learnFromAPIUsage_FailedCall_SavesWithSuccessFalse() {
        SystemLearning saved = new SystemLearning();
        saved.setId("learn-api-2");

        when(repository.save(any(SystemLearning.class))).thenReturn(Mono.just(saved));

        Mono<SystemLearning> result = enhancedLearningService.learnFromAPIUsage(
                "/api/generate", "openai", 5000L, false, null
        );

        StepVerifier.create(result)
                .expectNextMatches(l -> !l.getSuccess())
                .verifyComplete();
    }

    // ==================== learnFromAppGeneration Tests ====================

    @Test
    void learnFromAppGeneration_SuccessfulBuild_ReturnsSavedLearning() {
        SystemLearning saved = new SystemLearning();
        saved.setId("learn-app");

        when(repository.save(any(SystemLearning.class))).thenReturn(Mono.just(saved));

        Mono<SystemLearning> result = enhancedLearningService.learnFromAppGeneration(
                "Build a todo app", "React", true, "/build/app.apk",
                Map.of("testPassRate", 0.95), "gpt4"
        );

        StepVerifier.create(result)
                .expectNextMatches(l -> {
                    assertTrue(l.getSuccess());
                    assertEquals(0.95, l.getQualityScore());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void learnFromAppGeneration_FailedBuild_SavesWithLowQuality() {
        SystemLearning saved = new SystemLearning();
        saved.setId("learn-app-2");

        when(repository.save(any(SystemLearning.class))).thenReturn(Mono.just(saved));

        Mono<SystemLearning> result = enhancedLearningService.learnFromAppGeneration(
                "Build a complex app", "Flutter", false, null, null, "claude"
        );

        StepVerifier.create(result)
                .expectNextMatches(l -> {
                    assertFalse(l.getSuccess());
                    assertEquals(0.3, l.getQualityScore());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void learnFromAppGeneration_NullBuildMetrics_UsesDefaultQuality() {
        SystemLearning saved = new SystemLearning();
        saved.setId("learn-app-3");

        when(repository.save(any(SystemLearning.class))).thenReturn(Mono.just(saved));

        Mono<SystemLearning> result = enhancedLearningService.learnFromAppGeneration(
                "Simple app", "web", true, null, null, "gemini"
        );

        StepVerifier.create(result)
                .expectNextMatches(l -> l.getQualityScore() == 1.0)
                .verifyComplete();
    }

    // ==================== learnPredictivePattern Tests ====================

    @Test
    void learnPredictivePattern_ValidPattern_ReturnsSavedLearning() {
        SystemLearning saved = new SystemLearning();
        saved.setId("learn-pred");

        when(repository.save(any(SystemLearning.class))).thenReturn(Mono.just(saved));

        Mono<SystemLearning> result = enhancedLearningService.learnPredictivePattern(
                "code_style", Map.of("pattern", "camelCase"), 0.88, "learn-1"
        );

        StepVerifier.create(result)
                .expectNextMatches(l -> {
                    assertEquals("Predictive_Pattern_code_style", l.getTopic());
                    assertEquals("predictive", l.getCategory());
                    assertEquals(0.88, l.getConfidenceScore());
                    return true;
                })
                .verifyComplete();
    }

    // ==================== getBestPractices Tests ====================

    @Test
    void getBestPractices_ValidCategory_ReturnsFilteredFlux() {
        SystemLearning bp1 = new SystemLearning();
        bp1.setCategory("code_generation");
        bp1.setQualityScore(0.92);
        bp1.setSuccess(true);

        SystemLearning bp2 = new SystemLearning();
        bp2.setCategory("code_generation");
        bp2.setQualityScore(0.88);
        bp2.setSuccess(true);

        SystemLearning lowQuality = new SystemLearning();
        lowQuality.setCategory("code_generation");
        lowQuality.setQualityScore(0.5);
        lowQuality.setSuccess(true);

        when(repository.findByCategory("code_generation"))
                .thenReturn(Flux.just(bp1, bp2, lowQuality));

        Flux<SystemLearning> result = enhancedLearningService.getBestPractices("code_generation", 0.8);

        StepVerifier.create(result)
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void getBestPractices_EmptyCategory_ReturnsEmptyFlux() {
        when(repository.findByCategory("unknown")).thenReturn(Flux.empty());

        Flux<SystemLearning> result = enhancedLearningService.getBestPractices("unknown", 0.8);

        StepVerifier.create(result)
                .verifyComplete();
    }

    // ==================== getPredictiveRecommendations Tests ====================

    @Test
    void getPredictiveRecommendations_MatchingTask_ReturnsRecommendations() {
        SystemLearning rec1 = new SystemLearning();
        rec1.setLearningType("PREDICTIVE");
        rec1.setTags(Arrays.asList("predictive", "code_generation", "pattern_matching"));
        rec1.setConfidenceScore(0.9);

        SystemLearning rec2 = new SystemLearning();
        rec2.setLearningType("NLP");
        rec2.setTags(Arrays.asList("nlp", "test"));
        rec2.setConfidenceScore(0.8);

        when(repository.findAll()).thenReturn(Flux.just(rec1, rec2));

        Flux<SystemLearning> result = enhancedLearningService.getPredictiveRecommendations("code_generation", Map.of());

        StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void getPredictiveRecommendations_NoMatch_ReturnsEmptyFlux() {
        when(repository.findAll()).thenReturn(Flux.empty());

        Flux<SystemLearning> result = enhancedLearningService.getPredictiveRecommendations("unknown", Map.of());

        StepVerifier.create(result)
                .verifyComplete();
    }

    // ==================== applyLearning Tests ====================

    @Test
    void applyLearning_ExistingId_IncrementsTimesApplied() {
        SystemLearning existing = new SystemLearning();
        existing.setId("learn-1");
        existing.setTimesApplied(5);

        SystemLearning updated = new SystemLearning();
        updated.setId("learn-1");
        updated.setTimesApplied(6);

        when(repository.findById("learn-1")).thenReturn(Mono.just(existing));
        when(repository.save(any(SystemLearning.class))).thenReturn(Mono.just(updated));

        Mono<SystemLearning> result = enhancedLearningService.applyLearning("learn-1");

        StepVerifier.create(result)
                .expectNextMatches(l -> l.getTimesApplied() == 6)
                .verifyComplete();
    }

    @Test
    void applyLearning_NonExistentId_ReturnsEmpty() {
        when(repository.findById("nonexistent")).thenReturn(Mono.empty());

        Mono<SystemLearning> result = enhancedLearningService.applyLearning("nonexistent");

        StepVerifier.create(result)
                .verifyComplete();
    }

    // ==================== getLearningStats Tests ====================

    @Test
    void getLearningStats_WithLearnings_ReturnsCorrectStats() {
        SystemLearning l1 = new SystemLearning();
        l1.setLearningType("NLP");
        l1.setSuccess(true);
        l1.setQualityScore(0.9);

        SystemLearning l2 = new SystemLearning();
        l2.setLearningType("NLP");
        l2.setSuccess(false);
        l2.setQualityScore(0.5);

        SystemLearning l3 = new SystemLearning();
        l3.setLearningType("MULTIMODAL");
        l3.setSuccess(true);
        l3.setQualityScore(0.8);

        when(repository.findAll()).thenReturn(Flux.just(l1, l2, l3));

        Mono<Map<String, Object>> result = enhancedLearningService.getLearningStats();

        StepVerifier.create(result)
                .expectNextMatches(stats -> {
                    assertEquals(3, stats.get("total"));
                    assertEquals(2L, stats.get("successCount"));
                    assertEquals(1L, stats.get("failureCount"));
                    assertEquals(0.73, (double) stats.get("averageQuality"), 0.01);
                    assertEquals(2.0/3.0, (double) stats.get("successRate"), 0.01);
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getLearningStats_EmptyRepository_ReturnsZeroStats() {
        when(repository.findAll()).thenReturn(Flux.empty());

        Mono<Map<String, Object>> result = enhancedLearningService.getLearningStats();

        StepVerifier.create(result)
                .expectNextMatches(stats -> {
                    assertEquals(0, stats.get("total"));
                    return true;
                })
                .verifyComplete();
    }

    // ==================== improveSystemLearning Tests ====================

    @Test
    void improveSystemLearning_WithLearnings_ReturnsAnalysis() {
        SystemLearning l1 = new SystemLearning();
        l1.setId("l1");
        l1.setTopic("Test Topic 1");
        l1.setLearningType("NLP");
        l1.setSuccess(true);
        l1.setQualityScore(0.9);
        l1.setTimesApplied(3);
        l1.setLearnedAt(LocalDateTime.now().minusDays(90)); // Old

        SystemLearning l2 = new SystemLearning();
        l2.setId("l2");
        l2.setTopic("Test Topic 2");
        l2.setLearningType("ECOSYSTEM");
        l2.setSuccess(false);
        l2.setQualityScore(0.3);
        l2.setTimesApplied(10);
        l2.setLearnedAt(LocalDateTime.now());

        when(repository.findAll()).thenReturn(Flux.just(l1, l2));

        Mono<Map<String, Object>> result = enhancedLearningService.improveSystemLearning();

        StepVerifier.create(result)
                .expectNextMatches(res -> {
                    assertTrue((Boolean) res.get("success"));
                    assertEquals(2, ((Map<?, ?>) res.get("summary")).get("totalLearningsAnalyzed"));
                    assertNotNull(res.get("analysis"));
                    assertNotNull(res.get("opportunities"));
                    assertNotNull(res.get("optimization"));
                    assertNotNull(res.get("recommendations"));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void improveSystemLearning_EmptyLearnings_ReturnsDefaultResults() {
        when(repository.findAll()).thenReturn(Flux.empty());

        Mono<Map<String, Object>> result = enhancedLearningService.improveSystemLearning();

        StepVerifier.create(result)
                .expectNextMatches(res -> {
                    assertTrue((Boolean) res.get("success"));
                    assertEquals(0, ((Map<?, ?>) res.get("summary")).get("totalLearningsAnalyzed"));
                    return true;
                })
                .verifyComplete();
    }
}