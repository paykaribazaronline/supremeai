package org.example.agentorchestration.learning;

import org.example.agentorchestration.ExpertAgentRouter;
import org.example.service.AgentDecisionLogger;
import org.example.service.AgentDecisionLogger.AgentDecision;
import org.example.service.SystemLearningService;
import org.example.service.ThetaLearningAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * LEVEL 2: Agent Pattern Profiler
 *
 * "কোন agent কোন pattern follow করে" — এটা শেখে।
 *
 * কীভাবে কাজ করে:
 *   - AgentDecisionLogger থেকে সব সফল decision collect করে
 *   - প্রতিটা agent-এর জন্য: কোন taskType-এ, কী reasoning, কত confidence
 *   - Pattern cluster করে: "B-Fixer সবসময় আগে analyze করে, তারপর fix করে"
 *   - ThetaLearningAgent-এর সাথে integrate করে রাখে
 *
 * Output: AgentProfile per agent
 *   - top_reasoning_patterns: ["always checks stderr", "prefers incremental fix"]
 *   - confidence_by_task: {BUG_FIX: 0.91, CODE_GENERATION: 0.67}
 *   - decision_signature: unique fingerprint of how this agent thinks
 */
@Service
public class AgentPatternProfiler {

    private static final Logger logger = LoggerFactory.getLogger(AgentPatternProfiler.class);

    // Minimum decisions needed to build a reliable profile
    private static final int MIN_DECISIONS_FOR_PROFILE = 3;

    @Autowired
    private AgentDecisionLogger decisionLogger;

    @Autowired
    private ThetaLearningAgent thetaAgent;

    @Autowired
    private SystemLearningService learningService;

    @Autowired
    private LearningFirebaseRepository firebaseRepo;

    @Autowired(required = false)
    private KnowledgeSeedService knowledgeSeedService;

    // In-memory profile store: agentName → AgentProfile
    private final Map<String, AgentProfile> profiles = new ConcurrentHashMap<>();

    /**
     * Build or refresh profiles for all agents from decision history.
     * Called automatically after every RLVR training step.
     */
    public Map<String, AgentProfile> buildAllProfiles() {
        logger.info("🔍 Level 2: Building agent pattern profiles from decision history...");

        List<AgentDecision> allDecisions = decisionLogger.getAllDecisions();
        if (allDecisions == null || allDecisions.isEmpty()) {
            logger.info("   No decisions yet — profiles will build as agents operate.");
            return profiles;
        }

        // Group by agent
        Map<String, List<AgentDecision>> byAgent = allDecisions.stream()
            .filter(d -> d.agent != null)
            .collect(Collectors.groupingBy(d -> d.agent));

        for (Map.Entry<String, List<AgentDecision>> entry : byAgent.entrySet()) {
            String agent = entry.getKey();
            List<AgentDecision> decisions = entry.getValue();

            if (decisions.size() < MIN_DECISIONS_FOR_PROFILE) {
                logger.debug("   Skipping {} — only {} decisions (need {})",
                    agent, decisions.size(), MIN_DECISIONS_FOR_PROFILE);
                continue;
            }

            AgentProfile profile = extractProfile(agent, decisions);
            profiles.put(agent, profile);

            // Persist to SystemLearning memory
            learningService.recordTechnique(
                "agent-pattern",
                agent + "-profile",
                profile.toSummary(),
                profile.topReasoningPatterns,
                profile.overallConfidence
            );

            // Persist to Firebase (not GitHub)
            firebaseRepo.saveProfile(agent, profile.toMap());

            logger.info("   ✅ Profile built: {} | patterns={} | confidence={:.2f}",
                agent, profile.topReasoningPatterns.size(), profile.overallConfidence);
        }

        ensureKnownProfilesPresent();
        logger.info("🔍 Level 2 complete: {} agent profiles built.", profiles.size());
        return profiles;
    }

    /**
     * Get profile for a single agent. Returns empty profile if not yet built.
     */
    public AgentProfile getProfile(String agentName) {
        return profiles.getOrDefault(agentName, AgentProfile.empty(agentName));
    }

    /**
     * Get all profiles as a summary map (for REST API).
     */
    public List<Map<String, Object>> getAllProfileSummaries() {
        ensureKnownProfilesPresent();
        return profiles.values().stream()
            .sorted(Comparator.comparing(profile -> profile.agentName == null ? "" : profile.agentName))
            .map(AgentProfile::toMap)
            .collect(Collectors.toList());
    }

    public void ensureKnownProfilesPresent() {
        for (String agentName : getKnownAgentUniverse()) {
            profiles.computeIfAbsent(agentName, AgentProfile::empty);
        }
    }

    /**
     * Update a single agent's profile after a new decision is logged.
     * Lightweight — called after each decision, not full rebuild.
     */
    public void updateProfile(String agentName, AgentDecision newDecision) {
        AgentProfile existing = profiles.getOrDefault(agentName, AgentProfile.empty(agentName));

        // Add new reasoning to pattern list
        if (newDecision.reasoning != null && !newDecision.reasoning.isBlank()) {
            existing.addReasoningObservation(newDecision.reasoning, newDecision.confidence);
        }

        // Update confidence by task type
        if (newDecision.taskType != null) {
            existing.updateConfidence(newDecision.taskType, newDecision.confidence,
                "SUCCESS".equals(newDecision.result));
        }

        profiles.put(agentName, existing);
    }

