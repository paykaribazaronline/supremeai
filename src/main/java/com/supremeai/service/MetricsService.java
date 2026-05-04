package com.supremeai.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Metrics service for real-time monitoring.
 * Integrates with Prometheus for metrics collection.
 */
@Service
public class MetricsService {

    private static final Logger log = LoggerFactory.getLogger(MetricsService.class);

    private final MeterRegistry meterRegistry;
    private final Counter requestCounter;
    private final Counter errorCounter;
    private final Counter circuitBreakerTrips;
    private final Timer requestTimer;

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        this.requestCounter = Counter.builder("supremeai.requests.total")
            .description("Total number of requests")
            .register(meterRegistry);
            
        this.errorCounter = Counter.builder("supremeai.errors.total")
            .description("Total number of errors")
            .register(meterRegistry);
            
        this.circuitBreakerTrips = Counter.builder("supremeai.circuitbreaker.trips")
            .description("Number of circuit breaker trips")
            .register(meterRegistry);
            
        this.requestTimer = Timer.builder("supremeai.request.duration")
            .description("Request duration")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);
    }

    /**
     * Record a request.
     */
    public void recordRequest(String provider, String endpoint) {
        requestCounter.increment();
        log.debug("Recorded request to {}/{}", provider, endpoint);
    }

    /**
     * Record an error.
     */
    public void recordError(String provider, String errorType) {
        errorCounter.increment();
        log.debug("Recorded error for {}: {}", provider, errorType);
    }

    /**
     * Record circuit breaker trip.
     */
    public void recordCircuitBreakerTrip(String provider) {
        circuitBreakerTrips.increment();
        log.info("Circuit breaker tripped for provider: {}", provider);
    }

    /**
     * Time a request.
     */
    public <T> T timeRequest(String provider, Supplier<T> request) {
        return requestTimer.record(request);
    }

    /**
     * Time an async request.
     */
    public <T> CompletableFuture<T> timeAsyncRequest(String provider, Supplier<CompletableFuture<T>> request) {
        return requestTimer.record(request).toCompletableFuture();
    }

    /**
     * Get current metrics snapshot.
     */
    public MetricsSnapshot getSnapshot() {
        return new MetricsSnapshot(
            (long) requestCounter.count(),
            (long) errorCounter.count(),
            (long) circuitBreakerTrips.count(),
            requestTimer.mean(TimeUnit.SECONDS)
        );
    }

    /**
     * Metrics snapshot record.
     */
    public static class MetricsSnapshot {
        public final long totalRequests;
        public final long totalErrors;
        public final long circuitBreakerTrips;
        public final double avgResponseTimeSeconds;

        public MetricsSnapshot(long totalRequests, long totalErrors, 
                               long circuitBreakerTrips, double avgResponseTimeSeconds) {
            this.totalRequests = totalRequests;
            this.totalErrors = totalErrors;
            this.circuitBreakerTrips = circuitBreakerTrips;
            this.avgResponseTimeSeconds = avgResponseTimeSeconds;
        }
    }
}