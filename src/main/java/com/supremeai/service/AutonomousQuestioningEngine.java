package com.supremeai.service;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class AutonomousQuestioningEngine {

  private static final Logger logger = LoggerFactory.getLogger(AutonomousQuestioningEngine.class);

  @Autowired
  private com.supremeai.provider.AIProviderFactory providerFactory;

  @Autowired
  private ConfigService configService;

  @Autowired
  private com.supremeai.service.SupremeAIBrain supremeAIBrain;

  @Autowired
  private DynamicSignatureRegistry signatureRegistry;

  public enum IntentType {
    FACTUAL,
    TASK,
    CLARIFY,
    GREETING,
    CREATIVE,
    TEMPORAL,
    UNKNOWN
  }

  public enum ResponseStrategy {
    DIRECT_ANSWER,
    CLARIFY_FIRST,
    MULTI_TURN_PLAN,
    WEB_SEARCH_NEEDED,
    LEARN_AND_RESPOND
  }

/**
    * NEURAL ROUTING: Instead of hardcoded Regex, we ask the Supreme Brain to
    * classify intent.
    */
  public Mono<IntentType> classifyIntentAI(String input) {
    String prompt =
        signatureRegistry.getSignatures("INTENT_CLASSIFICATION_PROMPT")
            .stream()
            .findFirst()
            .orElse("Classify the intent for: \"%s\"")
            .formatted(input);

    return supremeAIBrain.think("ORCHESTRATION", prompt)
        .map(response -> {
          try {
            return IntentType.valueOf(response.trim().toUpperCase());
          } catch (Exception e) {
            logger.warn("AI Classification failed for: {}, using contextual detection.", input);
            return detectContextualIntent(input);
          }
        })
        .defaultIfEmpty(IntentType.CLARIFY);
  }

  /**
   * Alias for classifyIntentAI for backward compatibility.
   */
  public IntentType classifyIntent(String input) {
    return classifyIntentAI(input).block();
  }

  private IntentType detectContextualIntent(String input) {
    if (input == null || input.trim().isEmpty()) {
      return IntentType.CLARIFY;
    }

    String lower = input.toLowerCase();
    if (signatureRegistry.matchesAny(lower, "GREETING_PATTERNS")) {
      return IntentType.GREETING;
    }
    if (signatureRegistry.matchesAny(lower, "TEMPORAL_PATTERNS")) {
      return IntentType.TEMPORAL;
    }
    if (signatureRegistry.matchesAny(lower, "CREATIVE_PATTERNS")) {
      return IntentType.CREATIVE;
    }
    if (signatureRegistry.matchesAny(lower, "TASK_PATTERNS") || lower.contains("?")) {
      return IntentType.FACTUAL;
    }
    if (signatureRegistry.matchesAny(lower, "REQUEST_PATTERNS")) {
      return IntentType.TASK;
    }
    return IntentType.UNKNOWN;
  }

  public List<String> generateProbableOptions(String input, RequestType type) {
    List<String> options = new ArrayList<>();
    String lower = input != null ? input.toLowerCase().trim() : "";

    if (signatureRegistry.matchesAny(lower, "MOBILE_TOOLS")) {
      Set<String> mobileOptions = signatureRegistry.getSignatures("MOBILE_OPTION_TEMPLATES");
      options.addAll(mobileOptions.stream().limit(3).toList());
    } else if (signatureRegistry.matchesAny(lower, "BUILD_TOOLS")) {
      Set<String> buildOptions = signatureRegistry.getSignatures("BUILD_OPTION_TEMPLATES");
      options.addAll(buildOptions.stream().limit(3).toList());
    } else if (signatureRegistry.matchesAny(lower, "DATABASE_TOOLS")) {
      Set<String> dbOptions = signatureRegistry.getSignatures("DATABASE_OPTION_TEMPLATES");
      options.addAll(dbOptions.stream().limit(3).toList());
    } else if (signatureRegistry.matchesAny(lower, "DEBUG_TOOLS")) {
      Set<String> debugOptions = signatureRegistry.getSignatures("DEBUG_OPTION_TEMPLATES");
      options.addAll(debugOptions.stream().limit(3).toList());
    } else if (signatureRegistry.matchesAny(lower, "API_TOOLS")) {
      Set<String> apiOptions = signatureRegistry.getSignatures("API_OPTION_TEMPLATES");
      options.addAll(apiOptions.stream().limit(3).toList());
    } else {
      Set<String> generalOptions = signatureRegistry.getSignatures("GENERAL_OPTION_TEMPLATES");
      options.addAll(generalOptions.stream().limit(3).toList());
    }
    options.add("Something else (please specify)");
    return options;
  }

  /**
   * Validate user input and generate clarifying questions if needed
   *
   * <p>
   * NEW APPROACH: Instead of blocking queries, determine response strategy
   */
  public Mono<ValidationResult> validateAndQuestion(String userInput, RequestType requestType) {
    return classifyIntentAI(userInput)
        .flatMap(intent -> Mono.fromCallable(() -> {
          logger.info("Validating user input of type: {}", requestType);

          List<String> questions = new ArrayList<>();
          ResponseStrategy strategy = determineStrategy(userInput, intent, requestType);
          double clarityScore = calculateClarityScore(userInput, requestType);
          boolean isComplete;

          // Intent is now pre-classified via AI

          // INTELLIGENT FLOW: Don't block valid queries, determine response approach
          if (intent == IntentType.GREETING) {
            isComplete = true;
            clarityScore = 1.0;
          } else if (intent == IntentType.FACTUAL) {
            // Factual questions should always proceed - let the knowledge system answer
            isComplete = true;
            clarityScore = 0.85;
          } else if (requestType == RequestType.GENERAL_AI) {
            // For general AI, accept inputs >= 3 chars and let the system handle it
            if (userInput != null && userInput.trim().length() >= 3) {
              isComplete = true;
              clarityScore = Math.max(0.7, clarityScore);
            } else {
              isComplete = false;
              questions.add("Could you elaborate on your request? The input seems too brief.");
            }
          } else {
            // For specific task types (CODE_GENERATION, API_DESIGN, etc.)
            // Check for missing critical information
            questions.addAll(checkMissingInformation(userInput, requestType));

            isComplete = (clarityScore >= getMinClarityScore()) && questions.isEmpty();
          }

          ValidationResult result = new ValidationResult();
          result.setOriginalInput(userInput);
          result.setRequestType(requestType);
          result.setClarityScore(clarityScore);
          result.setComplete(isComplete);
          result.setClarifyingQuestions(questions);
          result.setIntentType(intent);
          result.setResponseStrategy(strategy);
          if (!isComplete) {
            result.setOptions(generateProbableOptions(userInput, requestType));
          } else {
            result.setOptions(Collections.emptyList());
          }

          logger.info(
              "Validation complete. Intent: {}, Strategy: {}, Clarity: {}, Questions: {}",
              intent,
              strategy,
              clarityScore,
              questions.size());
          return result;
        }))
        .subscribeOn(Schedulers.boundedElastic());
  }

  /** Determine the appropriate response strategy based on intent and context */
  private ResponseStrategy determineStrategy(
      String input, IntentType intent, RequestType requestType) {
    if (intent == IntentType.GREETING) {
      return ResponseStrategy.DIRECT_ANSWER;
    }

    if (intent == IntentType.UNKNOWN) {
      return ResponseStrategy.CLARIFY_FIRST;
    }

    // Local-first mode check: prefer local responses when enabled
    boolean localFirst = Boolean.parseBoolean(
        configService.getEffectiveSetting("supremeai.local-first.enabled", "false"));

    if (localFirst) {
      // In local-first mode, route to direct answer for most intents
      return switch (intent) {
        case FACTUAL, TASK, CREATIVE -> ResponseStrategy.DIRECT_ANSWER;
        case CLARIFY -> ResponseStrategy.CLARIFY_FIRST;
        case TEMPORAL -> ResponseStrategy.WEB_SEARCH_NEEDED;
        default -> ResponseStrategy.DIRECT_ANSWER;
      };
    }

    // Feature flag checking instead of hardcoded 180-degree shift
    boolean forceWebSearch = Boolean.parseBoolean(configService.getEffectiveSetting("force_web_search", "false"));

    if (forceWebSearch) {
      return ResponseStrategy.WEB_SEARCH_NEEDED;
    }

    // Standard logic when web search isn't explicitly forced
    return switch (intent) {
      case FACTUAL -> ResponseStrategy.DIRECT_ANSWER;
      case TASK -> ResponseStrategy.MULTI_TURN_PLAN;
      case CLARIFY -> ResponseStrategy.CLARIFY_FIRST;
      case TEMPORAL -> ResponseStrategy.WEB_SEARCH_NEEDED;
      case CREATIVE -> ResponseStrategy.DIRECT_ANSWER;
      default -> ResponseStrategy.CLARIFY_FIRST;
    };
  }

  /**
   * NEURAL CLARITY: Instead of word count, ask the brain if it has enough info.
   */
  public Mono<Double> calculateClarityScoreAI(String userInput, RequestType requestType) {
    String prompt = "Rate the clarity of this request for " + requestType + " from 0.0 to 1.0.\n"
        + "Input: \"" + userInput + "\"\n"
        + "Respond with only the numeric score.";

    return supremeAIBrain.think("VALIDATION", prompt)
        .map(score -> {
          try {
            return Double.parseDouble(score.trim());
          } catch (Exception e) {
            return 0.5;
          }
        })
        .defaultIfEmpty(0.5);
  }

  private double calculateClarityScore(String userInput, RequestType requestType) {
    if (userInput == null || userInput.isBlank()) {
      return 0.0;
    }
    String trimmed = userInput.trim();
    double score = 0.35;
    if (trimmed.length() >= 20) {
      score += 0.2;
    }
    if (trimmed.length() >= 80) {
      score += 0.15;
    }
    if (trimmed.contains("?") || trimmed.split("\\s+").length >= 5) {
      score += 0.15;
    }
    if (requestType == RequestType.GENERAL_AI) {
      score += 0.1;
    }
    return Math.min(1.0, score);
  }

  private double getMinClarityScore() {
    return 0.7;
  }

  private List<String> checkMissingInformation(String userInput, RequestType requestType) {
    List<String> questions = new ArrayList<>();
    if (userInput == null)
      return questions;

    Set<String> codeLangIndicators = signatureRegistry.getSignatures("CODE_LANGUAGE_INDICATORS");
    Set<String> dbIndicators = signatureRegistry.getSignatures("DATABASE_INDICATORS");

    if (requestType == RequestType.CODE_GENERATION) {
      boolean hasLanguage = codeLangIndicators.stream()
          .anyMatch(ind -> userInput.toLowerCase().contains(ind.toLowerCase()));
      if (!hasLanguage) {
        questions.add(getDynamicQuestion("CODE_LANGUAGE_QUESTION"));
      }
    } else if (requestType == RequestType.DATABASE_SCHEMA) {
      boolean hasDb = dbIndicators.stream()
          .anyMatch(ind -> userInput.toLowerCase().contains(ind.toLowerCase()));
      if (!hasDb) {
        questions.add(getDynamicQuestion("DATABASE_TYPE_QUESTION"));
      }
    }
    return questions;
  }

  private String getDynamicQuestion(String key) {
    Set<String> questions = signatureRegistry.getSignatures("MISSING_INFO_QUESTIONS");
    return questions.stream()
        .filter(q -> q.startsWith(key + ":"))
        .map(q -> q.substring(key.length() + 1))
        .findFirst()
        .orElse("Please provide more details about your request.");
  }

  /** Validation result container */
  public static class ValidationResult {
    private String originalInput;
    private RequestType requestType;
    private IntentType intentType;
    private ResponseStrategy responseStrategy;
    private double clarityScore;
    private boolean isComplete;
    private List<String> clarifyingQuestions;
    private List<String> options;

    public String getOriginalInput() {
      return originalInput;
    }

    public void setOriginalInput(String originalInput) {
      this.originalInput = originalInput;
    }

    public RequestType getRequestType() {
      return requestType;
    }

    public void setRequestType(RequestType requestType) {
      this.requestType = requestType;
    }

    public IntentType getIntentType() {
      return intentType;
    }

    public void setIntentType(IntentType intentType) {
      this.intentType = intentType;
    }

    public ResponseStrategy getResponseStrategy() {
      return responseStrategy;
    }

    public void setResponseStrategy(ResponseStrategy strategy) {
      this.responseStrategy = strategy;
    }

    public double getClarityScore() {
      return clarityScore;
    }

    public void setClarityScore(double clarityScore) {
      this.clarityScore = clarityScore;
    }

    public boolean isComplete() {
      return isComplete;
    }

    public void setComplete(boolean complete) {
      isComplete = complete;
    }

    public List<String> getClarifyingQuestions() {
      return clarifyingQuestions;
    }

    public void setClarifyingQuestions(List<String> clarifyingQuestions) {
      this.clarifyingQuestions = clarifyingQuestions;
    }

    public List<String> getOptions() {
      return options;
    }

    public void setOptions(List<String> options) {
      this.options = options;
    }

    public boolean hasQuestions() {
      return clarifyingQuestions != null && !clarifyingQuestions.isEmpty();
    }
  }

  /** Request types for different AI tasks */
  public enum RequestType {
    CODE_GENERATION,
    API_DESIGN,
    DATABASE_SCHEMA,
    BUG_FIX,
    GENERAL_AI
  }
}
