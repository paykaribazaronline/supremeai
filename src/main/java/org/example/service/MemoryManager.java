package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class MemoryManager {
    private final ObjectMapper mapper = new ObjectMapper();
    private final String memoryFilePath = "memory.json";
    private ObjectNode memory;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private FirebaseService firebaseService;

    public MemoryManager() {
        loadMemory();
    }
    
    public void setFirebaseService(FirebaseService firebaseService) {
        this.firebaseService = firebaseService;
        syncWithFirebase();
    }

    private void loadMemory() {
        File file = new File(memoryFilePath);
        if (file.exists()) {
            try {
                memory = (ObjectNode) mapper.readTree(file);
            } catch (IOException e) {
                memory = mapper.createObjectNode();
            }
        } else {
            memory = mapper.createObjectNode();
        }
        
        // Initialize structures if missing
        if (!memory.has("success_patterns")) memory.set("success_patterns", mapper.createArrayNode());
        if (!memory.has("fail_history")) memory.set("fail_history", mapper.createArrayNode());
        if (!memory.has("ai_scoreboard")) memory.set("ai_scoreboard", mapper.createObjectNode());
        if (!memory.has("safezone_agents")) memory.set("safezone_agents", mapper.createArrayNode());
        
        // Phase 2: Pattern library and failure patterns
        if (!memory.has("pattern_library")) memory.set("pattern_library", mapper.createObjectNode());
        if (!memory.has("failure_patterns")) memory.set("failure_patterns", mapper.createArrayNode());
    }

    private void syncWithFirebase() {
        if (firebaseService != null) {
            Map<String, Object> remoteConfig = firebaseService.getSystemConfig("ai_memory");
            if (remoteConfig != null && !remoteConfig.isEmpty()) {
                System.out.println("🔄 Memory synced from Firebase");
                saveToFirebase();
            }
        }
    }

    private void saveToFirebase() {
        if (firebaseService != null) {
            try {
                Map<String, Object> memoryMap = mapper.convertValue(memory, Map.class);
                firebaseService.saveSystemConfig("ai_memory", memoryMap);
            } catch (Exception e) {
                System.err.println("Cloud memory sync failed: " + e.getMessage());
            }
        }
    }

    public void recordSuccess(String patternId, String agentId, int timeTaken) {
        ArrayNode successes = (ArrayNode) memory.get("success_patterns");
        ObjectNode entry = mapper.createObjectNode();
        entry.put("pattern_id", patternId);
        entry.put("agent", agentId);
        entry.put("time_taken", timeTaken);
        entry.put("timestamp", LocalDateTime.now().format(formatter));
        successes.add(entry);
        
        updateAIScoreboard(agentId, true, timeTaken);
        saveMemory();
        saveToFirebase();
    }

    public void recordFailure(String taskId, String agentId, String reason) {
        ArrayNode failures = (ArrayNode) memory.get("fail_history");
        ObjectNode entry = mapper.createObjectNode();
        entry.put("task_id", taskId);
        entry.put("agent", agentId);
        entry.put("reason", reason);
        entry.put("timestamp", LocalDateTime.now().format(formatter));
        failures.add(entry);
        
        updateAIScoreboard(agentId, false, 0);
        saveMemory();
        saveToFirebase();
    }

    public void updateAIScoreboard(String agentId, boolean success, int timeTaken) {
        ObjectNode scoreboard = (ObjectNode) memory.get("ai_scoreboard");
        ObjectNode agentScore = (ObjectNode) scoreboard.get(agentId);
        
        if (agentScore == null) {
            agentScore = mapper.createObjectNode();
            agentScore.put("success_count", 0);
            agentScore.put("fail_count", 0);
            agentScore.put("avg_time", 0);
            scoreboard.set(agentId, agentScore);
        }
        
        if (success) {
            agentScore.put("success_count", agentScore.get("success_count").asInt() + 1);
            int prevAvg = agentScore.get("avg_time").asInt();
            int totalCount = agentScore.get("success_count").asInt();
            int newAvg = (prevAvg * (totalCount - 1) + timeTaken) / totalCount;
            agentScore.put("avg_time", newAvg);
        } else {
            agentScore.put("fail_count", agentScore.get("fail_count").asInt() + 1);
        }
    }

    /**
     * এডমিনের জন্য Top 10 এবং Safezone AI-এর পারফরম্যান্স রিপোর্ট তৈরি করে।
     */
    public List<Map<String, Object>> getTopAgentsReport(int limit) {
        ObjectNode scoreboard = (ObjectNode) memory.get("ai_scoreboard");
        List<Map<String, Object>> reports = new ArrayList<>();

        Iterator<String> fieldNames = scoreboard.fieldNames();
        while (fieldNames.hasNext()) {
            String agentId = fieldNames.next();
            ObjectNode agentScore = (ObjectNode) scoreboard.get(agentId);

            int success = agentScore.get("success_count").asInt();
            int fail = agentScore.get("fail_count").asInt();
            int avgTime = agentScore.get("avg_time").asInt();
            double successRate = (double) success / Math.max(1, success + fail) * 100;

            Map<String, Object> report = new HashMap<>();
            report.put("agentId", agentId);
            report.put("successRate", String.format("%.1f%%", successRate));
            report.put("avgTime", avgTime + "ms");
            report.put("totalTasks", success + fail);
            report.put("performanceRank", calculateRank(successRate, avgTime));
            report.put("isSafezone", isAgentInSafezone(agentId));
            
            reports.add(report);
        }

        // সাকসেস রেট অনুযায়ী সর্ট করে Top Limit (যেমন ১০টি) রিটার্ন করবে
        reports.sort((a, b) -> Double.compare(
            Double.parseDouble(((String) b.get("successRate")).replace("%", "")),
            Double.parseDouble(((String) a.get("successRate")).replace("%", ""))
        ));

        return reports.stream().limit(limit).collect(Collectors.toList());
    }

    private String calculateRank(double successRate, int avgTime) {
        if (successRate > 90 && avgTime < 2000) return "👑 MASTER (Top Performer)";
        if (successRate > 75) return "✅ RELIABLE";
        if (successRate > 50) return "⚖️ STABLE";
        return "⚠️ RISKY";
    }

    public List<String> getSafezoneAgents() {
        List<String> safezone = new ArrayList<>();
        ArrayNode agents = (ArrayNode) memory.get("safezone_agents");
        if (agents != null) {
            agents.forEach(a -> safezone.add(a.asText()));
        }
        return safezone;
    }

    public void addToSafezone(String agentId) {
        ArrayNode agents = (ArrayNode) memory.get("safezone_agents");
        boolean exists = false;
        for (int i = 0; i < agents.size(); i++) {
            if (agents.get(i).asText().equals(agentId)) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            agents.add(agentId);
            saveMemory();
            saveToFirebase();
        }
    }

    public boolean isAgentInSafezone(String agentId) {
        ArrayNode agents = (ArrayNode) memory.get("safezone_agents");
        for (int i = 0; i < agents.size(); i++) {
            if (agents.get(i).asText().equals(agentId)) return true;
        }
        return false;
    }

    public Map<String, Object> getAIScoreboard() {
        ObjectNode scoreboard = (ObjectNode) memory.get("ai_scoreboard");
        Map<String, Object> result = new HashMap<>();
        
        Iterator<String> fieldNames = scoreboard.fieldNames();
        while (fieldNames.hasNext()) {
            String agentId = fieldNames.next();
            ObjectNode agentScore = (ObjectNode) scoreboard.get(agentId);
            
            Map<String, Object> score = new HashMap<>();
            score.put("success_count", agentScore.get("success_count").asInt());
            score.put("fail_count", agentScore.get("fail_count").asInt());
            score.put("avg_time", agentScore.get("avg_time").asInt());
            result.put(agentId, score);
        }
        
        return result;
    }

    private void saveMemory() {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(memoryFilePath), memory);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ============================================================================
    // PHASE 2: Pattern Library & Intelligent Ranking Methods
    // ============================================================================

    /**
     * Phase 2: Get all historical patterns for a specific task type
     * Used by AIRankingService for task-type-specific ranking
     */
    public List<Map<String, Object>> getPatternsByTaskType(String taskType) {
        ObjectNode patternLib = (ObjectNode) memory.get("pattern_library");
        List<Map<String, Object>> patterns = new ArrayList<>();
        
        ArrayNode typePatterns = (ArrayNode) patternLib.get(taskType);
        if (typePatterns == null) {
            return patterns; // Empty if no patterns for this task type
        }
        
        for (int i = 0; i < typePatterns.size(); i++) {
            ObjectNode pattern = (ObjectNode) typePatterns.get(i);
            Map<String, Object> p = new HashMap<>();
            p.put("agent", pattern.get("agent").asText());
            p.put("success", pattern.get("success").asBoolean());
            p.put("time_taken", pattern.get("time_taken").asInt());
            p.put("timestamp", pattern.get("timestamp").asText());
            patterns.add(p);
        }
        
        return patterns;
    }

    /**
     * Phase 2: Record a pattern for a task type
     * Called when task completes successfully
     */
    public void recordPattern(String taskType, String agentId, boolean success, int timeTaken) {
        ObjectNode patternLib = (ObjectNode) memory.get("pattern_library");
        
        ArrayNode typePatterns = (ArrayNode) patternLib.get(taskType);
        if (typePatterns == null) {
            typePatterns = mapper.createArrayNode();
            patternLib.set(taskType, typePatterns);
        }
        
        ObjectNode pattern = mapper.createObjectNode();
        pattern.put("agent", agentId);
        pattern.put("success", success);
        pattern.put("time_taken", timeTaken);
        pattern.put("timestamp", LocalDateTime.now().format(formatter));
        typePatterns.add(pattern);
        
        saveMemory();
        saveToFirebase();
    }

    /**
     * Phase 2: Calculate agent score using Phase 2 formula
     * Score = (Success_Rate × 0.5) - (Failure_Rate × 0.3) + (Speed_Bonus × 0.2)
     * Speed_Bonus = max(0, 1 - (avg_time / baseline_time))
     * Baseline = 30000ms (30 seconds)
     */
    public double calculateAgentScore(String agentId) {
        ObjectNode scoreboard = (ObjectNode) memory.get("ai_scoreboard");
        ObjectNode agentScore = (ObjectNode) scoreboard.get(agentId);
        
        if (agentScore == null) {
            return 0.0;
        }
        
        int successCount = agentScore.get("success_count").asInt();
        int failCount = agentScore.get("fail_count").asInt();
        int totalCount = successCount + failCount;
        
        if (totalCount == 0) {
            return 0.0; // No data yet
        }
        
        double successRate = (double) successCount / totalCount;
        double failureRate = (double) failCount / totalCount;
        int avgTime = agentScore.get("avg_time").asInt();
        
        // Speed bonus with 30-second baseline
        final int BASELINE_TIME = 30000;
        double speedBonus = Math.max(0, 1 - ((double) avgTime / BASELINE_TIME));
        
        // Phase 2 scoring formula
        double score = (successRate * 0.5) - (failureRate * 0.3) + (speedBonus * 0.2);
        
        return Math.max(0, score); // Never return negative scores
    }

    /**
     * Phase 2: Get top K agents by calculated score
     * Returns agent IDs sorted by score (highest first)
     */
    public List<String> getTopAgents(int k) {
        ObjectNode scoreboard = (ObjectNode) memory.get("ai_scoreboard");
        
        List<String> agents = new ArrayList<>();
        Iterator<String> fieldNames = scoreboard.fieldNames();
        
        while (fieldNames.hasNext()) {
            agents.add(fieldNames.next());
        }
        
        // Sort by score (highest first)
        agents.sort((a, b) -> Double.compare(
            calculateAgentScore(b),
            calculateAgentScore(a)
        ));
        
        // Return top k
        return agents.stream()
                .limit(k)
                .collect(Collectors.toList());
    }

    /**
     * Phase 2: Record a failure pattern for learning
     * Failure types: TIMEOUT, RATE_LIMIT, TOKEN_LIMIT, API_ERROR, LOGIC_ERROR
     */
    public void recordFailurePattern(String taskType, String agentId, String errorType) {
        ArrayNode failurePatterns = (ArrayNode) memory.get("failure_patterns");
        
        ObjectNode failure = mapper.createObjectNode();
        failure.put("task_type", taskType);
        failure.put("agent", agentId);
        failure.put("error_type", errorType);
        failure.put("timestamp", LocalDateTime.now().format(formatter));
        failurePatterns.add(failure);
        
        saveMemory();
        saveToFirebase();
    }

    /**
     * Phase 2: Get all failure patterns (for analysis and learning)
     */
    public List<Map<String, Object>> getFailurePatterns() {
        List<Map<String, Object>> patterns = new ArrayList<>();
        ArrayNode failurePatterns = (ArrayNode) memory.get("failure_patterns");
        
        for (int i = 0; i < failurePatterns.size(); i++) {
            ObjectNode failure = (ObjectNode) failurePatterns.get(i);
            Map<String, Object> p = new HashMap<>();
            p.put("task_type", failure.get("task_type").asText());
            p.put("agent", failure.get("agent").asText());
            p.put("error_type", failure.get("error_type").asText());
            p.put("timestamp", failure.get("timestamp").asText());
            patterns.add(p);
        }
        
        return patterns;
    }

    /**
     * Phase 2: Get failure patterns for a specific agent
     * Used to identify problem agents that need rest
     */
    public List<Map<String, Object>> getFailurePatternsByAgent(String agentId) {
        List<Map<String, Object>> agentFailures = new ArrayList<>();
        
        for (Map<String, Object> failure : getFailurePatterns()) {
            if (failure.get("agent").equals(agentId)) {
                agentFailures.add(failure);
            }
        }
        
        return agentFailures;
    }

    /**
     * Phase 2: Get failure patterns by error type
     * Used to identify systemic issues (e.g., quota limits)
     */
    public List<Map<String, Object>> getFailurePatternsByErrorType(String errorType) {
        List<Map<String, Object>> typeFailures = new ArrayList<>();
        
        for (Map<String, Object> failure : getFailurePatterns()) {
            if (failure.get("error_type").equals(errorType)) {
                typeFailures.add(failure);
            }
        }
        
        return typeFailures;
    }

    /**
     * Phase 2: Get recent failure patterns (last N records)
     * Used for trend analysis
     */
    public List<Map<String, Object>> getRecentFailurePatterns(int limit) {
        List<Map<String, Object>> allFailures = getFailurePatterns();
        
        return allFailures.stream()
                .skip(Math.max(0, allFailures.size() - limit))
                .collect(Collectors.toList());
    }
}
