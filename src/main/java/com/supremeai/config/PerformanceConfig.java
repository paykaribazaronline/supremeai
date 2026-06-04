package com.supremeai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.google.common.util.concurrent.RateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.*;

/**
 * Performance Optimization Configuration
 * Optimized for high-concurrency AI workloads with virtual threads
 */
@Configuration
@EnableCaching
@EnableAsync
public class PerformanceConfig {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PerformanceConfig.class);

    @Value("${performance.virtual-threads.enabled:true}")
    private boolean virtualThreadsEnabled;

    @Value("${performance.async.core-pool-size:50}")
    private int asyncCorePoolSize;

    @Value("${performance.async.max-pool-size:500}")
    private int asyncMaxPoolSize;

    @Value("${performance.async.queue-capacity:5000}")
    private int asyncQueueCapacity;

    @Value("${performance.rate-limit:5000.0}")
    private double rateLimitPerSecond;

    @Value("${performance.io-timeout-seconds:30}")
    private int ioTimeoutSeconds;

    /**
     * Shared ObjectMapper with Afterburner for 20-30% faster JSON serialization.
     * This replaces all individual ObjectMapper instances across providers.
     * Afterburner module generates bytecode for faster serialization/deserialization.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        log.info("Configuring shared ObjectMapper with Afterburner for optimal JSON performance");
        return JsonMapper.builder()
                .addModule(new AfterburnerModule())
                .addModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true)
                .build();
    }

    @Bean
    public AsyncTaskExecutor applicationTaskExecutor() {
        return new TaskExecutorAdapter(getVirtualThreadExecutor());
    }

    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
        return protocolHandler -> {
            protocolHandler.setExecutor(getVirtualThreadExecutor());
        };
    }

    /**
     * Virtual thread executor for handling high-concurrency requests efficiently.
     * Provides 100x better concurrency compared to traditional thread pools.
     */
    public static ExecutorService getVirtualThreadExecutor() {
        try {
            return (ExecutorService) Executors.class
                .getMethod("newVirtualThreadPerTaskExecutor")
                .invoke(null);
        } catch (Exception e) {
            log.warn("Virtual threads not available, falling back to bounded thread pool");
            return new ThreadPoolExecutor(
                200, 1000, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(5000),
                new ThreadPoolExecutor.CallerRunsPolicy()
            );
        }
    }

    /**
     * Async task executor for background processing.
     * Optimized for CPU-intensive AI tasks.
     */
    @Bean(name = "asyncTaskExecutor")
    public Executor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(asyncCorePoolSize);
        executor.setMaxPoolSize(asyncMaxPoolSize);
        executor.setQueueCapacity(asyncQueueCapacity);
        executor.setThreadNamePrefix("async-exec-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("Async task executor configured: core={}, max={}, queue={}", 
            asyncCorePoolSize, asyncMaxPoolSize, asyncQueueCapacity);
        return executor;
    }

    /**
     * IO-bound task executor for network operations.
     * Separate pool for blocking IO operations to avoid starving CPU threads.
     */
    @Bean(name = "ioTaskExecutor")
    public Executor ioTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(50);
        executor.setMaxPoolSize(300);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("io-exec-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        log.info("IO task executor configured: core=50, max=300, queue=1000");
        return executor;
    }

    /**
     * CPU-bound task executor for computation-heavy operations.
     * Sized based on available processors.
     */
    @Bean(name = "cpuTaskExecutor")
    public Executor cpuTaskExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(processors);
        executor.setMaxPoolSize(processors * 2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("cpu-exec-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        log.info("CPU task executor configured: processors={}", processors);
        return executor;
    }

    @Bean
    public RateLimiter rateLimiter() {
        log.info("Rate limiter configured: {} requests/second", rateLimitPerSecond);
        return RateLimiter.create(rateLimitPerSecond);
    }

    @Bean(name = "strictRateLimiter")
    public RateLimiter strictRateLimiter() {
        return RateLimiter.create(rateLimitPerSecond / 10);
    }

    @Bean
    public int defaultTimeoutSeconds() {
        return ioTimeoutSeconds;
    }

    @Bean
    public CircuitBreakerConfig circuitBreakerConfig() {
        return new CircuitBreakerConfig();
    }

    public static class CircuitBreakerConfig {
        private int failureThreshold = 5;
        private int successThreshold = 2;
        private int waitDuration = 30; // seconds
        private int permittedCallsInHalfOpenState = 3;

        public int getFailureThreshold() { return failureThreshold; }
        public int getSuccessThreshold() { return successThreshold; }
        public int getWaitDuration() { return waitDuration; }
        public int getPermittedCallsInHalfOpenState() { return permittedCallsInHalfOpenState; }
    }
}
