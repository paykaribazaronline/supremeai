package com.supremeai.service;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Plan 14: Vision & Image Integration (Enhanced).
 *
 * <p>Processes images for: - Screenshot error reading (debug UI issues from screenshots) - UI
 * mockup parsing (extract component structure) - Visual data extraction (charts, tables from
 * images) - OCR-style text extraction from screenshots
 *
 * <p>Uses hybrid approach: Native on-device model first, then external APIs if needed. This reduces
 * latency, cost, and dependency on external services.
 */
@Service
public class VisionService {

  private static final Logger log = LoggerFactory.getLogger(VisionService.class);

  @Value("${openai.api.key:}")
  private String openAiKey;

  @Value("${gemini.api.key:}")
  private String geminiKey;

  @Value("${vision.max.image.size.bytes:4194304}")
  private long maxImageSizeBytes; // 4MB default

  @Autowired(required = false)
  private NativeVisionService nativeVisionService;

  @Autowired private ConfigService configService;

  private final WebClient webClient = WebClient.builder().build();

  // ─── Public API ───────────────────────────────────────────────────────────

  /**
   * Analyze a screenshot for errors, UI issues, or information extraction.
   *
   * @param base64Image Base64-encoded image data
   * @param analysisType Type of analysis: ERROR_DEBUG, UI_PARSE, DATA_EXTRACT, GENERAL
   * @return VisionAnalysisResult with findings
   */
  public Mono<VisionAnalysisResult> analyzeImage(String base64Image, AnalysisType analysisType) {
    if (base64Image == null || base64Image.isEmpty()) {
      return Mono.just(VisionAnalysisResult.error("Empty image data"));
    }

    // Check image size
    long estimatedBytes = (long) (base64Image.length() * 0.75);
    if (estimatedBytes > maxImageSizeBytes) {
      return Mono.just(
          VisionAnalysisResult.error(
              "Image too large: "
                  + (estimatedBytes / 1024)
                  + "KB. Max: "
                  + (maxImageSizeBytes / 1024)
                  + "KB"));
    }

    log.info("[VISION] Analyzing image type={} size=~{}KB", analysisType, estimatedBytes / 1024);

    // First try native on-device processing (faster, no API cost)
    if (nativeVisionService != null && nativeVisionService.isModelLoaded()) {
      return tryNativeVisionFirst(base64Image, analysisType);
    }

    // Fallback to external APIs if native not available
    return tryExternalVisionApis(base64Image, analysisType);
  }

  /** Try native on-device vision processing first for lower latency and cost. */
  private Mono<VisionAnalysisResult> tryNativeVisionFirst(
      String base64Image, AnalysisType analysisType) {
    NativeVisionService.VisionTaskType taskType = mapToNativeTaskType(analysisType);

    return nativeVisionService
        .processImageNative(base64Image, taskType)
        .flatMap(
            nativeResult -> {
              if (nativeResult.isSuccess() && nativeResult.getConfidence() >= 0.75f) {
                log.info(
                    "[VISION] Native processing successful for {}: confidence={}",
                    analysisType,
                    nativeResult.getConfidence());
                return Mono.just(convertNativeResult(nativeResult, analysisType));
              } else {
                log.info(
                    "[VISION] Native processing insufficient (confidence={}), trying external APIs",
                    nativeResult.getConfidence());
                return tryExternalVisionApis(base64Image, analysisType);
              }
            })
        .onErrorResume(
            e -> {
              log.warn(
                  "[VISION] Native processing failed: {}, falling back to external APIs",
                  e.getMessage());
              return tryExternalVisionApis(base64Image, analysisType);
            });
  }

  private boolean isMockAllowed() {
    return "true".equalsIgnoreCase(configService.getSetting("allow_mock_fallback", "false"));
  }

  /**
   * Try external vision APIs as fallback. Model names must come from config (application.yml) — no
   * hardcoded fallback to an invalid model name.
   */
  private Mono<VisionAnalysisResult> tryExternalVisionApis(
      String base64Image, AnalysisType analysisType) {
    String prompt = buildPrompt(analysisType);

    String openAiModel = configService.getSetting("vision_openai_model", null);
    String geminiModel = configService.getSetting("vision_gemini_model", null);

    // Try external vision APIs — model names must be configured; skip if absent
    if (openAiKey != null && !openAiKey.isEmpty() && openAiModel != null) {
      return callOpenAiVision(base64Image, prompt, openAiModel)
          .onErrorResume(
              e -> {
                log.warn("[VISION] OpenAI vision failed: {}, trying Gemini", e.getMessage());
                if (geminiModel != null) {
                  return callGeminiVision(base64Image, prompt, geminiModel);
                }
                log.warn("[VISION] Gemini model also not configured");
                if (isMockAllowed()) return Mono.just(mockAnalysis(analysisType));
                return Mono.error(
                    new RuntimeException("Vision API execution failed: " + e.getMessage(), e));
              })
          .onErrorResume(
              e -> {
                if (isMockAllowed()) {
                  log.warn("[VISION] Gemini vision failed: {}, using mock", e.getMessage());
                  return Mono.just(mockAnalysis(analysisType));
                }
                log.error("[VISION] Vision APIs failed and mock is not allowed", e);
                return Mono.error(
                    new RuntimeException("Vision API execution failed: " + e.getMessage(), e));
              });
    } else if (geminiKey != null && !geminiKey.isEmpty() && geminiModel != null) {
      return callGeminiVision(base64Image, prompt, geminiModel)
          .onErrorResume(
              e -> {
                if (isMockAllowed()) {
                  log.warn("[VISION] Gemini vision failed: {}, using mock", e.getMessage());
                  return Mono.just(mockAnalysis(analysisType));
                }
                log.error("[VISION] Gemini vision failed and mock is not allowed", e);
                return Mono.error(
                    new RuntimeException("Vision API execution failed: " + e.getMessage(), e));
              });
    } else {
      if (isMockAllowed()) {
        log.warn("[VISION] No vision API key or model configured — returning structured mock");
        return Mono.just(mockAnalysis(analysisType));
      }
      log.error("[VISION] No vision API key/model configured and mock fallback is disabled");
      return Mono.error(
          new RuntimeException(
              "Vision API execution failed: no configured model. "
                  + "Set vision_openai_model or vision_gemini_model in application.yml."));
    }
  }

