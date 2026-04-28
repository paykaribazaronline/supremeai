package com.supremeai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Autonomous Questioning Engine (S3)
 * Validates user input and asks clarifying questions if information is incomplete
 * Ensures AI doesn't start work with half or wrong information
 */
@Service
public class AutonomousQuestioningEngine {

    private static final Logger logger = LoggerFactory.getLogger(AutonomousQuestioningEngine.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Minimum thresholds for different request types
    private static final int MIN_PROMPT_LENGTH = 10;
    private static final int MIN_CODE_REQUIREMENT_LENGTH = 20;
    private static final double MIN_CLARITY_SCORE = 0.6;

    @Autowired
    private com.supremeai.provider.AIProviderFactory providerFactory;

    /**
     * Validate user input and generate clarifying questions if needed
     */
    public ValidationResult validateAndQuestion(String userInput, RequestType requestType) {
        logger.info("Validating user input of type: {}", requestType);

        List<String> questions = new ArrayList<>();
        double clarityScore = calculateClarityScore(userInput, requestType);
        boolean isComplete = clarityScore >= MIN_CLARITY_SCORE;

        // Check for missing critical information
        questions.addAll(checkMissingInformation(userInput, requestType));

        // Check for ambiguity
        questions.addAll(checkAmbiguity(userInput));

        // Check for conflicting information
        questions.addAll(checkConflicts(userInput));
        
        // S3 Enhancement: Check for missing context or "low-effort" prompts
        questions.addAll(checkContextualCompleteness(userInput, requestType));

        ValidationResult result = new ValidationResult();
        result.setOriginalInput(userInput);
        result.setRequestType(requestType);
        result.setClarityScore(clarityScore);
        result.setComplete(isComplete && questions.isEmpty());
        result.setClarifyingQuestions(questions);

        logger.info("Validation complete. Clarity: {}, Questions: {}", clarityScore, questions.size());
        return result;
    }

    /**
     * Calculate clarity score based on input completeness
     */
    private double calculateClarityScore(String input, RequestType type) {
        double score = 0.0;
        String lowerInput = input.toLowerCase();

        // Length check
        if (input.length() >= getMinLengthForType(type)) {
            score += 0.3;
        }

        // Has specific keywords for the request type
        score += checkKeywordPresence(lowerInput, type) * 0.3;

        // Has clear objective
        if (hasClearObjective(lowerInput)) {
            score += 0.2;
        }

        // Has constraints or requirements specified
        if (hasConstraints(lowerInput)) {
            score += 0.2;
        }

        return Math.min(score, 1.0);
    }

    /**
     * Check for missing critical information based on request type
     */
    private List<String> checkMissingInformation(String input, RequestType type) {
        List<String> questions = new ArrayList<>();
        String lowerInput = input.toLowerCase();

        switch (type) {
            case CODE_GENERATION:
                if (!hasProgrammingLanguage(lowerInput)) {
                    questions.add("Which programming language do you want the code in? (e.g., Python, Java, JavaScript)");
                }
                if (!hasOutputSpecification(lowerInput)) {
                    questions.add("What should the code output or accomplish? Please specify the expected behavior.");
                }
                if (input.length() < MIN_CODE_REQUIREMENT_LENGTH) {
                    questions.add("Could you provide more details about the requirements? The current description seems incomplete.");
                }
                break;

            case API_DESIGN:
                if (!mentionsEndpoints(lowerInput)) {
                    questions.add("What endpoints do you need? Please specify the API endpoints required.");
                }
                if (!mentionsHttpMethod(lowerInput)) {
                    questions.add("What HTTP methods should be used? (GET, POST, PUT, DELETE, etc.)");
                }
                break;

            case DATABASE_SCHEMA:
                if (!mentionsEntities(lowerInput)) {
                    questions.add("What entities/tables do you need? Please list the main data entities.");
                }
                if (!mentionsRelationships(lowerInput)) {
                    questions.add("Do you need any relationships between entities? (one-to-many, many-to-many, etc.)");
                }
                break;

            case BUG_FIX:
                if (!hasErrorCode(lowerInput) && !hasErrorMessage(lowerInput)) {
                    questions.add("What is the exact error message or error code you're encountering?");
                }
                if (!hasCodeSnippet(lowerInput)) {
                    questions.add("Could you provide the code snippet that's causing the issue?");
                }
                break;

            case GENERAL_AI:
                if (input.length() < MIN_PROMPT_LENGTH) {
                    questions.add("Could you elaborate on your request? The input seems too brief.");
                }
                break;
        }

        return questions;
    }

    /**
     * Check for ambiguous terms or phrases
     */
    private List<String> checkAmbiguity(String input) {
        List<String> questions = new ArrayList<>();
        String lowerInput = input.toLowerCase();

        // Check for ambiguous pronouns
        if (lowerInput.matches(".*\\b(it|this|that|they|them)\\b.*")) {
            questions.add("Could you clarify what you mean by the pronouns used (it/this/that/they)?");
        }

        // Check for vague terms
        List<String> vagueTerms = Arrays.asList("something", "anything", "stuff", "things", "somehow", "maybe");
        for (String term : vagueTerms) {
            if (lowerInput.contains(term)) {
                questions.add("Could you be more specific instead of using vague terms like '" + term + "'?");
                break;
            }
        }

        return questions;
    }

    /**
     * Check for conflicting information in the input
     */
    private List<String> checkConflicts(String input) {
        List<String> questions = new ArrayList<>();
        String lowerInput = input.toLowerCase();

        // Check for conflicting requirements
        if (lowerInput.contains("fast") && lowerInput.contains("detailed")) {
            questions.add("You mentioned both 'fast' and 'detailed' - which is more important for this task?");
        }

        if (lowerInput.contains("simple") && (lowerInput.contains("complex") || lowerInput.contains("advanced"))) {
            questions.add("You mentioned both 'simple' and 'complex/advanced' - could you clarify the complexity level needed?");
        }

        return questions;
    }

    /**
     * Check for missing context or low-effort prompts
     */
    private List<String> checkContextualCompleteness(String input, RequestType type) {
        List<String> questions = new ArrayList<>();
        String lowerInput = input.toLowerCase();
        
        // Check for "empty" action verbs without subjects
        if (lowerInput.matches("^(build|create|make|generate|implement|fix)(\\s+a|\\s+an|\\s+the)?\\s*$")) {
            questions.add("What exactly do you want me to " + lowerInput.split("\\s+")[0] + "? Please provide a subject.");
        }
        
        // Check for lack of tech stack in creation tasks
        if ((type == RequestType.CODE_GENERATION || type == RequestType.API_DESIGN) && 
            !lowerInput.contains("stack") && !hasProgrammingLanguage(lowerInput)) {
            questions.add("Do you have a preferred technology stack or framework (e.g., React, Spring Boot, Node.js)?");
        }
        
        // Check for lack of "why" or "goal"
        if (input.split("\\s+").length < 5) {
            questions.add("Could you describe the end goal or the use case for this request?");
        }
        
        return questions;
    }

    private int getMinLengthForType(RequestType type) {
        switch (type) {
            case CODE_GENERATION: return MIN_CODE_REQUIREMENT_LENGTH;
            default: return MIN_PROMPT_LENGTH;
        }
    }

    private double checkKeywordPresence(String input, RequestType type) {
        int matches = 0;
        int total = 0;

        switch (type) {
            case CODE_GENERATION:
                total = 3;
                if (input.contains("function") || input.contains("method") || input.contains("class")) matches++;
                if (input.contains("return") || input.contains("output") || input.contains("result")) matches++;
                if (input.contains("parameter") || input.contains("argument") || input.contains("input")) matches++;
                break;
            case API_DESIGN:
                total = 2;
                if (input.contains("endpoint") || input.contains("route") || input.contains("url")) matches++;
                if (input.contains("get") || input.contains("post") || input.contains("put") || input.contains("delete")) matches++;
                break;
            default:
                return 1.0;
        }

        return total > 0 ? (double) matches / total : 1.0;
    }

    private boolean hasClearObjective(String input) {
        String[] objectiveMarkers = {"create", "build", "generate", "make", "implement", "design", "fix", "solve"};
        for (String marker : objectiveMarkers) {
            if (input.contains(marker)) return true;
        }
        return false;
    }

    private boolean hasConstraints(String input) {
        String[] constraintMarkers = {"should", "must", "need to", "require", "constraint", "limit", "only"};
        for (String marker : constraintMarkers) {
            if (input.contains(marker)) return true;
        }
        return false;
    }

    private boolean hasProgrammingLanguage(String input) {
        String[] languages = {"python", "java", "javascript", "js", "typescript", "ts", "c++", "c#", "go", "rust", "php", "ruby"};
        for (String lang : languages) {
            // Use word boundaries for alphabetical names to prevent "go" from matching "good"
            if (lang.contains("+") || lang.contains("#")) {
                if (input.contains(lang)) return true;
            } else {
                if (input.matches(".*\\b" + lang + "\\b.*")) return true;
            }
        }
        return false;
    }

    private boolean hasOutputSpecification(String input) {
        String[] outputMarkers = {"return", "output", "print", "display", "show", "result"};
        for (String marker : outputMarkers) {
            if (input.contains(marker)) return true;
        }
        return false;
    }

    private boolean mentionsEndpoints(String input) {
        return input.contains("endpoint") || input.contains("route") || input.contains("/api");
    }

    private boolean mentionsHttpMethod(String input) {
        String[] methods = {"get", "post", "put", "delete", "patch", "head", "options"};
        for (String method : methods) {
            if (input.contains(method)) return true;
        }
        return false;
    }

    private boolean mentionsEntities(String input) {
        String[] entityMarkers = {"table", "entity", "model", "class", "collection"};
        for (String marker : entityMarkers) {
            if (input.contains(marker)) return true;
        }
        return false;
    }

    private boolean mentionsRelationships(String input) {
        String[] relationMarkers = {"relationship", "foreign key", "reference", "one-to-many", "many-to-many", "belongs to"};
        for (String marker : relationMarkers) {
            if (input.contains(marker)) return true;
        }
        return false;
    }

    private boolean hasErrorCode(String input) {
        return Pattern.compile("error\\s*#?\\d+|code\\s*#?\\d+|exception|null pointer|stack trace").matcher(input).find();
    }

    private boolean hasErrorMessage(String input) {
        return input.contains("error") || input.contains("exception") || input.contains("failed") || input.contains("crash");
    }

    private boolean hasCodeSnippet(String input) {
        return input.contains("{") || input.contains("function") || input.contains("def ") || input.contains("class ");
    }

    /**
     * Validation result container
     */
    public static class ValidationResult {
        private String originalInput;
        private RequestType requestType;
        private double clarityScore;
        private boolean isComplete;
        private List<String> clarifyingQuestions;

        public String getOriginalInput() { return originalInput; }
        public void setOriginalInput(String originalInput) { this.originalInput = originalInput; }

        public RequestType getRequestType() { return requestType; }
        public void setRequestType(RequestType requestType) { this.requestType = requestType; }

        public double getClarityScore() { return clarityScore; }
        public void setClarityScore(double clarityScore) { this.clarityScore = clarityScore; }

        public boolean isComplete() { return isComplete; }
        public void setComplete(boolean complete) { isComplete = complete; }

        public List<String> getClarifyingQuestions() { return clarifyingQuestions; }
        public void setClarifyingQuestions(List<String> clarifyingQuestions) { this.clarifyingQuestions = clarifyingQuestions; }

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
