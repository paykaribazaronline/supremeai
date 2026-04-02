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

            String aiEnhancements = vote != null ? vote.getWinningResponse() : "No AI enhancement response.";
            double confidence = vote != null && vote.getConfidenceScore() != null ? vote.getConfidenceScore() : 0.0;

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
}
