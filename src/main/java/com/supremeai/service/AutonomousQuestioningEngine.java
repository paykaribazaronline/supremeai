package com.supremeai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Autonomous Questioning Engine (S3)
 * 
 * INTELLIGENT INTENT CLASSIFICATION SYSTEM
 * 
 * This engine determines the appropriate response strategy for any user query.
 * Instead of hardcoded validation rules, it uses:
 * - Intent classification (informational vs task vs clarification needed)
 * - Context-aware analysis
 * - Confidence scoring
 * 
 * The system decides WHAT to do, WHEN to do it, and HOW to do it.
 */
@Service
public class AutonomousQuestioningEngine {

    private static final Logger logger = LoggerFactory.getLogger(AutonomousQuestioningEngine.class);

    // Minimum thresholds for different request types
    @Autowired
    private com.supremeai.provider.AIProviderFactory providerFactory;

    @Autowired
    private ConfigService configService;
    
    /**
     * INTENT TYPES for Response Strategy
     * - FACTUAL: "what is X", "explain Y" - Direct answer needed
     * - TASK: "create", "build", "fix" - Action required
     * - CLARIFY: Ambiguous queries - Need user input
     * - GREETING: "hi", "hello" - Friendly acknowledgment
     * - CREATIVE: "song", "poem", "story" - Unique requests, shouldn't be memorized
     * - TEMPORAL: Time-sensitive data, news, prices - Will search web but NEVER memorize
     */
    public enum IntentType {
        FACTUAL, TASK, CLARIFY, GREETING, CREATIVE, TEMPORAL, UNKNOWN
    }
    
    /**
     * Response Strategy determined by the engine
     */
    public enum ResponseStrategy {
        DIRECT_ANSWER,       // Can answer directly from knowledge
        CLARIFY_FIRST,       // Need clarification before answering
        MULTI_TURN_PLAN,     // Complex task requiring step-by-step
        WEB_SEARCH_NEEDED,   // Need to search the web for current info
        LEARN_AND_RESPOND    // New concept - learn then respond
    }

    public IntentType classifyIntent(String input) {
        if (input == null || input.trim().isEmpty()) {
            return IntentType.UNKNOWN;
        }
        
        String lower = input.trim().toLowerCase();
        
        // Greeting detection
        if (isGreeting(input) || isCommonPhrase(input)) {
            return IntentType.GREETING;
        }
        
        // Creative patterns
        if (lower.matches(".*\\b(song|poem|story|joke|creative|write me a|গান|কবিতা|গল্প|জোকস|ছড়া|প্রবন্ধ)\\b.*")) {
            return IntentType.CREATIVE;
        }
        
        // Temporal / Time-sensitive patterns
        if (lower.matches(".*\\b(latest|current|today|news|weather|price|now|সাম্প্রতিক|আজকের|খবর|বর্তমান|এখনকার)\\b.*")) {
            return IntentType.TEMPORAL;
        }
        
        // Factual question patterns
        if (lower.matches(".*\\b(what is|what are|explain|tell me about|describe|define)\\b.*") ||
            lower.matches(".*\\b(how does|how work|difference between)\\b.*")) {
            return IntentType.FACTUAL;
        }
        
        // Task-oriented patterns
        if (lower.matches(".*\\b(create|build|make|develop|implement|write|code|fix|debug|deploy|design)\\b.*")) {
            return IntentType.TASK;
        }
        
        // Default to factual for questions ending with '?'
        if (lower.endsWith("?")) {
            return IntentType.FACTUAL;
        }
        
        return IntentType.CLARIFY;
    }

    private boolean isGreeting(String input) {
        if (input == null) return false;
        String trimmed = input.trim().toLowerCase();
        return trimmed.matches("^(hi|hello|hey|hlw|greetings|hola|good morning|good afternoon|good evening|assalamualaikum|namaste|hello there|hi there|yo|hello neural|hi neural|হাই|হ্যালো|হেই|কি অবস্থা|কেমন আছেন)(\\s+.*)?$");
    }

    private boolean isCommonPhrase(String input) {
        if (input == null) return false;
        String trimmed = input.trim().toLowerCase();
        return trimmed.matches("^(thanks|thank you|ok|okay|yes|no|cool|awesome|great|perfect|bye|goodbye)(\\s+.*)?$");
    }

    public List<String> generateProbableOptions(String input, RequestType type) {
        List<String> options = new ArrayList<>();
        String lower = input != null ? input.toLowerCase().trim() : "";

        if (lower.contains("flutter") || lower.contains("mobile")) {
            options.add("How to create a Flutter app");
            options.add("Flutter setup & installation");
            options.add("Integrate Firebase with Flutter");
        } else if (lower.contains("create") || lower.contains("build") || lower.contains("app")) {
            options.add("Build a Spring Boot backend app");
            options.add("Build a React frontend web app");
            options.add("Build a Flutter mobile app");
        } else if (lower.contains("database") || lower.contains("schema") || lower.contains("sql")) {
            options.add("Design a PostgreSQL schema");
            options.add("Design a MongoDB / Firestore schema");
            options.add("Database connection setup");
        } else if (lower.contains("error") || lower.contains("bug") || lower.contains("fix")) {
            options.add("Fix a NullPointerException");
            options.add("Fix a CORS policy blocked error");
            options.add("Debug Spring Security 403 Forbidden");
        } else if (lower.contains("api") || lower.contains("rest")) {
            options.add("Design a REST API with GET & POST endpoints");
            options.add("Spring Security JWT authentication API");
            options.add("Implement pagination in REST API");
        } else {
            options.add("Explain the concept in detail");
            options.add("Provide a practical code example");
            options.add("Show step-by-step implementation guide");
        }
        options.add("অন্য কিছু (বিস্তারিত লিখুন)");
        return options;
    }

