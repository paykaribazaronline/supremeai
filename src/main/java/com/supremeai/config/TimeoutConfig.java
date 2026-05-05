package com.supremeai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;

/**
 * Provider-specific timeout configuration.
 * Implements timeout cascades for different AI providers.
 * 
 * Groq: 5s (fastest provider)
 * OpenAI: 30s (standard)
 * Local/Ollama: 60s (resource intensive)
 * Anthropic: 30s
 */
@Configuration
@ConfigurationProperties(prefix = "provider.timeouts")
public class TimeoutConfig {

    private Map<String, Duration> connect;
    private Map<String, Duration> read;
    private Map<String, Duration> write;

    public TimeoutConfig() {
        // Default timeouts
        this.connect = Map.of(
            "groq", Duration.ofSeconds(5),
            "openai", Duration.ofSeconds(10),
            "local", Duration.ofSeconds(15),
            "anthropic", Duration.ofSeconds(10)
        );
        this.read = Map.of(
            "groq", Duration.ofSeconds(5),
            "openai", Duration.ofSeconds(30),
            "local", Duration.ofSeconds(60),
            "anthropic", Duration.ofSeconds(30)
        );
        this.write = Map.of(
            "groq", Duration.ofSeconds(5),
            "openai", Duration.ofSeconds(10),
            "local", Duration.ofSeconds(15),
            "anthropic", Duration.ofSeconds(10)
        );
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

    // Getters and setters
    public Map<String, Duration> getConnect() { return connect; }
    public void setConnect(Map<String, Duration> connect) { this.connect = connect; }
    
    public Map<String, Duration> getRead() { return read; }
    public void setRead(Map<String, Duration> read) { this.read = read; }
    
    public Map<String, Duration> getWrite() { return write; }
    public void setWrite(Map<String, Duration> write) { this.write = write; }
}