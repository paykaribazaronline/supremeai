package com.supremeai.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class HybridDataCollector {

    public static class CollectorHealth {
        public String status;
        public double successRate;
        public Metrics metrics;
    }

    public static class Metrics {
        public int apiSuccess;
        public int apiFailed;
    }

    public CollectorHealth getHealth() {
        CollectorHealth health = new CollectorHealth();
        health.status = "HEALTHY";
        health.successRate = 1.0;
        Metrics metrics = new Metrics();
        metrics.apiSuccess = 100;
        metrics.apiFailed = 0;
        health.metrics = metrics;
        return health;
    }
}