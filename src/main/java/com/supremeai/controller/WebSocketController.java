package com.supremeai.controller;

import com.supremeai.model.User;
import com.supremeai.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import java.util.HashMap;
import java.util.Map;

@Controller
public class WebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UserRepository userRepository;

    @MessageMapping("/dashboard/subscribe")
    @SendTo("/topic/dashboard")
    public Map<String, Object> subscribeToDashboard() {
        return getDashboardData();
    }

    @Scheduled(fixedRate = 30000) // Update every 30 seconds
    public void broadcastDashboardUpdates() {
        Map<String, Object> data = getDashboardData();
        messagingTemplate.convertAndSend("/topic/dashboard", data);
    }

    @Scheduled(fixedRate = 10000) // Update every 10 seconds
    public void broadcastQuotaUpdates() {
        Map<String, Object> quotaData = getQuotaData();
        messagingTemplate.convertAndSend("/topic/quota", quotaData);
    }

    private Map<String, Object> getDashboardData() {
        Map<String, Object> data = new HashMap<>();

        // System metrics
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeAIAgents", 12);
        stats.put("systemHealthScore", 98.5);
        stats.put("runningProjects", 5);
        stats.put("completedProjects", 142);
        stats.put("successRate", 99.2);
        stats.put("systemHealthStatus", "HEALTHY");
        stats.put("timestamp", System.currentTimeMillis());

        data.put("stats", stats);
        data.put("type", "dashboard_update");

        return data;
    }

    private Map<String, Object> getQuotaData() {
        Map<String, Object> data = new HashMap<>();

        // Sample quota data - in real implementation, this would come from QuotaService
        Map<String, Object> quotaStats = new HashMap<>();
        quotaStats.put("totalRequests", 15420);
        quotaStats.put("usedQuota", 8420);
        quotaStats.put("remainingQuota", 7000);
        quotaStats.put("usagePercentage", 54.6);
        quotaStats.put("timestamp", System.currentTimeMillis());

        data.put("quota", quotaStats);
        data.put("type", "quota_update");

        return data;
    }
}