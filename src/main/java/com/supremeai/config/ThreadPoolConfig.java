package com.supremeai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Thread pool configuration.
 * Optimized for Java 21 Virtual Threads to ensure maximum concurrency and zero bottleneck.
 */
@Configuration
public class ThreadPoolConfig {
    
    @Bean(destroyMethod = "shutdown")
    @Qualifier("aiProviderExecutor")
    public ExecutorService aiProviderExecutor() {
        // High-concurrency executor for AI provider requests
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean(destroyMethod = "shutdown")
    @Qualifier("consensusTaskExecutor")
    public ExecutorService consensusTaskExecutor() {
        // High-concurrency executor for multi-agent coordination
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean(destroyMethod = "shutdown")
    @org.springframework.context.annotation.Primary
    public java.util.concurrent.ScheduledExecutorService scheduledExecutorService() {
        return java.util.concurrent.Executors.newScheduledThreadPool(2);
    }
}
