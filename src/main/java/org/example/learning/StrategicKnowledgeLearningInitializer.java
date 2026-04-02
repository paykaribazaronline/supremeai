package org.example.learning;

import org.example.service.SystemLearningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;

/**
 * Seeds strategic product, governance, and operational knowledge into SupremeAI memory.
 */
@Component
public class StrategicKnowledgeLearningInitializer {

    private static final Logger logger = LoggerFactory.getLogger(StrategicKnowledgeLearningInitializer.class);

    @Autowired
    private SystemLearningService systemLearningService;

    @EventListener(ApplicationReadyEvent.class)
    public void seedStrategicKnowledge() {
        logger.info("🧠 StrategicKnowledgeLearningInitializer: seeding strategic system knowledge...");

        seedAdminControlKnowledge();
        seedSecurityAndGitRules();
        seedQuotaAndPerformanceKnowledge();
        seedArchitectureKnowledge();
        seedDeploymentScopeKnowledge();

        logger.info("✅ StrategicKnowledgeLearningInitializer: strategic knowledge seeded.");
    }

    private void seedAdminControlKnowledge() {
        systemLearningService.recordRequirement(
            "Three-mode admin control is mandatory",
            "All state-changing operations must respect AUTO, WAIT, and FORCE_STOP modes with admin override and audit trail."
        );
        systemLearningService.recordRequirement(
            "Firebase-only authentication must remain stable",
            "The system must not create hardcoded default users and should rely on Firebase Auth users with protected setup flows."
        );

        systemLearningService.recordTechnique(
            "ADMIN_CONTROL",
            "Check admin mode before mutating state",
            "Every generation, git, deployment, or queue-changing action must check the runtime admin mode first.",
            Arrays.asList(
                "Read current admin mode before starting any state-changing work.",
                "Proceed immediately in AUTO mode.",
                "Queue for approval in WAIT mode.",
                "Block immediately in FORCE_STOP mode."
            ),
            0.99,
            Map.of("source", "admin-control", "priority", "critical")
        );
    }

    private void seedSecurityAndGitRules() {
        systemLearningService.recordTechnique(
            "SECURITY",
            "Protect setup and auth paths",
            "Never open bootstrap or setup endpoints to unauthenticated callers just to make onboarding easier.",
            Arrays.asList(
                "Use /api/auth/setup with token protection instead of open init flows.",
                "Validate required auth environment variables at startup.",
                "Do not bypass auth checks to work around failing tests or demos."
            ),
            0.99,
            Map.of("source", "common-mistakes", "severity", "critical")
        );

        systemLearningService.recordTechnique(
            "GIT_OPERATIONS",
            "Execute Git safely and verify semantic results",
            "Git commands must be safe from injection and must verify real outcome instead of trusting process exit code only.",
            Arrays.asList(
                "Use ProcessBuilder array arguments, never shell string concatenation.",
                "Validate branch names against a safe regex before push or checkout.",
                "Capture stdout and stderr separately.",
                "Parse output for fatal, error, and nothing-to-commit states before reporting success."
            ),
            0.99,
            Map.of("source", "common-mistakes", "priority", "high")
        );

        systemLearningService.recordPattern(
            "SECURITY",
            "Never remove security controls to pass a build or unblock a feature",
            "Real fixes preserve setup-token validation, auth rules, and input validation. Fast but unsafe shortcuts create production incidents."
        );
    }

    private void seedQuotaAndPerformanceKnowledge() {
        systemLearningService.recordTechnique(
            "QUOTA",
            "Operate within solo and multi-AI limits",
            "SupremeAI must adapt generation rate, concurrency, and memory usage to the configured operating mode.",
            Arrays.asList(
                "SOLO mode target: 10 apps per day, 1 concurrent app, 30-second timeout, around 3 GB memory.",
                "MULTI_AI mode target: 100 apps per day, 5 concurrent apps, 5-second timeout, around 8 GB memory.",
                "Queue new work when the max queue size of 50 is reached.",
                "Trigger cleanup before memory exceeds 7 GB and alert when CPU exceeds 85 percent."
            ),
            0.97,
            Map.of("source", "QUOTA_CONFIG.properties", "priority", "high")
        );

        systemLearningService.recordPattern(
            "QUOTA",
            "High-confidence learned patterns should be reused aggressively",
            "The system already tracks around 90 learned patterns with roughly 0.92 average confidence, so known fixes should be favored over fresh guesswork when signatures match."
        );

        systemLearningService.recordTechnique(
            "PERFORMANCE",
            "Use mode-aware performance targets",
            "Generation quality should be judged against configured success-rate and code-quality targets instead of vague expectations.",
            Arrays.asList(
                "SOLO target: 0.92 success rate and 0.85 code quality.",
                "MULTI_AI target: 0.96 success rate and 0.95 code quality.",
                "Use cache, retry, and backoff before treating transient external failures as hard failures."
            ),
            0.95,
            Map.of("source", "QUOTA_CONFIG.properties")
        );
    }

    private void seedArchitectureKnowledge() {
        systemLearningService.recordTechnique(
            "ARCHITECTURE",
            "Use layered generation and service orchestration",
            "SupremeAI should generate and validate through the model, service, controller, validation, and deployment layers instead of mixing concerns.",
            Arrays.asList(
                "RequirementAnalyzer defines what is needed.",
                "MultiAIConsensusService helps choose the best architecture path.",
                "CodeGenerator and SelfExtender create and integrate the implementation.",
                "Validation and auto-fix services verify and repair before completion."
            ),
            0.98,
            Map.of("source", "system-architecture")
        );

        systemLearningService.recordPattern(
            "SYSTEM_PHILOSOPHY",
            "SupremeAI is always a student with great ability",
            "It should learn from past failures, multi-AI consensus, feedback loops, and verified outcomes instead of acting like a one-shot code generator."
        );
    }

    private void seedDeploymentScopeKnowledge() {
        systemLearningService.recordTechnique(
            "DEPLOYMENT_SCOPE",
            "Generate all platforms but publish with correct ownership boundaries",
            "SupremeAI can generate production artifacts broadly, but publishing responsibility differs by platform.",
            Arrays.asList(
                "SupremeAI can generate backend, web, mobile, and desktop code and artifacts.",
                "SupremeAI may automate web deployment.",
                "Android Play Store and iOS App Store publishing remain owner-controlled because legal accounts and signing assets belong to the owner.",
                "System guidance should export production-ready artifacts and owner instructions for mobile publishing."
            ),
            0.96,
            Map.of("source", "deployment-scope", "priority", "medium")
        );

        systemLearningService.recordPattern(
            "DEPLOYMENT_SCOPE",
            "Separate build generation from legal publication responsibility",
            "This keeps the system technically capable while respecting real-world ownership, signing, and store-account constraints."
        );
    }
}