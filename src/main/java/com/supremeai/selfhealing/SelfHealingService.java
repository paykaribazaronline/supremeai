package com.supremeai.selfhealing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Map;

@Service("selfHealingService-selfhealing")
public class SelfHealingService {

    private static final Logger logger = LoggerFactory.getLogger(SelfHealingService.class);

    // Healing strategy functional interface
    @FunctionalInterface
    public interface HealingStrategy {
        boolean heal(Exception error);
    }

    // Healing strategies map
    private final Map<String, HealingStrategy> healingStrategies = new ConcurrentHashMap<>();

    // Healing event log
    private final List<String> healingEventLog = new CopyOnWriteArrayList<>();

    public <T> T executeWithRetry(Callable<T> task, int maxAttempts, long timeoutMs) throws Exception {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return task.call();
            } catch (Exception e) {
                lastException = e;
                logger.warn("Attempt {} failed: {}", attempt, e.getMessage());

                if (attempt < maxAttempts) {
                    TimeUnit.MILLISECONDS.sleep(timeoutMs);
                }
            }
        }

        throw lastException;
    }

    public void runWithRetry(Runnable task, int maxAttempts, long timeoutMs) throws Exception {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                task.run();
                return;
            } catch (Exception e) {
                lastException = e;
                logger.warn("Attempt {} failed: {}", attempt, e.getMessage());

                if (attempt < maxAttempts) {
                    TimeUnit.MILLISECONDS.sleep(timeoutMs);
                }
            }
        }

        throw lastException;
    }

    public void registerHealingStrategy(String errorType, HealingStrategy strategy) {
        healingStrategies.put(errorType, strategy);
        logger.info("Registered healing strategy for error type: {}", errorType);
    }

    public boolean applyHealingStrategy(String errorType, Exception error) {
        HealingStrategy strategy = healingStrategies.get(errorType);
        if (strategy != null) {
            try {
                boolean result = strategy.heal(error);
                healingEventLog.add("Applied strategy for " + errorType + ": " + result);
                return result;
            } catch (Exception e) {
                logger.error("Failed to apply healing strategy for {}: {}", errorType, e.getMessage());
                return false;
            }
        }
        return false;
    }
}
