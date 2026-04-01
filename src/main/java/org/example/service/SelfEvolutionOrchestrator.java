package org.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/**
 * PHASE 10: SELF-EVOLUTION ORCHESTRATOR
 * Automated management of the system's self-improvement cycle.
 */
@Service
public class SelfEvolutionOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(SelfEvolutionOrchestrator.class);

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

    /**
     * Run a full evolution cycle every 24 hours
     */
    @Scheduled(fixedRate = 86400000)
    public void runDailyEvolution() {
        logger.info("🌌 Starting Daily Self-Evolution Cycle...");

        // 1. Learn from previous day's builds
        Map<String, Object> learningReport = thetaAgent.learnPatterns();
        if (learningReport.containsKey("top_patterns")) {
            List<Map<String, Object>> patterns = (List<Map<String, Object>>) learningReport.get("top_patterns");
            patterns.forEach(firebaseService::saveLearnedPattern);
        }

        // 2. Synchronize knowledge base
        iotaAgent.manageKnowledge();

        // 3. Evolve agent configurations (Genetic Algorithm)
        Map<String, Object> evolutionReport = etaAgent.evolveAgents();
        firebaseService.saveEvolutionReport(evolutionReport);

        // 4. Evaluate Meta-Consensus & Apply improvements
        Map<String, Object> consensusReport = kappaAgent.orchestrateEvolution();
        processConsensus(consensusReport);

        logger.info("✓ Daily Self-Evolution Cycle completed successfully.");
    }

    private void processConsensus(Map<String, Object> report) {
        if (report.containsKey("promotion_status")) {
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
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("evolution_cycle", "ACTIVE");
        state.put("eta_meta", etaAgent.evolveAgents());
        state.put("kappa_consensus", kappaAgent.orchestrateEvolution());
        return state;
    }
}
