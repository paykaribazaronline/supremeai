package org.example.monitoring;

import io.micrometer.core.instrument.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Production metrics and monitoring using Micrometer
 * Track performance, errors, and business metrics
 */
public class MetricsService {
    private static final Logger logger = LoggerFactory.getLogger(MetricsService.class);
    private final MeterRegistry meterRegistry;
    
    // Counters
    private final Counter orchestrationSuccess;
    private final Counter orchestrationFailure;
    private final Counter apiCallSuccess;
    private final Counter apiCallFailure;
    private final Counter requirementProcessed;
    private final Counter approvalGranted;
    private final Counter approvalDenied;
    
    // Timers
    private final io.micrometer.core.instrument.Timer orchestrationDuration;
    private final io.micrometer.core.instrument.Timer apiCallDuration;
    private final io.micrometer.core.instrument.Timer requirementProcessingDuration;
    
    // Gauges
    private final AtomicInteger activeOrchestrations;
    private final Map<String, Integer> apiQuotaRemaining;
    
    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize counters
        this.orchestrationSuccess = Counter.builder("orchestration.success")
                .description("Number of successful orchestrations")
                .register(meterRegistry);
        
        this.orchestrationFailure = Counter.builder("orchestration.failure")
                .description("Number of failed orchestrations")
                .register(meterRegistry);
        
        this.apiCallSuccess = Counter.builder("api.call.success")
                .description("Number of successful API calls")
                .register(meterRegistry);
        
        this.apiCallFailure = Counter.builder("api.call.failure")
                .description("Number of failed API calls")
                .register(meterRegistry);
        
        this.requirementProcessed = Counter.builder("requirement.processed")
                .description("Number of requirements processed")
                .register(meterRegistry);
        
        this.approvalGranted = Counter.builder("approval.granted")
                .description("Number of approvals granted")
                .register(meterRegistry);
        
        this.approvalDenied = Counter.builder("approval.denied")
                .description("Number of approvals denied")
                .register(meterRegistry);
        
        // Initialize timers
        this.orchestrationDuration = io.micrometer.core.instrument.Timer.builder("orchestration.duration")
                .description("Time to complete orchestration")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
        
        this.apiCallDuration = io.micrometer.core.instrument.Timer.builder("api.call.duration")
                .description("Time for API calls to complete")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
        
        this.requirementProcessingDuration = io.micrometer.core.instrument.Timer.builder("requirement.processing.duration")
                .description("Time to process requirements")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
        
        // Initialize gauges
        this.activeOrchestrations = new AtomicInteger(0);
        Gauge.builder("orchestration.active", activeOrchestrations, AtomicInteger::get)
                .description("Number of active orchestrations")
                .register(meterRegistry);
        
        this.apiQuotaRemaining = new ConcurrentHashMap<>();
        
        logger.info("MetricsService initialized");
    }
    
    /**
     * Record successful orchestration
     */
    public io.micrometer.core.instrument.Timer.Sample recordOrchestrationStart() {
        activeOrchestrations.incrementAndGet();
        return io.micrometer.core.instrument.Timer.start(meterRegistry);
    }
    
    public void recordOrchestrationSuccess(io.micrometer.core.instrument.Timer.Sample sample) {
        sample.stop(orchestrationDuration);
        orchestrationSuccess.increment();
        activeOrchestrations.decrementAndGet();
    }
    
    public void recordOrchestrationFailure(io.micrometer.core.instrument.Timer.Sample sample) {
        sample.stop(orchestrationDuration);
        orchestrationFailure.increment();
        activeOrchestrations.decrementAndGet();
    }
    
    /**
     * Record API call metrics
     */
    public io.micrometer.core.instrument.Timer.Sample recordAPICallStart(String apiName) {
        return io.micrometer.core.instrument.Timer.start(meterRegistry);
    }
    
    public void recordAPICallSuccess(io.micrometer.core.instrument.Timer.Sample sample, String apiName) {
        sample.stop(io.micrometer.core.instrument.Timer.builder("api.call.duration")
                .tag("api", apiName)
                .tag("status", "success")
                .register(meterRegistry));
        apiCallSuccess.increment();
    }
    
    public void recordAPICallFailure(io.micrometer.core.instrument.Timer.Sample sample, String apiName, String reason) {
        sample.stop(io.micrometer.core.instrument.Timer.builder("api.call.duration")
                .tag("api", apiName)
                .tag("status", "failure")
                .tag("reason", reason)
                .register(meterRegistry));
        apiCallFailure.increment();
    }
    
    /**
     * Record requirement processing
     */
    public io.micrometer.core.instrument.Timer.Sample recordRequirementProcessingStart(String size) {
        return io.micrometer.core.instrument.Timer.start(meterRegistry);
    }
    
    public void recordRequirementProcessingComplete(io.micrometer.core.instrument.Timer.Sample sample, String size) {
        sample.stop(io.micrometer.core.instrument.Timer.builder("requirement.processing.duration")
                .tag("size", size)
                .register(meterRegistry));
        requirementProcessed.increment();
    }
    
    /**
     * Record approvals
     */
    public void recordApprovalGranted(String size) {
        approvalGranted.increment();
        meterRegistry.counter("approval.granted", "size", size).increment();
    }
    
    public void recordApprovalDenied(String size, String reason) {
        approvalDenied.increment();
        meterRegistry.counter("approval.denied", "size", size, "reason", reason).increment();
    }
    
    /**
     * Record API quota usage
     */
    public void recordAPIQuota(String apiName, int remaining) {
        apiQuotaRemaining.put(apiName, remaining);
        Gauge.builder("api.quota.remaining", 
                () -> apiQuotaRemaining.getOrDefault(apiName, 0))
                .tag("api", apiName)
                .register(meterRegistry);
    }
    
    /**
     * Get all metrics summary
     */
    public Map<String, Object> getMetricsSummary() {
        return Map.ofEntries(
            Map.entry("orchestration_success", orchestrationSuccess.count()),
            Map.entry("orchestration_failure", orchestrationFailure.count()),
            Map.entry("orchestration_active", activeOrchestrations.get()),
            Map.entry("api_call_success", apiCallSuccess.count()),
            Map.entry("api_call_failure", apiCallFailure.count()),
            Map.entry("requirement_processed", requirementProcessed.count()),
            Map.entry("approval_granted", approvalGranted.count()),
            Map.entry("approval_denied", approvalDenied.count()),
            Map.entry("api_quotas", new HashMap<>(apiQuotaRemaining))
        );
    }
}
