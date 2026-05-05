package com.supremeai.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SLO (Service Level Objective) tracking service.
 * Tracks p95, p99 latency and error rates.
 */
@Service
public class SLOTrackingService {

    private static final Logger log = LoggerFactory.getLogger(SLOTrackingService.class);

    private final Timer requestTimer;
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong errorRequests = new AtomicLong(0);

    // SLO targets
    private static final double P95_TARGET_MS = 500.0;
    private static final double P99_TARGET_MS = 1000.0;
    private static final double ERROR_RATE_TARGET = 0.01; // 1%

    public SLOTrackingService(MeterRegistry meterRegistry) {
        this.requestTimer = Timer.builder("supremeai.slo.request.duration")
            .description("SLO request duration")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);
    }

    /**
     * Record a request for SLO tracking.
     */
    public void recordRequest(boolean success, Duration duration) {
        totalRequests.incrementAndGet();
        if (!success) {
            errorRequests.incrementAndGet();
        }
        requestTimer.record(duration);
    }

    /**
     * Get SLO status.
     */
    public SLOStatus getStatus() {
        double p95 = requestTimer.percentile(0.95, java.util.concurrent.TimeUnit.MILLISECONDS);
        double p99 = requestTimer.percentile(0.99, java.util.concurrent.TimeUnit.MILLISECONDS);
        double errorRate = (double) errorRequests.get() / totalRequests.get();

        return new SLOStatus(
            p95,
            p99,
            errorRate,
            p95 <= P95_TARGET_MS,
            p99 <= P99_TARGET_MS,
            errorRate <= ERROR_RATE_TARGET
        );
    }

    /**
     * Check if SLOs are being met.
     */
    public boolean isMeetingSLOs() {
        SLOStatus status = getStatus();
        return status.p95Met && status.p99Met && status.errorRateMet;
    }

    /**
     * SLO status record.
     */
    public static class SLOStatus {
        public final double p95LatencyMs;
        public final double p99LatencyMs;
        public final double errorRate;
        public final boolean p95Met;
        public final boolean p99Met;
        public final boolean errorRateMet;

        public SLOStatus(double p95LatencyMs, double p99LatencyMs, double errorRate,
                         boolean p95Met, boolean p99Met, boolean errorRateMet) {
            this.p95LatencyMs = p95LatencyMs;
            this.p99LatencyMs = p99LatencyMs;
            this.errorRate = errorRate;
            this.p95Met = p95Met;
            this.p99Met = p99Met;
            this.errorRateMet = errorRateMet;
        }
    }
}