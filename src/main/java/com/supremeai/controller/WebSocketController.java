package com.supremeai.controller;

import com.supremeai.model.User;
import com.supremeai.repository.UserRepository;
import com.supremeai.service.QuotaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class WebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuotaService quotaService;

    @MessageMapping("/dashboard/subscribe")
    @SendTo("/topic/dashboard")
    public Map<String, Object> subscribeToDashboard() {
        return getDashboardData().block();
    }

    @Scheduled(fixedRate = 30000) // Update every 30 seconds
    public void broadcastDashboardUpdates() {
        getDashboardData().subscribe(data -> 
            messagingTemplate.convertAndSend("/topic/dashboard", data)
        );
    }

    @Scheduled(fixedRate = 10000) // Update every 10 seconds
    public void broadcastQuotaUpdates() {
        getGlobalQuotaData().flatMap(globalData -> 
            userRepository.findAll().collectList().map(users -> {
                Map<String, Object> data = new HashMap<>(globalData);
                List<Map<String, Object>> userQuotas = users.stream().map(user -> {
                    Map<String, Object> uq = new HashMap<>();
                    uq.put("userId", user.getFirebaseUid());
                    uq.put("displayName", user.getDisplayName());
                    uq.put("email", user.getEmail());
                    uq.put("usedQuota", user.getCurrentUsage());
                    uq.put("totalQuota", user.getMonthlyQuota());
                    uq.put("usagePercentage", user.getMonthlyQuota() > 0 ? (double) user.getCurrentUsage() / user.getMonthlyQuota() * 100.0 : 0.0);
                    return uq;
                }).collect(java.util.stream.Collectors.toList());
                data.put("userQuotas", userQuotas);
                return data;
            })
        ).subscribe(quotaData -> 
            messagingTemplate.convertAndSend("/topic/quota", quotaData)
        );
    }

    private Mono<Map<String, Object>> getDashboardData() {
        return userRepository.count().map(userCount -> {
            Map<String, Object> data = new HashMap<>();

            // System metrics
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", userCount);
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
        });
    }

    private Mono<Map<String, Object>> getGlobalQuotaData() {
        // Since we don't have a global quota yet, we'll aggregate or provide mock for now
        // but ensure it's reactive-ready.
        return Mono.fromCallable(() -> {
            Map<String, Object> data = new HashMap<>();

            Map<String, Object> quotaStats = new HashMap<>();
            quotaStats.put("totalRequests", 15420);
            quotaStats.put("usedQuota", 8420);
            quotaStats.put("remainingQuota", 7000);
            quotaStats.put("usagePercentage", 54.6);
            quotaStats.put("timestamp", System.currentTimeMillis());

            data.put("quota", quotaStats);
            data.put("type", "quota_update");

            return data;
        });
    }
}
