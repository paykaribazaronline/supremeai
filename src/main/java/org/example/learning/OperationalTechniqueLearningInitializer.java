package org.example.learning;

import org.example.service.SystemLearningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Seeds app-creation and GPT-5.4 root-cause debugging techniques into SupremeAI memory.
 */
@Component
public class OperationalTechniqueLearningInitializer {

    private static final Logger logger = LoggerFactory.getLogger(OperationalTechniqueLearningInitializer.class);

    @Autowired
    private SystemLearningService systemLearningService;

    @EventListener(ApplicationReadyEvent.class)
    public void seedOperationalTechniques() {
        logger.info("🧠 OperationalTechniqueLearningInitializer: seeding app-creation and debugging techniques...");

        seedCriticalRequirements();
        seedAppCreationTechniques();
        seedErrorSolvingTechniques();
        seedBackendServiceMappings();

        logger.info("✅ OperationalTechniqueLearningInitializer: techniques seeded successfully.");
    }

    private void seedCriticalRequirements() {
        systemLearningService.recordRequirement(
            "App generation must end in verification",
            "Every generated app must pass compile, tests, runtime smoke checks, and security/config review before success is declared."
        );
        systemLearningService.recordRequirement(
            "Error solving must use root-cause reasoning",
            "SupremeAI must capture the exact failure, classify it, identify root cause, apply the smallest correct fix, re-run verification, and store the lesson."
        );
        systemLearningService.recordRequirement(
            "Security controls cannot be removed to make failures pass",
            "Keep token protection, input validation, and admin controls intact while fixing issues."
        );
    }

    private void seedAppCreationTechniques() {
        systemLearningService.recordTechnique(
            "APP_CREATION",
            "Normalize request into build specification",
            "Convert user intent into explicit architecture and delivery requirements before code generation starts.",
            Arrays.asList(
                "Extract app name, roles, platforms, features, integrations, deployment target, and test requirements.",
                "Infer missing pieces carefully and mark assumptions.",
                "Do not generate files until the specification is explicit enough to validate later."
            ),
            0.97,
            Map.of("phase", "planning", "source", "operational-playbook")
        );

        systemLearningService.recordTechnique(
            "APP_CREATION",
            "Choose architecture before generation",
            "Make architecture choices explicit so generated code is coherent across backend, frontend, mobile, and deployment layers.",
            Arrays.asList(
                "Use RequirementAnalyzer and MultiAIConsensusService to decide stack and boundaries.",
                "State backend, frontend, data model, API surface, and deployment choices explicitly.",
                "Reject vague stack labels like modern stack or latest framework."
            ),
            0.96,
            Map.of("phase", "architecture", "services", Arrays.asList("RequirementAnalyzer", "MultiAIConsensusService"))
        );

        systemLearningService.recordTechnique(
            "APP_CREATION",
            "Generate code in layers",
            "Create each feature in stable layers so validation and fixes stay localized.",
            Arrays.asList(
                "Generate data model first.",
                "Generate request and response DTOs next.",
                "Generate service logic and then controller endpoints.",
                "Add validation, security checks, and tests before declaring the feature complete."
            ),
            0.98,
            Map.of("phase", "generation", "services", Arrays.asList("CodeGenerator", "SelfExtender"))
        );

        systemLearningService.recordTechnique(
            "APP_CREATION",
            "Verify before claiming success",
            "A generated app is incomplete until verification evidence exists.",
            Arrays.asList(
                "Compile the generated code.",
                "Run failing tests first, then related tests, then broader validation.",
                "Perform runtime smoke tests for startup or endpoint health.",
                "Store evidence and confidence for reuse in SystemLearningService."
            ),
            0.99,
            Map.of("phase", "verification", "services", Arrays.asList("CodeValidationService", "SystemLearningService"))
        );
    }

    private void seedErrorSolvingTechniques() {
        systemLearningService.recordTechnique(
            "GPT54_DEBUGGING",
            "Use evidence-first debugging",
            "Fix the real cause of failures by starting from the actual error evidence instead of patching symptoms.",
            Arrays.asList(
                "Capture the exact error text, failing test, stack trace, or CI log fragment.",
                "Classify the failure: compilation, test, runtime, auth, deployment, configuration, API, or regression.",
                "Identify the failing file, method, and violated expectation before editing code."
            ),
            0.99,
            Map.of("phase", "triage", "services", Arrays.asList("GitHubActionsErrorParser", "AutoFixLoopService"))
        );

        systemLearningService.recordTechnique(
            "GPT54_DEBUGGING",
            "Apply the smallest correct fix",
            "Prefer one stable change at the right layer over broad rewrites.",
            Arrays.asList(
                "Propose one strongest root cause supported by evidence.",
                "Fix one service, controller, validation rule, or test at a time.",
                "Avoid broad rewrites unless architecture is the true cause."
            ),
            0.98,
            Map.of("phase", "fixing", "source", "operational-playbook")
        );

        systemLearningService.recordTechnique(
            "GPT54_DEBUGGING",
            "Re-run verification immediately after fixes",
            "A fix is incomplete until the same failure path is re-tested.",
            Arrays.asList(
                "Run the originally failing test or compile step first.",
                "Run the nearest related tests next.",
                "Store the failure signature, root cause, fix, and verification result for reuse."
            ),
            0.98,
            Map.of("phase", "verification", "services", Arrays.asList("AutoFixLoopService", "SystemLearningService"))
        );

        systemLearningService.recordTechnique(
            "GPT54_DEBUGGING",
            "Preserve security while debugging",
            "Never solve a bug by weakening authentication, setup token checks, or input validation.",
            Arrays.asList(
                "Validate env vars instead of bypassing auth.",
                "Keep setup endpoints token-protected.",
                "Keep stderr separate from stdout for command debugging.",
                "Never trust Git exit code alone when deciding success."
            ),
            0.99,
            Map.of("phase", "safety", "source", "COMMON_MISTAKES.md")
        );
    }

    private void seedBackendServiceMappings() {
        Map<String, Object> executionChainContext = new HashMap<>();
        executionChainContext.put("kind", "TECHNIQUE");
        executionChainContext.put("services", Arrays.asList(
            "RequirementAnalyzer",
            "MultiAIConsensusService",
            "CodeGenerator",
            "SelfExtender",
            "CodeValidationService",
            "GitHubActionsErrorParser",
            "AutoFixLoopService",
            "SystemLearningService"
        ));

        systemLearningService.recordTechnique(
            "BACKEND_SERVICES",
            "Use the operational execution chain",
            "Route every generation and repair task through the same backend service chain so decisions, validation, and learning stay connected.",
            Arrays.asList(
                "RequirementAnalyzer turns natural language into an actionable specification.",
                "MultiAIConsensusService helps pick architecture or repair direction.",
                "CodeGenerator and SelfExtender create and integrate code.",
                "CodeValidationService, GitHubActionsErrorParser, and AutoFixLoopService validate and repair.",
                "SystemLearningService stores what worked for future reuse."
            ),
            0.97,
            executionChainContext
        );
    }
}