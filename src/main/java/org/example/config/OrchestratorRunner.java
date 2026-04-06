package org.example.config;

import org.example.service.AgentOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 🚀 Cloud Orchestrator Runner
 *
 * Performs a startup health-check for the Spring-managed AgentOrchestrator bean
 * and logs readiness. The orchestrator itself is wired by Spring DI using the
 * {@code apiKeys}, {@code FirebaseService}, and {@code SystemConfig} beans defined
 * in {@link ServiceConfiguration} — no manual instantiation needed here.
 */
@Component
public class OrchestratorRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(OrchestratorRunner.class);

    @Autowired(required = false)
    private AgentOrchestrator agentOrchestrator;

    @Override
    public void run(String... args) {
        logger.info("\n🤖 SupremeAI Cloud Orchestrator startup check...");
        if (agentOrchestrator != null) {
            logger.info("✅ AgentOrchestrator is LIVE. Optimal agent (general): {}",
                agentOrchestrator.getOptimalAgent("general"));
        } else {
            logger.warn("⚠️ AgentOrchestrator bean is not available — some AI features may be inactive.");
        }
    }
}
