package com.supremeai.service;

import com.supremeai.model.ReasoningLog;
import reactor.core.publisher.Mono;
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
    @org.springframework.beans.factory.annotation.Autowired
    private ConfigService configService;

    private int getMaxRecentLogs() {
        return configService.getSetting("max_recent_logs", 1000);
    }

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
        if (recentLogs.size() > getMaxRecentLogs()) {
            recentLogs.poll();
        }
        
        // In a real scenario, we would also save this to Firestore
        log.info("AI Reasoning Logged: [{}] because [{}]", decision, reason);
    }

    public Mono<Void> logReasoningAsync(String taskId, String decision, String reason, String modelName) {
        return Mono.fromRunnable(() -> logReasoning(taskId, decision, reason, modelName))
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .then();
    }

    public List<ReasoningLog> getRecentLogs() {
        return new ArrayList<>(recentLogs);
    }
}
