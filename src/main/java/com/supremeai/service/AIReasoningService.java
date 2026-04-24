package com.supremeai.service;

import com.supremeai.model.ReasoningLog;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class AIReasoningService {

    private final Queue<ReasoningLog> recentLogs = new ConcurrentLinkedQueue<>();
    private static final int MAX_RECENT_LOGS = 1000;

    public void logReasoning(String taskId, String decision, String reason, String modelName) {
        ReasoningLog log = ReasoningLog.builder()
                .id(UUID.randomUUID().toString())
                .taskId(taskId)
                .decision(decision)
                .reason(reason)
                .modelName(modelName)
                .status("LOGGED")
                .timestamp(LocalDateTime.now())
                .build();

        recentLogs.add(log);
        if (recentLogs.size() > MAX_RECENT_LOGS) {
            recentLogs.poll();
        }
        
        // In a real scenario, we would also save this to Firestore
        System.out.println("AI Reasoning Logged: [" + decision + "] because [" + reason + "]");
    }

    public List<ReasoningLog> getRecentLogs() {
        return new ArrayList<>(recentLogs);
    }
}
