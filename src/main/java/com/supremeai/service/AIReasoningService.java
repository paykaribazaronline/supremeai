package com.supremeai.service;

import com.supremeai.model.ReasoningLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class AIReasoningService {

    private static final Logger log = LoggerFactory.getLogger(AIReasoningService.class);
    private final Queue<ReasoningLog> recentLogs = new ConcurrentLinkedQueue<>();
    private static final int MAX_RECENT_LOGS = 1000;

    public void logReasoning(String taskId, String decision, String reason, String modelName) {
        ReasoningLog reasoningLog = ReasoningLog.builder()
                .id(UUID.randomUUID().toString())
                .taskId(taskId)
                .decision(decision)
                .reason(reason)
                .modelName(modelName)
                .status("LOGGED")
                .timestamp(LocalDateTime.now())
                .build();

        recentLogs.add(reasoningLog);
        if (recentLogs.size() > MAX_RECENT_LOGS) {
            recentLogs.poll();
        }
        
        // In a real scenario, we would also save this to Firestore
        log.info("AI Reasoning Logged: [{}] because [{}]", decision, reason);
    }

    public List<ReasoningLog> getRecentLogs() {
        return new ArrayList<>(recentLogs);
    }
}
