package com.supremeai.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.supremeai.fallback.ThirdOpinionOrchestrator;
import com.supremeai.learning.SupremeLearningOrchestrator;
import com.supremeai.learning.knowledge.GlobalKnowledgeBase;
import com.supremeai.model.SupremeAIResponse;
import com.supremeai.model.UserContext;
import com.supremeai.provider.AIProviderFactory;
import com.supremeai.repository.APIHealthReportRepository;
import com.supremeai.repository.ProviderRepository;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

/**
 * Isolated unit tests for SelfHealingService.analyseError().
 *
 * <p>Avoids Mockito mock-matching ambiguity by constructing real lightweight
 * RootCauseAnalysisService stubs (anonymous subclasses) and injecting them via reflection. Only the
 * knowledge-base side-effects are mocked.
 */
@ExtendWith(MockitoExtension.class)
class SelfHealingServiceHappypathTest {

  @Mock GlobalKnowledgeBase globalKnowledgeBase;
  @Mock SupremeLearningOrchestrator learningOrchestrator;
  @Mock ThirdOpinionOrchestrator fallbackOrchestrator;
  @Mock MultiAIVotingService votingService;
  @Mock ProviderRepository providerRepository;
  @Mock AIProviderFactory providerFactory;
  @Mock APIHealthReportRepository healthReportRepository;

  SelfHealingService shs;

  /**
   * Stub RCA: returns canAutoFix=true, confidence=0.93. recordSuccessfulCorrection() returns
   * Mono.empty() (no private-field access).
   */
  private static RootCauseAnalysisService autoFixRca() {
    return new RootCauseAnalysisService(null) {
      @Override
      public RootCauseAnalysis analyzeError(
          String errorSignature, String errorMessage, String codeContext) {
        return new RootCauseAnalysis(
            errorSignature,
            "null_pointer",
            "Null pointer — fixed by null guard",
            0.93,
            0.0,
            CorrectionAction.AUTO_FIX_NULL,
            "Objects.requireNonNull(x);",
            true,
            java.time.LocalDateTime.now());
      }

      @Override // returns non-null Mono<Void> so SHS.subscribe() is safe
      public Mono<Void> recordSuccessfulCorrection(String errorSignature, String correctedCode) {
        return Mono.empty();
      }
    };
  }

  /** Stub RCA: returns canAutoFix=false, confidence=0.60 (> 0.5 → review path). */
  private static RootCauseAnalysisService reviewRca() {
    return new RootCauseAnalysisService(null) {
      @Override
      public RootCauseAnalysis analyzeError(
          String errorSignature, String errorMessage, String codeContext) {
        return new RootCauseAnalysis(
            errorSignature,
            "missing_import",
            "Missing import — needs review",
            0.60,
            0.0,
            CorrectionAction.MANUAL_REVIEW,
            null,
            false,
            java.time.LocalDateTime.now());
      }
    };
  }

  /** Stub RCA: always throws — simulates provider outage. */
  private static RootCauseAnalysisService throwingRca() {
    return new RootCauseAnalysisService(null) {
      @Override
      public RootCauseAnalysis analyzeError(
          String errorSignature, String errorMessage, String codeContext) {
        throw new RuntimeException("RCA unavailable");
      }
    };
  }

  /** Stub RCA: returns null analysis. */
  private static RootCauseAnalysisService nullMonoRca() {
    return new RootCauseAnalysisService(null) {
      @Override
      public RootCauseAnalysis analyzeError(
          String errorSignature, String errorMessage, String codeContext) {
        return null;
      }
    };
  }

  // ─── Test lifecycle ────────────────────────────────────────────────────

  @BeforeEach
  void setUp() throws Exception {
    shs = new SelfHealingService();
    inject(shs, "rootCauseAnalysisService", autoFixRca()); // default: auto-fix
    inject(shs, "globalKnowledgeBase", globalKnowledgeBase);
    inject(shs, "learningOrchestrator", learningOrchestrator);
    inject(shs, "fallbackOrchestrator", fallbackOrchestrator);
    inject(shs, "votingService", votingService);
    inject(shs, "providerRepository", providerRepository);
    inject(shs, "providerFactory", providerFactory);
    inject(shs, "healthReportRepository", healthReportRepository);

    lenient()
        .when(
            globalKnowledgeBase.recordSuccessWithPermission(
                anyString(), anyString(), anyString(), anyLong(), anyDouble()))
        .thenReturn(Mono.empty());
  }

