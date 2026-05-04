package com.supremeai.config;

import com.google.common.util.concurrent.RateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.*;

/**
 * Performance Optimization Configuration
 */
@Configuration
@EnableCaching
@EnableAsync
public class PerformanceConfig {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PerformanceConfig.class);

    @Value("${performance.virtual-threads.enabled:true}")
    private boolean virtualThreadsEnabled;

    @Value("${performance.async.core-pool-size:10}")
    private int asyncCorePoolSize;

    @Value("${performance.async.max-pool-size:100}")
    private int asyncMaxPoolSize;

    @Value("${performance.async.queue-capacity:1000}")
    private int asyncQueueCapacity;

    @Value("${performance.rate-limit:1000.0}")
    private double rateLimitPerSecond;

    @Value("${performance.io-timeout-seconds:30}")
    private int ioTimeoutSeconds;

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

    public static ExecutorService getVirtualThreadExecutor() {
        try {
            return (ExecutorService) Executors.class
                .getMethod("newVirtualThreadPerTaskExecutor")
                .invoke(null);
        } catch (Exception e) {
            log.warn("Virtual threads not available, falling back to bounded thread pool");
            return new ThreadPoolExecutor(
                50, 500, 60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(2000),
                new ThreadPoolExecutor.CallerRunsPolicy()
            );
        }
    }

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
        return executor;
    }

    @Bean(name = "ioTaskExecutor")
    public Executor ioTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(200);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("io-exec-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
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
