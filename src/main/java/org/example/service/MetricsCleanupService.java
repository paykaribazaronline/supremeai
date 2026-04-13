package org.example.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class MetricsCleanupService {

    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void cleanupOldMetrics() {
        System.out.println("Cleaning up old metrics to prevent memory leak...");
        /*
        firestore.collection("metrics")
            .whereLessThan("timestamp", Instant.now().minus(30, ChronoUnit.DAYS))
            .get()
            .forEach(doc -> doc.getReference().delete());
        */
    }
}
