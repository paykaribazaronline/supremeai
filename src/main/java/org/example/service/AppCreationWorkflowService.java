package org.example.service;

import org.example.model.ConsensusVote;
import org.example.service.RequirementAnalyzer.Requirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * AppCreationWorkflowService
 *
 * End-to-end pipeline for "teach SupremeAI to create apps using AI model":
 * 1. Analyze natural-language requirement
 * 2. Generate base code artifacts
 * 3. Ask multi-AI consensus for quality/security upgrades
 * 4. Record generated techniques into learning memory
 */
@Service
public class AppCreationWorkflowService {

    private static final Logger logger = LoggerFactory.getLogger(AppCreationWorkflowService.class);

    @Autowired
    private RequirementAnalyzer requirementAnalyzer;

    @Autowired
    private CodeGenerator codeGenerator;

    @Autowired
    private MultiAIConsensusService consensusService;

    @Autowired
    private SystemLearningService learningService;

    @Autowired
    private AIErrorSolvingService errorSolvingService;

    public Map<String, Object> createAppPlanAndCode(String userId, String requirementText) {
        Map<String, Object> result = new HashMap<>();

        if (requirementText == null || requirementText.trim().isEmpty()) {
            result.put("status", "error");
            result.put("message", "requirementText is required");
            return result;
        }

        try {
            Requirement req = requirementAnalyzer.analyze(requirementText);
            String generatedCode = generateByType(req);

            String aiQuestion = buildEnhancementQuestion(req, generatedCode);
            ConsensusVote vote = consensusService.askAllAI(userId, aiQuestion);

            String aiEnhancements;
            double confidence;
            if (vote != null && vote.getWinningResponse() != null
                    && !vote.getWinningResponse().isBlank()
                    && !vote.getWinningResponse().contains("[QUOTA_EXCEEDED]")) {
                aiEnhancements = vote.getWinningResponse();
                confidence = vote.getConfidenceScore() != null ? vote.getConfidenceScore() : 0.0;
            } else {
                // Solo mode: generate built-in enhancements
                logger.info("🧠 Solo mode: generating built-in enhancements for {}", req.name);
                aiEnhancements = generateSoloEnhancements(req, generatedCode);
                confidence = 0.7; // Solo confidence
            }

            // Seed learning memory with this successful generation flow.
            learningService.recordRequirement(
                "App creation workflow execution",
                "Requirement=" + req.description + " | Type=" + req.type + " | Name=" + req.name
            );

            learningService.recordPattern(
                "APP_CREATION",
                "Generated app artifact for " + req.name,
                "Generated type=" + req.type + " with methods=" + req.methods.keySet()
            );

            learningService.recordPattern(
                "AI_SELECTION",
                "Consensus enhancement applied",
                "Confidence=" + confidence + " for requirement " + req.name
            );

            result.put("status", "success");
            result.put("requirement", req.description);
            result.put("analyzedName", req.name);
            result.put("analyzedType", req.type);
            result.put("dependencies", req.dependencies);
            result.put("methods", req.methods);
            result.put("generatedCode", generatedCode);
            result.put("aiEnhancements", aiEnhancements);
            result.put("consensusConfidence", confidence);
            result.put("timestamp", System.currentTimeMillis());

            logger.info("✅ App creation workflow completed for {} ({})", req.name, req.type);
        } catch (Exception e) {
            logger.error("❌ App creation workflow failed: {}", e.getMessage(), e);
            result.put("status", "error");
            result.put("message", e.getMessage());

            // Learn from failure.
            learningService.recordError(
                "APP_CREATION",
                "Workflow failed for requirement: " + requirementText,
                e,
                "Inspect generated methods, dependencies, and type detection for invalid signatures"
            );
        }

        return result;
    }

    /**
     * Convenience API to solve build/test errors after generation.
     */
    public Map<String, Object> solveGenerationError(String userId, String rawErrorText, String requirementContext) {
        return errorSolvingService.solveError(userId, rawErrorText, requirementContext);
    }

    private String generateByType(Requirement req) {
        if ("CONTROLLER".equals(req.type)) {
            return codeGenerator.generateController(req);
        }
        if ("MODEL".equals(req.type)) {
            return codeGenerator.generateModel(req);
        }
        return codeGenerator.generateService(req);
    }

