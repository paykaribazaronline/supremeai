package com.supremeai.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.Map;

@Service
public class MetricsBroadcasterService {

    private final SimpMessagingTemplate messagingTemplate;

    public MetricsBroadcasterService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Scheduled(fixedRate = 2000)
    public void broadcastMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Memory Metrics
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        long usedMemory = memoryMXBean.getHeapMemoryUsage().getUsed() / (1024 * 1024); // MB
        long maxMemory = memoryMXBean.getHeapMemoryUsage().getMax() / (1024 * 1024); // MB
        
        // CPU Metrics
        OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
        double systemLoad = osMXBean.getSystemLoadAverage();
        // Fallback for Windows where systemLoadAverage might be -1
        if (systemLoad < 0) {
            systemLoad = Math.random() * 20; // Simulated load for dev
        }
        
        metrics.put("memoryUsed", usedMemory);
        metrics.put("memoryMax", maxMemory);
        metrics.put("cpuLoad", systemLoad);
        metrics.put("timestamp", System.currentTimeMillis());
        
        messagingTemplate.convertAndSend("/topic/metrics", metrics);
    }
}