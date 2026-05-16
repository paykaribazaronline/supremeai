package com.supremeai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import java.time.Duration;

@Configuration
public class WebClientConfig {

    /**
     * Shared WebClient for simulator API proxy.
     * Configured with connection pooling for high-concurrency scenarios.
     */
    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        ConnectionProvider provider = ConnectionProvider.builder("simulator-proxy")
                .maxConnections(500)          // Total max connections
                .maxIdleTime(Duration.ofSeconds(30))
                .maxLifeTime(Duration.ofMinutes(2))
                .pendingAcquireMaxCount(1000)  // Max pending requests
                .pendingAcquireTimeout(Duration.ofSeconds(60))
                .build();

        HttpClient httpClient = HttpClient.create(provider);

        return builder
                .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(httpClient))
                .build();
    }
}
