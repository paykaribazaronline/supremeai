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
}