  private static void inject(Object target, String fieldName, Object value) {
    try {
      Field f = target.getClass().getDeclaredField(fieldName);
      f.setAccessible(true);
      f.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static UserContext ctx(String code) {
    UserContext uc = new UserContext();
    uc.setCodeContext(code);
    return uc;
  }

  // ─── Tests ─────────────────────────────────────────────────────────────

  /**
   * GIVEN RCA analyses returns canAutoFix=true and confidence=0.93 (> 0.8) WHEN
   * SelfHealingService.analyzeError() is called THEN the response is success=true and carries the
   * RCA analysis.
   */
  @Test
  void autoFixPath_returnsSuccessResponse() {
    SupremeAIResponse resp =
        shs.analyzeError(
            "Cannot invoke toString() on null reference",
            new NullPointerException("null"),
            ctx("x.toString()"));

    assert resp != null : "response must not be null";
    assert resp.isSuccess() : "Expected success=true when RCA.canAutoFix=true and confidence>0.8";
    assert resp.getMessage() != null && !resp.getMessage().isBlank()
        : "Success response must carry a non-empty message";
    assert resp.getRootCauseAnalysis() != null : "Success response must echo the RCA analysis";
  }

  /**
   * GIVEN RCA returns non-auto-fixable analysis with confidence=0.60 (> 0.5) WHEN
   * SelfHealingService.analyzeError() is called THEN the response is non-success but carries RCA
   * details and an explainer.
   */
  @Test
  void reviewPath_nonSuccessWithRcaDetails() {
    inject(shs, "rootCauseAnalysisService", reviewRca());

    SupremeAIResponse resp =
        shs.analyzeError(
            "Compilation failed: cannot find symbol class Foo",
            new RuntimeException("cannot find symbol: class Foo"),
            ctx(""));

    assert resp != null : "response must not be null";
    assert !resp.isSuccess() : "Expected non-success when RCA is not auto-fixable";
    assert resp.getMessage() != null : "Non-success response must carry an explainer message";
    assert resp.getRootCauseAnalysis() != null : "Non-success response must carry the RCA analysis";
  }

  /**
   * GIVEN RCA throws a runtime error WHEN SelfHealingService.analyzeError() is called THEN SHS
   * catches the error, writes an unknown-error artifact to GKB, calls recordFailedCorrection() to
   * learn from the RCA failure, and returns a non-success response.
   */
  @Test
  void rcaThrows_recordsFailureOnMlPredictor() {
    java.util.concurrent.atomic.AtomicInteger recordFailedCalls =
        new java.util.concurrent.atomic.AtomicInteger(0);

    RootCauseAnalysisService trackingRca =
        new RootCauseAnalysisService(null) {
          @Override
          public RootCauseAnalysis analyzeError(
              String errorSignature, String errorMessage, String codeContext) {
            throw new RuntimeException("RCA unavailable");
          }

          @Override
          public void recordFailedCorrection(
              String errorSignature, String errorMessage, String codeContext) {
            recordFailedCalls.incrementAndGet();
          }
        };

    inject(shs, "rootCauseAnalysisService", trackingRca);

    SupremeAIResponse resp =
        shs.analyzeError("Something broke", new RuntimeException("rca-down"), new UserContext());

    assert resp != null : "response must not be null when RCA throws";
    assert !resp.isSuccess() : "Expected failure when RCA is unavailable";
    verify(globalKnowledgeBase)
        .recordSuccessWithPermission(anyString(), anyString(), anyString(), anyLong(), anyDouble());
    verify(learningOrchestrator, atLeastOnce()).logUnknownError(anyString(), anyString());
    assert recordFailedCalls.get() == 1
        : "Expected recordFailedCorrection() to be called once; was called "
            + recordFailedCalls.get();
  }

  /**
   * GIVEN userContext is null (production edge case) WHEN SelfHealingService.analyzeError() is
   * called THEN SHS replaces null with "" for codeContext and does not throw.
   */
  @Test
  void nullUserContext_replaceWithEmptyCodeContext() {
    inject(shs, "rootCauseAnalysisService", autoFixRca());

    SupremeAIResponse resp =
        shs.analyzeError("edge-case error", new RuntimeException("null-ctx"), null);

    assert resp != null : "Must return a response even with null UserContext";
    assert resp.isSuccess() : "Auto-fixable edge case should still succeed";
  }

  /**
   * GIVEN RCA service is null (all external providers down / solo mode) WHEN
   * SelfHealingService.analyzeError() is called THEN SHS records unknown error to GKB and returns
   * non-success response without throwing. This codifies the rule: "no RCA → no crash → knowledge
   * artifact always created first".
   */
  @Test
  void soloMode_nullRca_recordsUnknownErrorAndReturnsNonSuccess() {
    inject(shs, "rootCauseAnalysisService", null);

    SupremeAIResponse resp =
        shs.analyzeError(
            "Connection refused", new java.net.ConnectException("No AI available"), ctx(""));

    assert resp != null : "response must not be null in solo mode";
    assert !resp.isSuccess() : "Expected non-success when RCA is null (solo mode fallback)";
    assert resp.getMessage() != null : "Non-success response must carry an explainer message";
    verify(globalKnowledgeBase)
        .recordSuccessWithPermission(anyString(), anyString(), anyString(), anyLong(), anyDouble());
    verify(learningOrchestrator, atLeastOnce()).logUnknownError(anyString(), anyString());
  }

  /**
   * GIVEN ProviderRepository has zero active entries WHEN a new ThirdOpinionOrchestrator is
   * constructed and init() completes THEN getSoloMode() returns true — solo mode is automatically
   * detected and flagged.
   */
  @Test
  void soloMode_noActiveProviders_setsSoloFlag() {
    when(providerRepository.findAll()).thenReturn(reactor.core.publisher.Flux.empty());

    ThirdOpinionOrchestrator orchestrator =
        new ThirdOpinionOrchestrator(
            mock(com.supremeai.cost.QuotaManager.class),
            mock(GlobalKnowledgeBase.class),
            mock(com.supremeai.learning.immunity.CodeImmunitySystem.class),
            mock(com.supremeai.intelligence.profiling.AIProfiler.class),
            mock(com.supremeai.resilience.RetryableAIExecutor.class),
            mock(com.supremeai.security.ApiKeyRotationService.class),
            mock(AIProviderFactory.class),
            mock(RequestHedgingService.class),
            providerRepository,
            "airllm-sidecar");
    orchestrator.init();
    assert orchestrator.getSoloMode()
        : "Expected soloMode=true when providerRepository returns no active providers";
  }
}