    // ── Internal ────────────────────────────────────────────────────────────────

    private AgentProfile extractProfile(String agentName, List<AgentDecision> decisions) {
        AgentProfile profile = AgentProfile.empty(agentName);

        // Only use successful decisions for pattern extraction
        List<AgentDecision> successful = decisions.stream()
            .filter(d -> "SUCCESS".equals(d.result))
            .collect(Collectors.toList());

        // Extract reasoning patterns from successful decisions
        Map<String, Integer> reasoningFrequency = new LinkedHashMap<>();
        for (AgentDecision d : successful) {
            if (d.reasoning != null && !d.reasoning.isBlank()) {
                String normalized = normalizeReasoning(d.reasoning);
                reasoningFrequency.merge(normalized, 1, Integer::sum);
            }
        }

        // Top 5 most frequent reasoning patterns
        profile.topReasoningPatterns = reasoningFrequency.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        // Confidence by task type
        Map<String, List<Float>> confByTask = new LinkedHashMap<>();
        for (AgentDecision d : decisions) {
            if (d.taskType != null) {
                confByTask.computeIfAbsent(d.taskType, k -> new ArrayList<>())
                    .add(d.confidence);
            }
        }
        confByTask.forEach((task, confs) -> {
            double avg = confs.stream().mapToDouble(Float::doubleValue).average().orElse(0);
            profile.confidenceByTaskType.put(task, avg);
        });

        // Overall confidence = average of successful decisions
        profile.overallConfidence = successful.stream()
            .mapToDouble(d -> d.confidence).average().orElse(0.5);

        // Decision signature: most common first step in multi-step decisions
        profile.decisionSignature = extractSignature(successful);

        profile.totalDecisions = decisions.size();
        profile.successfulDecisions = successful.size();
        profile.successRate = decisions.isEmpty() ? 0 :
            (double) successful.size() / decisions.size();

        return profile;
    }

    private String normalizeReasoning(String raw) {
        // Lowercase, strip noise words, keep core concept
        return raw.toLowerCase()
            .replaceAll("because|therefore|since|as|the|a|an|is|are|was|were", "")
            .replaceAll("\\s+", " ")
            .trim()
            .substring(0, Math.min(raw.length(), 80));
    }

    private String extractSignature(List<AgentDecision> decisions) {
        if (decisions.isEmpty()) return "no-data";
        // Most common decision keyword
        Map<String, Long> freq = decisions.stream()
            .filter(d -> d.decision != null)
            .collect(Collectors.groupingBy(
                d -> d.decision.split("\\s+")[0].toLowerCase(),
                Collectors.counting()));
        return freq.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("unknown");
    }

    private Set<String> getKnownAgentUniverse() {
        Set<String> knownAgents = new LinkedHashSet<>(ExpertAgentRouter.ALL_AGENTS);
        if (knowledgeSeedService != null) {
            knownAgents.addAll(knowledgeSeedService.getAllProviders());
        }
        return knownAgents;
    }

    // ── AgentProfile value object ───────────────────────────────────────────────

    public static class AgentProfile {
        public String agentName;
        public List<String> topReasoningPatterns = new ArrayList<>();
        public Map<String, Double> confidenceByTaskType = new LinkedHashMap<>();
        public String decisionSignature;
        public double overallConfidence;
        public int totalDecisions;
        public int successfulDecisions;
        public double successRate;
        public long lastUpdated = System.currentTimeMillis();

        public static AgentProfile empty(String name) {
            AgentProfile p = new AgentProfile();
            p.agentName = name;
            p.overallConfidence = 0.5;
            return p;
        }

        public void addReasoningObservation(String reasoning, float confidence) {
            String normalized = reasoning.substring(0, Math.min(reasoning.length(), 80));
            if (!topReasoningPatterns.contains(normalized)) {
                topReasoningPatterns.add(normalized);
                if (topReasoningPatterns.size() > 10) topReasoningPatterns.remove(0);
            }
            overallConfidence = overallConfidence * 0.9 + confidence * 0.1; // EMA
            lastUpdated = System.currentTimeMillis();
        }

        public void updateConfidence(String taskType, float confidence, boolean success) {
            double current = confidenceByTaskType.getOrDefault(taskType, 0.5);
            double updated = success
                ? current * 0.85 + confidence * 0.15
                : current * 0.95 - 0.02;
            confidenceByTaskType.put(taskType, Math.max(0.01, Math.min(1.0, updated)));
            lastUpdated = System.currentTimeMillis();
        }

        public String toSummary() {
            return String.format("Agent %s: patterns=%d, confidence=%.2f, successRate=%.0f%%",
                agentName, topReasoningPatterns.size(), overallConfidence, successRate * 100);
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("agent", agentName);
            m.put("top_reasoning_patterns", topReasoningPatterns);
            m.put("confidence_by_task_type", confidenceByTaskType);
            m.put("decision_signature", decisionSignature);
            m.put("overall_confidence", Math.round(overallConfidence * 100.0) / 100.0);
            m.put("total_decisions", totalDecisions);
            m.put("success_rate_pct", Math.round(successRate * 100));
            return m;
        }
    }
}