    /**
     * Validate user input and generate clarifying questions if needed
     * 
     * NEW APPROACH: Instead of blocking queries, determine response strategy
     */
    public Mono<ValidationResult> validateAndQuestion(String userInput, RequestType requestType) {
        return Mono.fromCallable(() -> {
            logger.info("Validating user input of type: {}", requestType);

            List<String> questions = new ArrayList<>();
            IntentType intent = classifyIntent(userInput);
            ResponseStrategy strategy = determineStrategy(userInput, intent, requestType);
            double clarityScore = calculateClarityScore(userInput, requestType);
            boolean isComplete;

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

            logger.info("Validation complete. Intent: {}, Strategy: {}, Clarity: {}, Questions: {}", 
                    intent, strategy, clarityScore, questions.size());
            return result;
        }).subscribeOn(Schedulers.boundedElastic());
    }
    
    /**
     * Determine the appropriate response strategy based on intent and context
     */
    private ResponseStrategy determineStrategy(String input, IntentType intent, RequestType requestType) {
        if (intent == IntentType.GREETING) {
            return ResponseStrategy.DIRECT_ANSWER;
        }
        
        if (intent == IntentType.UNKNOWN) {
            return ResponseStrategy.CLARIFY_FIRST;
        }
        
        // Local-first mode check: prefer local responses when enabled
        boolean localFirst = Boolean.parseBoolean(
                configService.getEffectiveSetting("supremeai.local-first.enabled", "false")
        );
        
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
        boolean forceWebSearch = Boolean.parseBoolean(
                configService.getEffectiveSetting("force_web_search", "false")
        );
        
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

    private double calculateClarityScore(String userInput, RequestType requestType) {
        if (userInput == null || userInput.trim().isEmpty()) return 0.0;
        int wordCount = userInput.trim().split("\\s+").length;
        if (wordCount >= 5) return 1.0;
        if (wordCount >= 3) return 0.8;
        return 0.5;
    }

    private double getMinClarityScore() {
        return 0.7;
    }

    private List<String> checkMissingInformation(String userInput, RequestType requestType) {
        List<String> questions = new ArrayList<>();
        if (userInput == null) return questions;
        String lower = userInput.toLowerCase();
        
        if (requestType == RequestType.CODE_GENERATION) {
            if (!lower.contains("language") && !lower.contains("java") && !lower.contains("python") && !lower.contains("js")) {
                questions.add("Which programming language should be used?");
            }
        } else if (requestType == RequestType.DATABASE_SCHEMA) {
            if (!lower.contains("sql") && !lower.contains("nosql") && !lower.contains("mongo") && !lower.contains("postgres")) {
                questions.add("What type of database are you targeting (e.g., PostgreSQL, MongoDB)?");
            }
        }
        return questions;
    }



    /**
     * Validation result container
     */
    public static class ValidationResult {
        private String originalInput;
        private RequestType requestType;
        private IntentType intentType;
        private ResponseStrategy responseStrategy;
        private double clarityScore;
        private boolean isComplete;
        private List<String> clarifyingQuestions;
        private List<String> options;

        public String getOriginalInput() { return originalInput; }
        public void setOriginalInput(String originalInput) { this.originalInput = originalInput; }

        public RequestType getRequestType() { return requestType; }
        public void setRequestType(RequestType requestType) { this.requestType = requestType; }

        public IntentType getIntentType() { return intentType; }
        public void setIntentType(IntentType intentType) { this.intentType = intentType; }

        public ResponseStrategy getResponseStrategy() { return responseStrategy; }
        public void setResponseStrategy(ResponseStrategy strategy) { this.responseStrategy = strategy; }

        public double getClarityScore() { return clarityScore; }
        public void setClarityScore(double clarityScore) { this.clarityScore = clarityScore; }

        public boolean isComplete() { return isComplete; }
        public void setComplete(boolean complete) { isComplete = complete; }

        public List<String> getClarifyingQuestions() { return clarifyingQuestions; }
        public void setClarifyingQuestions(List<String> clarifyingQuestions) { this.clarifyingQuestions = clarifyingQuestions; }

        public List<String> getOptions() { return options; }
        public void setOptions(List<String> options) { this.options = options; }

        public boolean hasQuestions() { return clarifyingQuestions != null && !clarifyingQuestions.isEmpty(); }
    }

    /**
     * Request types for different AI tasks
     */
    public enum RequestType {
        CODE_GENERATION,
        API_DESIGN,
        DATABASE_SCHEMA,
        BUG_FIX,
        GENERAL_AI
    }
}
