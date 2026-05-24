package com.supremeai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import jakarta.annotation.PostConstruct;

/**
 * Provider-specific timeout configuration.
 * Timeout categories (provider type names) are sourced from application.yml
 * under {@code provider.timeouts.{connect,read,write}}.
 *
 * Per-provider timeouts must be configured there — no brand names are hardcoded here.
 */
@Configuration
@ConfigurationProperties(prefix = "provider.timeouts")
public class TimeoutConfig {

    private Map<String, Duration> connect;
    private Map<String, Duration> read;
    private Map<String, Duration> write;

    @PostConstruct
    public void init() {
        // Seed only neutral/category-level fallbacks if no YAML config is present
        if (connect == null || connect.isEmpty()) {
            this.connect = Map.of("default", Duration.ofSeconds(10));
        }
        if (read == null || read.isEmpty()) {
            this.read = Map.of("default", Duration.ofSeconds(30));
        }
        if (write == null || write.isEmpty()) {
            this.write = Map.of("default", Duration.ofSeconds(10));
        }
    }

    public Duration getConnectTimeout(String provider) {
        return connect.getOrDefault(provider.toLowerCase(), Duration.ofSeconds(10));
    }

    public Duration getReadTimeout(String provider) {
        return read.getOrDefault(provider.toLowerCase(), Duration.ofSeconds(30));
    }

    public Duration getWriteTimeout(String provider) {
        return write.getOrDefault(provider.toLowerCase(), Duration.ofSeconds(10));
    }

    // Getters and setters for @ConfigurationProperties
    public Map<String, Duration> getConnect() { return connect; }
    public void setConnect(Map<String, Duration> connect) { this.connect = connect; }

    public Map<String, Duration> getRead() { return read; }
    public void setRead(Map<String, Duration> read) { this.read = read; }

    public Map<String, Duration> getWrite() { return write; }
    public void setWrite(Map<String, Duration> write) { this.write = write; }
}
