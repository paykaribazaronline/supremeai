package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Phase 6: Agent Self-Reflection & Decision Logging Service
 * Tracks agent decision-making, reasoning, and learning for analysis
 * 
 * Purpose:
 * - Log all agent decisions with confidence scores
 * - Store reasoning chains for audit/learning
 * - Track voting progression and consensus formation
 * - Enable pattern learning from past decisions
 * 
 * Success Metrics:
 * - All decisions logged with metadata
 * - Confidence tracking: 0.0 - 1.0
 * - Decision queryable by: agent, project, time, confidence
 */
@Service
public class AgentDecisionLogger {
    
    private static final Logger logger = LoggerFactory.getLogger(AgentDecisionLogger.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String LOG_DIR = "./agent_decisions";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final Map<String, AgentDecision> recentDecisions = new ConcurrentHashMap<>();
    private final Map<String, List<AgentDecision>> decisionHistory = new ConcurrentHashMap<>();

    public AgentDecisionLogger() {
        try {
            Files.createDirectories(Paths.get(LOG_DIR));
        } catch (IOException e) {
            logger.warn("Failed to create agent decisions directory: {}", e.getMessage());
        }
    }

    @PostConstruct
    public void loadPersistedDecisions() {
        try {
            java.nio.file.Path logDir = Paths.get(LOG_DIR);
            if (!Files.exists(logDir)) return;
            // Walk all decision JSON files and reload into memory
            Files.walk(logDir)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(p -> {
                        try {
                            String content = Files.readString(p);
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> entries = mapper.readValue(content, List.class);
                            for (Map<String, Object> entry : entries) {
                                AgentDecision d = mapper.convertValue(entry, AgentDecision.class);
                                if (d.decisionId != null) {
                                    recentDecisions.put(d.decisionId, d);
                                    if (d.projectId != null) {
                                        decisionHistory.computeIfAbsent(d.projectId, k -> new ArrayList<>()).add(d);
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            logger.debug("Skip loading {}: {}", p.getFileName(), ex.getMessage());
                        }
                    });
            logger.info("✅ AgentDecisionLogger ready — loaded {} decisions from disk", recentDecisions.size());
        } catch (Exception e) {
            logger.warn("⚠️ Could not load persisted decisions: {}", e.getMessage());
        }
    }

    /**
     * Agent Decision Record - Complete decision metadata
     */
    public static class AgentDecision {
        public String decisionId;
        public String timestamp;
        public String agent;
        public String taskType;
        public String projectId;
        
        // Decision content
        public String decision;
        public String reasoning;
        public float confidence; // 0.0 - 1.0
        public List<String> alternativesConsidered;
        public String selectedAlternative;
        
        // Voting & Consensus
        public int totalVotes;
        public int votesForDecision;
        public float consensusPercentage;
        public List<String> votingAgents;
        public Map<String, Object> votingBreakdown; // agent -> confidence score
        
        // Outcome tracking
        public String status; // PENDING, APPLIED, REJECTED, SUPERSEDED
        public long appliedAt;
        public String result; // SUCCESS, FAILURE, PARTIAL
        public String outcome;
        public double successMetric;
        
        // Learning metadata
        public List<String> relatedPatterns;
        public boolean learned; // If this decision was used for pattern learning
        public long decisionTimeMs;
        
        public AgentDecision() {
            this.decisionId = UUID.randomUUID().toString();
            this.timestamp = LocalDateTime.now().format(formatter);
            this.votingBreakdown = new HashMap<>();
            this.alternativesConsidered = new ArrayList<>();
            this.votingAgents = new ArrayList<>();
            this.relatedPatterns = new ArrayList<>();
            this.status = "PENDING";
        }
    }

    /**
     * Log an agent's decision with full reasoning
     */
    public AgentDecision logDecision(String agent, String taskType, String projectId, 
                                     String decision, String reasoning, float confidence,
                                     List<String> alternatives) {
        AgentDecision agentDecision = new AgentDecision();
        agentDecision.agent = agent;
        agentDecision.taskType = taskType;
        agentDecision.projectId = projectId;
        agentDecision.decision = decision;
        agentDecision.reasoning = reasoning;
        agentDecision.confidence = Math.max(0, Math.min(1, confidence)); // Clamp to 0-1
        agentDecision.alternativesConsidered = alternatives;
        agentDecision.selectedAlternative = decision;
        
        // Store in memory
        recentDecisions.put(agentDecision.decisionId, agentDecision);
        decisionHistory.computeIfAbsent(projectId, k -> new ArrayList<>())
            .add(agentDecision);
        
        logger.debug("📊 Agent decision logged: {} - {} (confidence: {:.2f}%)", 
            agent, decision, agentDecision.confidence * 100);
        
        // Persist to disk
        persistDecision(agentDecision);
        
        return agentDecision;
    }

    /**
     * Log consensus voting for a decision
     */
    public void logConsensusVote(String decisionId, List<AgentVote> votes, float threshold) {
        AgentDecision decision = recentDecisions.get(decisionId);
        if (decision == null) return;
        
        decision.totalVotes = votes.size();
        decision.votesForDecision = (int) votes.stream()
            .filter(v -> v.approves).count();
        decision.consensusPercentage = (float) decision.votesForDecision / decision.totalVotes;
        
        // Record voting breakdown
        for (AgentVote vote : votes) {
            decision.votingAgents.add(vote.agent);
            decision.votingBreakdown.put(vote.agent, vote.confidence);
        }
        
        boolean consensusReached = decision.consensusPercentage >= threshold;
        logger.info("🗳️ Consensus vote: {} ({}/{} votes, {:.1f}%) - {}",
            decision.decision, decision.votesForDecision, decision.totalVotes,
            decision.consensusPercentage * 100,
            consensusReached ? "APPROVED" : "REJECTED");
        
        persistDecision(decision);
    }

    /**
     * Mark decision as applied
     */
    public void markDecisionApplied(String decisionId, long durationMs) {
        AgentDecision decision = recentDecisions.get(decisionId);
        if (decision == null) return;
        
        decision.status = "APPLIED";
        decision.appliedAt = System.currentTimeMillis();
        decision.decisionTimeMs = durationMs;
        
        logger.debug("✅ Decision applied: {} ({}ms)", decision.decision, durationMs);
        persistDecision(decision);
    }

    /**
     * Record decision outcome
     */
    public void recordDecisionOutcome(String decisionId, String result, String outcome,
                                     double successMetric, String... patterns) {
        AgentDecision decision = recentDecisions.get(decisionId);
        if (decision == null) return;
        
        decision.result = result;
        decision.outcome = outcome;
        decision.successMetric = Math.max(0, Math.min(1, successMetric));
        decision.relatedPatterns = Arrays.asList(patterns);
        
        String resultEmoji = "SUCCESS".equals(result) ? "✅" : 
                           "FAILURE".equals(result) ? "❌" : "⚠️";
        
        logger.info("{} Decision outcome: {} - Metric: {:.2f}%",
            resultEmoji, outcome, successMetric * 100);
        
        persistDecision(decision);
    }

    /**
     * Get decision history for a project
     */
    public List<AgentDecision> getProjectDecisions(String projectId) {
        return decisionHistory.getOrDefault(projectId, new ArrayList<>());
    }

    /**
     * Get decisions by agent
     */
    public List<AgentDecision> getAgentDecisions(String agent) {
        return recentDecisions.values().stream()
            .filter(d -> agent.equals(d.agent))
            .collect(Collectors.toList());
    }

    /**
     * Get high-confidence decisions (for pattern learning)
     */
    public List<AgentDecision> getHighConfidenceDecisions(float minConfidence) {
        return recentDecisions.values().stream()
            .filter(d -> d.confidence >= minConfidence)
            .filter(d -> "SUCCESS".equals(d.result))
            .collect(Collectors.toList());
    }

    /**
     * Get all decisions across all projects (used for ML pattern analysis)
     */
    public List<AgentDecision> getAllDecisions() {
        return recentDecisions.values().stream()
            .collect(Collectors.toList());
    }

    /**
     * Get decision statistics
     */
    public Map<String, Object> getDecisionStats() {
        int total = recentDecisions.size();
        int applied = (int) recentDecisions.values().stream()
            .filter(d -> "APPLIED".equals(d.status)).count();
        int successful = (int) recentDecisions.values().stream()
            .filter(d -> "SUCCESS".equals(d.result)).count();
        
        double avgConfidence = recentDecisions.values().stream()
            .mapToDouble(d -> d.confidence).average().orElse(0);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDecisions", total);
        stats.put("appliedDecisions", applied);
        stats.put("successfulDecisions", successful);
        stats.put("successRate", total > 0 ? (double) successful / total : 0);
        stats.put("averageConfidence", avgConfidence);
        stats.put("timestamp", System.currentTimeMillis());
        
        return stats;
    }

    /**
     * Agent voting record
     */
    public static class AgentVote {
        public String agent;
        public boolean approves;
        public float confidence; // 0.0 - 1.0
        public String reasoning;

        public AgentVote(String agent, boolean approves, float confidence, String reasoning) {
            this.agent = agent;
            this.approves = approves;
            this.confidence = Math.max(0, Math.min(1, confidence));
            this.reasoning = reasoning;
        }
    }

    /**
     * Persist decision to disk
     */
    private void persistDecision(AgentDecision decision) {
        try {
            String projectDir = LOG_DIR + "/" + decision.projectId;
            Files.createDirectories(Paths.get(projectDir));
            
            String dateFormat = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String logFile = projectDir + "/decisions_" + dateFormat + ".json";
            
            List<Map<String, Object>> decisions = new ArrayList<>();
            if (Files.exists(Paths.get(logFile))) {
                String content = new String(Files.readAllBytes(Paths.get(logFile)));
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> existing = mapper.readValue(content, List.class);
                decisions.addAll(existing);
            }
            
            Map<String, Object> decisionMap = mapper.convertValue(decision, Map.class);
            decisions.add(decisionMap);
            
            Files.write(Paths.get(logFile), 
                mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(decisions));
        } catch (IOException e) {
            logger.error("Failed to persist decision: {}", e.getMessage());
        }
    }

    /**
     * Clear old decisions (keep last N)
     */
    public void cleanupOldDecisions(int keepCount) {
        if (recentDecisions.size() > keepCount) {
            List<String> keys = new ArrayList<>(recentDecisions.keySet());
            keys = keys.subList(0, keys.size() - keepCount);
            for (String key : keys) {
                recentDecisions.remove(key);
            }
            logger.debug("Cleaned up old decisions, keeping {} most recent", keepCount);
        }
    }
}
