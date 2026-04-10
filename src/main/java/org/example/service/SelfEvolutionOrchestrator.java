package org.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.*;

/**
 * PHASE 10: SELF-EVOLUTION ORCHESTRATOR
 * Automated management of the system's self-improvement cycle.
 */
@Service
public class SelfEvolutionOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(SelfEvolutionOrchestrator.class);

    /** Maximum time any single agent call is allowed to take before being abandoned. */
    private static final long AGENT_TIMEOUT_MINUTES = 10;

    @Autowired
    private EtaMetaAgent etaAgent;
    @Autowired
    private ThetaLearningAgent thetaAgent;
    @Autowired
    private IotaKnowledgeAgent iotaAgent;
    @Autowired
    private KappaEvolutionAgent kappaAgent;
    @Autowired
    private FirebaseService firebaseService;
    @Autowired
    private SystemModeService systemModeService;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "evolution-worker");
        t.setDaemon(true);
        return t;
    });

    @jakarta.annotation.PreDestroy
    void shutdownExecutor() {
        executor.shutdownNow();
    }

    /**
     * Run a full evolution cycle every 24 hours.
     * Each agent call is wrapped in a timeout to prevent the scheduled thread from hanging.
     */
    @Scheduled(fixedRate = 86400000)
    public void runDailyEvolution() {
        SystemModeService.OperationDecision decision =
            systemModeService.canExecuteOperation("ANALYZE_PERFORMANCE", 90);
        if (!decision.isAllowed()) {
            logger.info("⏸️ Daily self-evolution skipped by system mode: {}", decision.getReason());
            return;
        }

        logger.info("🌌 Starting Daily Self-Evolution Cycle...");

        try {
            // 1. Learn from previous day's builds
            Map<String, Object> learningReport = runWithTimeout("ThetaAgent.learnPatterns",
                    () -> thetaAgent.learnPatterns());
            if (learningReport != null && learningReport.containsKey("top_patterns")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> patterns =
                        (List<Map<String, Object>>) learningReport.get("top_patterns");
                patterns.forEach(firebaseService::saveLearnedPattern);
            }

            // 2. Synchronize knowledge base
            runWithTimeout("IotaAgent.manageKnowledge", () -> {
                iotaAgent.manageKnowledge();
                return null;
            });

            // 3. Evolve agent configurations (Genetic Algorithm)
            Map<String, Object> evolutionReport = runWithTimeout("EtaAgent.evolveAgents",
                    () -> etaAgent.evolveAgents());
            if (evolutionReport != null) {
                firebaseService.saveEvolutionReport(evolutionReport);
            }

            // 4. Evaluate Meta-Consensus & Apply improvements
            Map<String, Object> consensusReport = runWithTimeout("KappaAgent.orchestrateEvolution",
                    () -> kappaAgent.orchestrateEvolution());
            if (consensusReport != null) {
                processConsensus(consensusReport);
            }

            logger.info("✓ Daily Self-Evolution Cycle completed successfully.");
        } catch (Exception e) {
            logger.error("❌ Daily Self-Evolution Cycle failed: {}", e.getMessage(), e);
        }
    }

    private void processConsensus(Map<String, Object> report) {
        if (report.containsKey("promotion_status")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> promotion = (Map<String, Object>) report.get("promotion_status");
            if ("READY_FOR_PROMOTION".equals(promotion.get("status"))) {
                String variantId = (String) promotion.get("pending_promotion");
                logger.info("🚀 PROMOTING NEW SYSTEM CONFIGURATION: {}", variantId);
                
                Map<String, Object> newConfig = new HashMap<>();
                newConfig.put("active_variant", variantId);
                newConfig.put("evolution_timestamp", System.currentTimeMillis());
                
                firebaseService.updateActiveSystemConfig(newConfig);
                firebaseService.sendNotification("admin", "System Evolved", 
                    "New configuration " + variantId + " has been promoted by meta-consensus.", "INFO");
            }
        }
    }

    public Map<String, Object> getSystemEvolutionState() {
        SystemModeService.OperationDecision decision =
            systemModeService.canExecuteOperation("ANALYZE_PERFORMANCE", 90);
        if (!decision.isAllowed()) {
            return Map.of(
                "evolution_cycle", "BLOCKED_BY_MODE",
                "reason", decision.getReason(),
                "requiresApproval", decision.isRequiresApproval()
            );
        }

        Map<String, Object> state = new LinkedHashMap<>();
        state.put("evolution_cycle", "ACTIVE");
        state.put("eta_meta", etaAgent.evolveAgents());
        state.put("kappa_consensus", kappaAgent.orchestrateEvolution());
        return state;
    }

    /**
     * Run a {@link Callable} on the worker thread with a timeout.
     * Returns null if the call times out or throws.
     */
    private <T> T runWithTimeout(String label, Callable<T> task) {
        Future<T> future = executor.submit(task);
        try {
            return future.get(AGENT_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            future.cancel(true);
            logger.warn("⏱️ {} timed out after {} minutes — skipping", label, AGENT_TIMEOUT_MINUTES);
            return null;
        } catch (Exception e) {
            logger.warn("⚠️ {} failed: {}", label, e.getMessage());
            return null;
        }
    }
}
