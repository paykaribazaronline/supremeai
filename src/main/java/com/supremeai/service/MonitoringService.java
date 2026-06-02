package com.supremeai.service;

import com.supremeai.controller.SystemMetricsController;
import com.supremeai.model.MonitoringLog;
import com.supremeai.repository.MonitoringLogRepository;
import com.supremeai.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class MonitoringService {
    public MonitoringService(SimpMessagingTemplate messagingTemplate, ProductionHealthMonitor healthMonitor, MonitoringLogRepository monitoringLogRepository) {
        this.messagingTemplate = messagingTemplate;
        this.healthMonitor = healthMonitor;
        this.monitoringLogRepository = monitoringLogRepository;
    }


    private static final Logger logger = LoggerFactory.getLogger(MonitoringService.class);




    private static final double CPU_ALERT_THRESHOLD = 90.0;
    private static final double MEMORY_ALERT_THRESHOLD = 90.0;

    /**
     * Broadcast system resource metrics every 5 seconds.
     * This replaces polling from the frontend.
     */
    @Scheduled(fixedRate = 5000)
    public void broadcastSystemMetrics() {
        try {
            Map<String, Object> health = healthMonitor.getHealthStatus();
            Map<String, Object> metrics = (Map<String, Object>) health.get("metrics");
            if (metrics != null) {
                Map<String, Object> data = new HashMap<>(metrics);
                data.put("type", "SYSTEM_RESOURCES");
                
                // Add some extra computed metrics for the UI
                double cpuLoad = (double) metrics.getOrDefault("cpuLoad", 0.0);
                int cores = (int) metrics.getOrDefault("availableProcessors", 1);
                double cpuUsage = Math.min((cpuLoad / cores) * 100, 100.0);
                metrics.put("cpuUsagePercentage", cpuUsage);

                // Memory metrics check
                long freeMemory = (long) metrics.getOrDefault("freeMemory", 0L);
                long totalMemory = (long) metrics.getOrDefault("totalMemory", 1L);
                double memoryUsage = ((double)(totalMemory - freeMemory) / totalMemory) * 100;
                metrics.put("memoryUsagePercentage", memoryUsage);
                
                messagingTemplate.convertAndSend("/topic/monitoring", data);

                // Resource Alerts
                if (cpuUsage > CPU_ALERT_THRESHOLD) {
                    broadcastLog("ALERT", "System", "High CPU Usage detected: " + String.format("%.2f", cpuUsage) + "%");
                }
                if (memoryUsage > MEMORY_ALERT_THRESHOLD) {
                    broadcastLog("ALERT", "System", "High Memory Usage detected: " + String.format("%.2f", memoryUsage) + "%");
                }

                logger.trace("Broadcasted system metrics: {}", metrics);
            }
        } catch (Exception e) {
            logger.error("Error broadcasting system metrics: {}", e.getMessage());
        }
    }

    /**
     * Broadcast a custom monitoring log message and persist to Firestore.
     */
    public void broadcastLog(String level, String component, String message) {
        long timestamp = System.currentTimeMillis();
        
        // Prepare for WebSocket
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("type", "SYSTEM_LOG");
        logEntry.put("level", level.toUpperCase()); // INFO, WARN, ERROR, SUCCESS, ALERT
        logEntry.put("component", component);
        logEntry.put("message", message);
        logEntry.put("timestamp", timestamp);
        
        messagingTemplate.convertAndSend("/topic/monitoring", logEntry);

        // Persist to Firestore asynchronously
        try {
            MonitoringLog persistentLog = MonitoringLog.builder()
                    .level(level.toUpperCase())
                    .component(component)
                    .message(message)
                    .timestamp(timestamp)
                    .build();
            
            monitoringLogRepository.save(persistentLog).subscribe();
        } catch (Exception e) {
            logger.error("Failed to persist monitoring log: {}", e.getMessage());
        }
        
        logger.debug("[MonitoringLog] {}: {} - {}", level, component, message);
    }
}
