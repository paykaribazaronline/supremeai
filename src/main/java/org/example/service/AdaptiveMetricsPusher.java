package org.example.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AdaptiveMetricsPusher {
    
    private int intervalMs = 2000; // Start with 2s
    
    @Scheduled(fixedDelayString = "${ai.metrics.push.interval:2000}")
    public void pushMetrics() {
        long latency = measureLatency();
        if (latency > 1000) {
            intervalMs = Math.min(intervalMs + 1000, 10000); // Max 10s to prevent WebSocket connection drops
        }
        // System.out.println("Pushing metrics with interval: " + intervalMs);
        // Push logic
    }
    
    private long measureLatency() {
        // Return simulated latency for now
        return 500;
    }
}