  /** Map analysis type to native vision task type. */
  private NativeVisionService.VisionTaskType mapToNativeTaskType(AnalysisType analysisType) {
    return switch (analysisType) {
      case ERROR_DEBUG -> NativeVisionService.VisionTaskType.OBJECT_DETECTION;
      case UI_PARSE -> NativeVisionService.VisionTaskType.OBJECT_DETECTION;
      case DATA_EXTRACT -> NativeVisionService.VisionTaskType.TABLE_EXTRACTION;
      case GENERAL -> NativeVisionService.VisionTaskType.IMAGE_CLASSIFICATION;
    };
  }

  /** Convert native vision result to VisionAnalysisResult. */
  private VisionAnalysisResult convertNativeResult(
      NativeVisionService.NativeVisionResult nativeResult, AnalysisType analysisType) {
    String summary = buildSummaryFromNativeResult(nativeResult, analysisType);
    return VisionAnalysisResult.success(summary, nativeResult.getProcessor());
  }

  /** Build human-readable summary from native vision result. */
  private String buildSummaryFromNativeResult(
      NativeVisionService.NativeVisionResult nativeResult, AnalysisType analysisType) {
    StringBuilder summary = new StringBuilder();

    switch (analysisType) {
      case ERROR_DEBUG:
        summary.append("Native analysis detected:\n");
        if (nativeResult.getDetectedObjects() != null) {
          nativeResult
              .getDetectedObjects()
              .forEach(
                  obj -> {
                    summary.append(
                        String.format(
                            "- %s (confidence: %.0f%%) at position (%d, %d)\n",
                            obj.getLabel(), obj.getConfidence() * 100, obj.getX(), obj.getY()));
                  });
        }
        break;

      case UI_PARSE:
        summary.append("UI components detected:\n");
        if (nativeResult.getDetectedObjects() != null) {
          nativeResult
              .getDetectedObjects()
              .forEach(
                  obj -> {
                    summary.append(String.format("- %s component\n", obj.getLabel()));
                  });
        }
        break;

      case DATA_EXTRACT:
        summary.append("Extracted data:\n").append(nativeResult.getResult());
        break;

      case GENERAL:
        summary.append("Image classified as: ").append(nativeResult.getResult());
        break;
    }

    return summary.toString();
  }

  /** Synchronous version for compatibility with existing code. */
  public String analyzeScreenshot(String base64Image) {
    return analyzeImage(base64Image, AnalysisType.ERROR_DEBUG)
        .map(VisionAnalysisResult::getSummary)
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
        .block(java.time.Duration.ofSeconds(30));
  }

  /** Convert raw image bytes to base64. */
  public String convertImageToBase64(byte[] imageBytes) {
    return Base64.getEncoder().encodeToString(imageBytes);
  }

  /** Extract text from a screenshot (OCR-like). */
  public Mono<String> extractText(String base64Image) {
    return analyzeImage(base64Image, AnalysisType.DATA_EXTRACT)
        .map(
            result ->
                result.getExtractedText() != null
                    ? result.getExtractedText()
                    : result.getSummary());
  }

  /** Parse UI mockup and return component structure description. */
  public Mono<VisionAnalysisResult> parseMockup(String base64Image) {
    return analyzeImage(base64Image, AnalysisType.UI_PARSE);
  }

  // ─── Private API callers ──────────────────────────────────────────────────

