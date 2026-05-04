package com.supremeai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

/**
 * Bulkhead pattern configuration for AI provider isolation.
 * Each provider gets its own thread pool to prevent cascade failures.
 */
@Configuration
public class BulkheadConfig {

    /**
     * Thread pool for OpenAI provider
     * Isolated to prevent OpenAI issues from affecting other providers
     */
    @Bean(name = "openaiExecutor")
    public ExecutorService openaiExecutor() {
        return new ThreadPoolExecutor(
            10,   // core pool size
            50,   // max pool size
            60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100),
            new ThreadFactory() {
                private final ThreadFactory delegate = Executors.defaultThreadFactory();
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = delegate.newThread(r);
                    t.setName("openai-executor-" + t.getName());
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * Thread pool for Groq provider
     * Fast provider with lower timeout requirements
     */
    @Bean(name = "groqExecutor")
    public ExecutorService groqExecutor() {
        return new ThreadPoolExecutor(
            10,   // core pool size
            50,   // max pool size
            60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100),
            new ThreadFactory() {
                private final ThreadFactory delegate = Executors.defaultThreadFactory();
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = delegate.newThread(r);
                    t.setName("groq-executor-" + t.getName());
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * Thread pool for local/Ollama provider
     * Higher resource requirements, separate pool
     */
    @Bean(name = "localExecutor")
    public ExecutorService localExecutor() {
        return new ThreadPoolExecutor(
            5,    // core pool size (fewer, more resource intensive)
            20,   // max pool size
            60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(50),
            new ThreadFactory() {
                private final ThreadFactory delegate = Executors.defaultThreadFactory();
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = delegate.newThread(r);
                    t.setName("local-executor-" + t.getName());
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * Thread pool for Anthropic/Claude provider
     */
    @Bean(name = "anthropicExecutor")
    public ExecutorService anthropicExecutor() {
        return new ThreadPoolExecutor(
            10,
            50,
            60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(100),
            new ThreadFactory() {
                private final ThreadFactory delegate = Executors.defaultThreadFactory();
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = delegate.newThread(r);
                    t.setName("anthropic-executor-" + t.getName());
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}