    private String buildEnhancementQuestion(Requirement req, String code) {
        StringBuilder sb = new StringBuilder();
        sb.append("Improve this generated ").append(req.type).append(" for production use.\n");
        sb.append("Name: ").append(req.name).append("\n");
        sb.append("Requirement: ").append(req.description).append("\n");
        sb.append("Generated code:\n").append(code).append("\n\n");
        sb.append("Provide upgrades for:\n");
        sb.append("1) input validation\n");
        sb.append("2) error handling\n");
        sb.append("3) security (auth/admin checks if needed)\n");
        sb.append("4) testability\n");
        sb.append("5) performance\n");
        sb.append("Output exact patches or snippets.");
        return sb.toString();
    }

    /**
     * Solo mode: generate production-quality enhancements without external AI.
     * Applies well-known best practices based on the code type and structure.
     */
    private String generateSoloEnhancements(Requirement req, String generatedCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("[SOLO] Built-in Enhancement Recommendations for ").append(req.name).append("\n\n");

        String type = req.type != null ? req.type.toUpperCase() : "SERVICE";
        String code = generatedCode != null ? generatedCode.toLowerCase() : "";

        // 1. Input Validation
        sb.append("## 1. Input Validation\n");
        if ("CONTROLLER".equals(type)) {
            sb.append("- Add @Valid annotation on @RequestBody parameters\n");
            sb.append("- Add @NotNull, @NotBlank, @Size annotations on DTO fields\n");
            sb.append("- Validate path variables (check for null/empty/negative IDs)\n");
            sb.append("- Return 400 Bad Request with field-level error messages\n");
        } else if ("MODEL".equals(type)) {
            sb.append("- Add Bean Validation annotations (@NotNull, @Size, @Email, @Pattern)\n");
            sb.append("- Add custom validation for business rules\n");
            sb.append("- Validate relationships and constraints\n");
        } else {
            sb.append("- Validate all method parameters at entry point\n");
            sb.append("- Use Objects.requireNonNull() for critical parameters\n");
            sb.append("- Throw IllegalArgumentException with descriptive messages\n");
        }
        sb.append("\n");

        // 2. Error Handling
        sb.append("## 2. Error Handling\n");
        sb.append("- Wrap operations in try-catch with specific exception types\n");
        sb.append("- Use @ExceptionHandler or @ControllerAdvice for global handling\n");
        sb.append("- Log errors with full context (method, params, stack trace)\n");
        sb.append("- Return structured error responses: {error, message, timestamp, path}\n");
        if (code.contains("repository") || code.contains("database") || code.contains("jpa")) {
            sb.append("- Handle DataIntegrityViolationException for duplicate entries\n");
            sb.append("- Handle EntityNotFoundException for missing records\n");
        }
        sb.append("\n");

        // 3. Security
        sb.append("## 3. Security\n");
        if ("CONTROLLER".equals(type)) {
            sb.append("- Add @PreAuthorize or role checks on sensitive endpoints\n");
            sb.append("- Sanitize all string inputs to prevent XSS\n");
            sb.append("- Use parameterized queries (never concatenate SQL)\n");
            sb.append("- Add rate limiting (@RateLimiter) on public endpoints\n");
            sb.append("- Set CORS policy to restrict origins\n");
        } else {
            sb.append("- Never log sensitive data (passwords, tokens, PII)\n");
            sb.append("- Use proper access modifiers (private fields, package-private methods)\n");
            sb.append("- Validate data before persistence\n");
        }
        sb.append("\n");

        // 4. Testability
        sb.append("## 4. Testability\n");
        sb.append("- Extract dependencies via constructor injection (not field injection)\n");
        sb.append("- Keep methods focused and small (<20 lines)\n");
        sb.append("- Use interfaces for external dependencies (easy to mock)\n");
        sb.append("- Add unit test class: ").append(req.name).append("Test.java\n");
        if (req.methods != null && !req.methods.isEmpty()) {
            sb.append("- Test each method: ");
            sb.append(String.join(", ", req.methods.keySet()));
            sb.append("\n");
        }
        sb.append("- Test edge cases: null input, empty collections, boundary values\n");
        sb.append("\n");

        // 5. Performance
        sb.append("## 5. Performance\n");
        if (code.contains("list") || code.contains("findall") || code.contains("collection")) {
            sb.append("- Add pagination (Pageable) for list endpoints — never return all records\n");
        }
        sb.append("- Add @Cacheable for frequently accessed, rarely changed data\n");
        sb.append("- Use @Async for non-blocking operations where appropriate\n");
        sb.append("- Add proper indexes on queried database fields\n");
        if ("CONTROLLER".equals(type)) {
            sb.append("- Add response compression (gzip)\n");
            sb.append("- Use ETags for cache validation\n");
        }
        sb.append("\n");

        sb.append("## Mode: Solo (built-in best practices)\n");
        sb.append("*Connect an AI provider for code-specific patches and deeper analysis.*\n");

        return sb.toString();
    }
}
