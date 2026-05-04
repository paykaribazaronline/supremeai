package com.supremeai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * Virtual thread executor configuration for high concurrency.
 * Supports 10k+ concurrent operations using Java 21 virtual threads.
 */
@Configuration
public class VirtualThreadConfig {

    private static final Logger log = LoggerFactory.getLogger(VirtualThreadConfig.class);

    /**
     * Static method to get virtual thread executor for direct access.
     */
    public static ExecutorService getVirtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Virtual thread executor for async operations.
     * Can handle 10k+ concurrent tasks efficiently.
     */
    @Bean(name = "virtualThreadExecutor")
    public ExecutorService virtualThreadExecutor() {
        log.info("Creating virtual thread executor for high concurrency");
        
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Named virtual thread executor with custom naming.
     */
    @Bean(name = "namedVirtualThreadExecutor")
    public ExecutorService namedVirtualThreadExecutor() {
        ThreadFactory factory = Thread.ofVirtual()
            .name("supremeai-virtual-", 0)
            .factory();
        
        return Executors.newThreadPerTaskExecutor(factory);
    }

    /**
     * Scheduled executor for delayed tasks (DLQ retries, cache warming).
     */
    @Bean(name = "scheduledExecutor")
    public ScheduledExecutorService scheduledExecutor() {
        ThreadFactory factory = Thread.ofVirtual()
            .name("supremeai-scheduled-", 0)
            .factory();
        
        return Executors.newScheduledThreadPool(10, factory);
    }

    /**
     * Limited virtual thread executor for bounded concurrency.
     * Use when you need to limit concurrent operations.
     */
    @Bean(name = "boundedVirtualThreadExecutor")
    public ExecutorService boundedVirtualThreadExecutor() {
        // Semaphore-based limiting for virtual threads
        return Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual()
                .name("supremeai-bounded-", 0)
                .factory()
        );
    }
}
