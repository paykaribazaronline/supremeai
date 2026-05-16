package com.supremeai.learning;

import com.supremeai.provider.AIProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import reactor.core.publisher.Mono;

/**
 * Tracking AI Provider Wrapper
 *
 * Decorates AIProvider instances to automatically record
 * performance metrics (latency, success/failure) to the
 * self-learning router.
 */
@Component
public class TrackingAIProviderDecorator {

    private static final Logger logger = LoggerFactory.getLogger(TrackingAIProviderDecorator.class);

    private final AIProviderPerformanceTracker performanceTracker;

    public TrackingAIProviderDecorator(AIProviderPerformanceTracker performanceTracker) {
        this.performanceTracker = performanceTracker;
    }

    /**
     * Wrap an AIProvider to track its performance.
     */
    public AIProvider wrap(AIProvider delegate, String providerName) {
        return new TrackingProvider(delegate, providerName, performanceTracker);
    }

    /**
     * Internal tracking wrapper
     */
    private static class TrackingProvider implements AIProvider {
        private final AIProvider delegate;
        private final String providerName;
        private final AIProviderPerformanceTracker tracker;

        TrackingProvider(AIProvider delegate, String providerName, AIProviderPerformanceTracker tracker) {
            this.delegate = delegate;
            this.providerName = providerName;
            this.tracker = tracker;
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public Map<String, Object> getCapabilities() {
            return delegate.getCapabilities();
        }

        @Override
        public Mono<String> generate(String prompt) {
            long start = System.currentTimeMillis();
            return delegate.generate(prompt)
                .doOnSuccess(result -> {
                    boolean success = result != null && !result.isEmpty();
                    record("general_chat", prompt, success, start, 0, List.of());
                })
                .doOnError(e -> {
                    record("general_chat", prompt, false, start, 0, List.of());
                });
        }

        private void record(String taskType, String prompt, boolean success, long start, int inputLen, java.util.List<String> skills) {
            long latency = System.currentTimeMillis() - start;
            if (tracker != null) {
                try {
                    tracker.recordOutcome(taskType, providerName, success, latency, 
                        prompt != null ? prompt.length() : 0, skills);
                } catch (Exception ex) {
                    logger.warn("Tracker error: {}", ex.getMessage());
                }
            }
            if (success) {
                logger.trace("[TRACK] {} succeeded in {}ms", providerName, latency);
            } else {
                logger.debug("[TRACK] {} failed", providerName);
            }
        }
    }
}
