package com.supremeai.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import java.util.Properties;

/**
 * JVM Options configuration for optimal performance.
 * Recommended JVM arguments for production:
 * 
 * Heap Size: -Xms4g -Xmx8g
 * GC Tuning: -XX:+UseZGC or -XX:+UseG1GC
 * Virtual Threads: -XX:+UnlockExperimentalVMOptions (Java 19-20)
 * 
 * Example:
 * java -Xms4g -Xmx8g -XX:+UseZGC -XX:+UnlockExperimentalVMOptions -jar app.jar
 */
public class JVMOptionsConfig implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Properties props = new Properties();
        
        // Default JVM optimization properties
        props.setProperty("jvm.heap.initial", "4g");
        props.setProperty("jvm.heap.max", "8g");
        props.setProperty("jvm.gc.type", "ZGC");
        props.setProperty("jvm.gc.pause.target", "10");
        
        // Virtual thread settings
        props.setProperty("jvm.virtual-threads.enabled", "true");
        props.setProperty("jvm.virtual-threads.parallelism", "10000");
        
        // JIT compiler optimizations
        props.setProperty("jvm.jit.compile.threshold", "10000");
        props.setProperty("jvm.code.cache.size", "512m");
        
        environment.getPropertySources().addLast(
            new PropertiesPropertySource("jvmOptions", props)
        );
    }
}