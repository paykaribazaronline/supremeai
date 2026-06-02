package com.supremeai.service;

import com.supremeai.model.ActivityLog;
import com.supremeai.repository.ActivityLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * System Audit Service - Phase 4
 * Responsible for recording system activities and broadcasting them via WebSocket.
 */
@Service
public class SystemAuditService {
    public SystemAuditService(ActivityLogRepository activityLogRepository, SimpMessagingTemplate messagingTemplate) {
        this.activityLogRepository = activityLogRepository;
        this.messagingTemplate = messagingTemplate;
    }


    private static final Logger logger = LoggerFactory.getLogger(SystemAuditService.class);



    /**
     * Log a system activity and broadcast it.
     */
    public Mono<ActivityLog> log(String action, String user, String category, String severity, String details, String outcome, String ip) {
        ActivityLog log = new ActivityLog(action, user, category, severity, details, outcome, ip);
        
        return activityLogRepository.save(log)
                .doOnSuccess(savedLog -> {
                    broadcastLog(savedLog);
                    logger.info("Activity Log Saved: {} by {}", action, user);
                })
                .doOnError(e -> logger.error("Failed to save activity log: {}", e.getMessage()));
    }

    /**
     * Broadcast the log to subscribers.
     */
    private void broadcastLog(ActivityLog log) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "SYSTEM_LOG");
        payload.put("level", log.getSeverity() != null ? log.getSeverity().toUpperCase() : "INFO");
        payload.put("component", log.getCategory());
        payload.put("message", log.getAction() + ": " + log.getDetails());
        payload.put("timestamp", log.getTimestamp() != null
                ? log.getTimestamp().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                : System.currentTimeMillis());
        payload.put("user", log.getUser());
        payload.put("outcome", log.getOutcome());

        messagingTemplate.convertAndSend("/topic/monitoring", payload);
    }
}
