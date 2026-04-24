package com.supremeai.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenTelemetry configuration stub for distributed tracing.
 * To be fully implemented in Phase 4 (Observability).
 */
@Configuration
@ConditionalOnProperty(name = "tracing.enabled", havingValue = "true")
public class OpenTelemetryConfig {

    @Bean
    public String openTelemetryStub() {
        // This is a placeholder for actual OTel SDK initialization
        return "OpenTelemetry Stub Initialized";
    }
}