  private Mono<VisionAnalysisResult> callOpenAiVision(
      String base64Image, String prompt, String model) {
    return webClient
        .post()
        .uri("https://api.openai.com/v1/chat/completions")
        .header("Authorization", "Bearer " + openAiKey)
        .header("Content-Type", "application/json")
        .bodyValue(
            Map.of(
                "model",
                model,
                "messages",
                List.of(
                    Map.of(
                        "role",
                        "user",
                        "content",
                        List.of(
                            Map.of("type", "text", "text", prompt),
                            Map.of(
                                "type",
                                "image_url",
                                "image_url",
                                Map.of("url", "data:image/jpeg;base64," + base64Image))))),
                "max_tokens",
                1024))
        .retrieve()
        .bodyToMono(Map.class)
        .map(
            response -> {
              @SuppressWarnings("unchecked")
              List<Map<String, Object>> choices =
                  (List<Map<String, Object>>) response.get("choices");
              if (choices == null || choices.isEmpty()) {
                return VisionAnalysisResult.error("OpenAI API returned empty choices");
              }
              Map<?, ?> message = (Map<?, ?>) choices.get(0).get("message");
              if (message == null) {
                return VisionAnalysisResult.error("OpenAI API returned null message");
              }
              Object content = message.get("content");
              String text = content != null ? content.toString() : "";
              return VisionAnalysisResult.success(text, "openai-" + model);
            });
  }

  private Mono<VisionAnalysisResult> callGeminiVision(
      String base64Image, String prompt, String model) {
    String url =
        "https://generativelanguage.googleapis.com/v1beta/models/"
            + model
            + ":generateContent?key="
            + geminiKey;
    return webClient
        .post()
        .uri(url)
        .header("Content-Type", "application/json")
        .bodyValue(
            Map.of(
                "contents",
                List.of(
                    Map.of(
                        "parts",
                        List.of(
                            Map.of("text", prompt),
                            Map.of(
                                "inline_data",
                                Map.of("mime_type", "image/jpeg", "data", base64Image)))))))
        .retrieve()
        .bodyToMono(Map.class)
        .map(
            response -> {
              @SuppressWarnings("unchecked")
              List<Map<String, Object>> candidates =
                  (List<Map<String, Object>>) response.get("candidates");
              if (candidates == null || candidates.isEmpty()) {
                return VisionAnalysisResult.error("Gemini API returned empty candidates");
              }
              Map<?, ?> content = (Map<?, ?>) candidates.get(0).get("content");
              if (content == null) {
                return VisionAnalysisResult.error("Gemini API returned null content");
              }
              Object parts = content.get("parts");
              return VisionAnalysisResult.success(parts != null ? parts.toString() : "", model);
            });
  }

  private String buildPrompt(AnalysisType type) {
    return switch (type) {
      case ERROR_DEBUG ->
          "Analyze this screenshot for errors, exceptions, or UI issues. "
              + "List: 1) Errors/exceptions found, 2) UI problems visible, 3) Suggested fixes.";
      case UI_PARSE ->
          "Parse this UI mockup/screenshot. Describe: "
              + "1) Component layout structure, 2) Key UI elements (buttons, inputs, labels), "
              + "3) Suggested React/Flutter component names.";
      case DATA_EXTRACT ->
          "Extract all readable text from this image. Return as plain text, "
              + "preserving structure (tables as tab-separated, lists as line-separated).";
      case GENERAL ->
          "Describe what you see in this image in detail. "
              + "Focus on technical content relevant to software development.";
    };
  }

  private VisionAnalysisResult mockAnalysis(AnalysisType type) {
    String summary =
        switch (type) {
          case ERROR_DEBUG ->
              "Mock: Screenshot shows a NullPointerException at line 42. "
                  + "Suggested fix: Add null check before accessing the field.";
          case UI_PARSE ->
              "Mock: UI contains a header (AppBar), body with ListView (3 items), "
                  + "and a FloatingActionButton. Suggested components: AppHeader, ItemList, AddButton.";
          case DATA_EXTRACT ->
              "Mock: Extracted text — 'Dashboard | Users: 42 | Revenue: $1,234 | Status: Active'";
          case GENERAL ->
              "Mock: Image shows a mobile application interface with standard navigation pattern.";
        };
    return VisionAnalysisResult.mock(summary);
  }

  // ─── Inner Models ─────────────────────────────────────────────────────────

  public enum AnalysisType {
    ERROR_DEBUG,
    UI_PARSE,
    DATA_EXTRACT,
    GENERAL
  }

  public static class VisionAnalysisResult {
    private final boolean success;
    private final String summary;
    private final String extractedText;
    private final String provider;
    private final String errorMessage;

    private VisionAnalysisResult(
        boolean success,
        String summary,
        String extractedText,
        String provider,
        String errorMessage) {
      this.success = success;
      this.summary = summary;
      this.extractedText = extractedText;
      this.provider = provider;
      this.errorMessage = errorMessage;
    }

    public static VisionAnalysisResult success(String summary, String provider) {
      return new VisionAnalysisResult(true, summary, summary, provider, null);
    }

    public static VisionAnalysisResult mock(String summary) {
      return new VisionAnalysisResult(true, summary, null, "mock", null);
    }

    public static VisionAnalysisResult error(String message) {
      return new VisionAnalysisResult(false, null, null, null, message);
    }

    public boolean isSuccess() {
      return success;
    }

    public String getSummary() {
      return summary != null ? summary : errorMessage;
    }

    public String getExtractedText() {
      return extractedText;
    }

    public String getProvider() {
      return provider;
    }

    public String getErrorMessage() {
      return errorMessage;
    }
  }
}
