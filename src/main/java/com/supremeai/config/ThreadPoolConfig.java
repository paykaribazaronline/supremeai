package com.supremeai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ThreadPoolConfig {
    @Bean(destroyMethod = "shutdown")
    public ExecutorService aiProviderExecutor() {
        return Executors.newFixedThreadPool(10);
    }

    @Bean(destroyMethod = "shutdown")
    @Qualifier("consensusTaskExecutor")
    public ExecutorService consensusTaskExecutor() {
        return Executors.newFixedThreadPool(5);
    }
}
