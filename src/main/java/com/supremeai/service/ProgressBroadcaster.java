package com.supremeai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProgressBroadcaster {

    private static final Logger logger = LoggerFactory.getLogger(ProgressBroadcaster.class);
    private final SimpMessagingTemplate messagingTemplate;

    public ProgressBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastAppGenProgress(String requestId, String appName, String phase,
                                         int progressPercentage, String message) {
        try {
            java.util.Map<String, Object> progress = new java.util.HashMap<>();
            progress.put("type", "APP_GEN_PROGRESS");
            progress.put("requestId", requestId);
            progress.put("appName", appName);
            progress.put("phase", phase);
            progress.put("progress", progressPercentage);
            progress.put("message", message);
            progress.put("timestamp", System.currentTimeMillis());
            messagingTemplate.convertAndSend("/topic/app-gen", progress);
            messagingTemplate.convertAndSend("/topic/app-gen/" + requestId, progress);
        } catch (Exception e) {
            logger.debug("Progress broadcast failed (non-critical): {}", e.getMessage());
        }
    }
}
