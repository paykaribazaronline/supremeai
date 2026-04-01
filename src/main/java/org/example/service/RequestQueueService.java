package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.concurrent.*;
import java.util.*;

/**
 * Request Queue Service
 * Prevents race conditions by sequencing critical operations
 */
@Service
public class RequestQueueService {
    private static final Logger logger = LoggerFactory.getLogger(RequestQueueService.class);
    
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Map<String, Object> locks = new ConcurrentHashMap<>();
    
    /**
     * Execute git operation sequentially (prevent race conditions)
     */
    public <T> T executeGitOperation(String operationType, Callable<T> operation) throws Exception {
        // Get lock for this operation type
        Object lock = locks.computeIfAbsent("GIT_" + operationType, k -> new Object());
        
        synchronized(lock) {
            logger.info("🔒 Acquiring lock for: {}", operationType);
            try {
                T result = operation.call();
                logger.info("✅ Completed {}", operationType);
                return result;
            } catch (Exception e) {
                logger.error("❌ Failed {}: {}", operationType, e.getMessage());
                throw e;
            }
        }
    }
    
    /**
     * Queue extension request (prevent duplicate code generation)
     */
    public Future<?> queueExtensionRequest(String requirementId, Runnable task) {
        // Get lock for this requirement
        Object lock = locks.computeIfAbsent("EXT_" + requirementId, k -> new Object());
        
        synchronized(lock) {
            logger.info("📋 Queued extension: {}", requirementId);
            return executor.submit(() -> {
                synchronized(lock) {
                    task.run();
                }
            });
        }
    }
    
    /**
     * Check if operation already running
     */
    public boolean isOperationRunning(String operationType) {
        return locks.containsKey(operationType);
    }
    
    /**
     * Wait for all queued operations to complete
     */
    public void waitForAll(long timeoutSeconds) throws InterruptedException {
        executor.shutdown();
        boolean completed = executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS);
        if (!completed) {
            logger.warn("⏱️ Some operations timed out");
            executor.shutdownNow();
        }
    }
}
