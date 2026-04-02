package org.example.service;

import org.example.model.ConsensusVote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * AIErrorSolvingService
 *
 * Uses SystemLearning memory + Multi-AI consensus to solve errors.
 * Workflow:
 * 1. Parse error type
 * 2. Check known memory solutions
 * 3. If no strong memory solution, ask Multi-AI consensus
 * 4. Return actionable fix plan
 * 5. Learn from verified outcomes
 */
@Service
public class AIErrorSolvingService {

    private static final Logger logger = LoggerFactory.getLogger(AIErrorSolvingService.class);

    @Autowired
    private SystemLearningService learningService;

    @Autowired
    private MultiAIConsensusService consensusService;

    @Autowired
    private GitHubActionsErrorParser errorParser;

    /**
     * Solve raw error text (build logs, stack trace, runtime error, etc.)
     */
    public Map<String, Object> solveError(String userId, String rawErrorText, String context) {
        Map<String, Object> response = new HashMap<>();

        if (rawErrorText == null || rawErrorText.trim().isEmpty()) {
            response.put("status", "error");
            response.put("message", "rawErrorText is required");
            return response;
        }

        try {
            Map<String, Object> parsed = errorParser.parseJobOutput(rawErrorText);
            String errorType = String.valueOf(parsed.getOrDefault("errorType", "BUILD_FAILURE"));
            String category = mapErrorTypeToCategory(errorType);

            List<String> knownSolutions = learningService.getSolutionsFor(category);
            String memoryFix = pickBestMemoryFix(knownSolutions);

            // Build a robust question for consensus if memory doesn't have enough context.
            String question = buildConsensusQuestion(errorType, rawErrorText, context, memoryFix);
            ConsensusVote vote = consensusService.askAllAI(userId, question);

            String aiFix = vote != null ? vote.getWinningResponse() : "No consensus result available.";
            double confidence = vote != null && vote.getConfidenceScore() != null ? vote.getConfidenceScore() : 0.0;

            response.put("status", "success");
            response.put("errorType", errorType);
            response.put("category", category);
            response.put("memorySolutionAvailable", memoryFix != null);
            response.put("memorySolution", memoryFix);
            response.put("aiSolution", aiFix);
            response.put("confidenceScore", confidence);
            response.put("parsedError", parsed);
            response.put("timestamp", System.currentTimeMillis());

            learningService.recordPattern(
                "ERROR_SOLVING",
                "Error solved flow for category: " + category,
                "Raw error analyzed, memory consulted, AI consensus applied. Error type=" + errorType
            );

            logger.info("✅ Error solving completed. type={}, confidence={}", errorType, confidence);
        } catch (Exception e) {
            logger.error("❌ Failed to solve error: {}", e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "Failed to solve error: " + e.getMessage());
        }

        return response;
    }

    /**
     * Record whether a proposed fix worked, so SupremeAI learns for next time.
     */
    public void recordFixOutcome(String category, String errorSnippet, String fixApplied, boolean success) {
        String outcome = success ? "SUCCESS" : "FAILURE";
        String requirement = "Fix outcome for " + category + " should be remembered";
        String details = "Error=" + safe(errorSnippet) + " | Fix=" + safe(fixApplied) + " | Outcome=" + outcome;

        if (success) {
            learningService.recordPattern(category, "Verified fix pattern", details);
        } else {
            learningService.recordError(category, errorSnippet, null, fixApplied);
        }

        learningService.recordRequirement(requirement, details);
    }

    private String mapErrorTypeToCategory(String errorType) {
        switch (errorType) {
            case "COMPILATION_ERROR":
                return "BUILD_PATTERNS";
            case "TEST_FAILURE":
                return "TEST_PATTERNS";
            case "RUNTIME_ERROR":
                return "ERROR_SOLVING";
            case "BUILD_FAILURE":
            default:
                return "ERROR_SOLVING";
        }
    }

    private String pickBestMemoryFix(List<String> knownSolutions) {
        if (knownSolutions == null || knownSolutions.isEmpty()) {
            return null;
        }
        return knownSolutions.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .max(Comparator.comparingInt(String::length))
            .orElse(null);
    }

    private String buildConsensusQuestion(String errorType, String rawErrorText, String context, String memoryFix) {
        StringBuilder q = new StringBuilder();
        q.append("You are fixing a SupremeAI backend error.\n");
        q.append("Error Type: ").append(errorType).append("\n");
        q.append("Context: ").append(safe(context)).append("\n");
        q.append("Error Log:\n").append(rawErrorText).append("\n\n");

        if (memoryFix != null) {
            q.append("Known memory solution from previous learning:\n");
            q.append(memoryFix).append("\n\n");
            q.append("Improve this memory solution if needed and provide exact fix steps.\n");
        } else {
            q.append("No previous memory solution found. Provide exact code-level fix steps.\n");
        }

        q.append("Output format:\n");
        q.append("1) Root cause\n2) Exact code/config changes\n3) Verification commands\n4) Risk checks\n");
        return q.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
