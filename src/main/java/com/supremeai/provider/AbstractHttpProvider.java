package com.supremeai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Base HTTP provider implementation
 * Eliminates 90% duplicate code across all REST API providers
 */
@Component
public abstract class AbstractHttpProvider implements AIProvider {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Optimized OkHttpClient with connection pooling and HTTP/2 support.
     * 
     * Features:
     * - Connection pooling (max 100 connections)
     * - HTTP/2 support for multiplexed requests
     * - Connection keep-alive
     * - Retry on connection failure
     */
    protected static final OkHttpClient sharedHttpClient = new OkHttpClient.Builder()
            .connectionPool(new okhttp3.ConnectionPool(100, 5, TimeUnit.MINUTES))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    @Autowired
    protected ObjectMapper objectMapper;

    protected final String apiKey;
    protected final String baseUrl;
    protected final String defaultModel;

    protected AbstractHttpProvider(String apiKey, String baseUrl, String defaultModel) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.defaultModel = defaultModel;
    }

    /**
     * Common implementation for all HTTP based providers
     */
    @Override
    public Mono<String> generate(String prompt) {
        return Mono.fromCallable(() -> {
            try {
                Map<String, Object> body = createRequestBody(prompt);
                return executeRequest(body);
            } catch (Exception e) {
                logger.error("Generation failed: {}", e.getMessage());
                throw new RuntimeException("Generation failed", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Each provider implements their specific request body format
     */
    protected abstract Map<String, Object> createRequestBody(String prompt);

    /**
     * Each provider implements their specific response extraction
     */
    protected abstract String extractResponse(String responseBody) throws Exception;

    /**
     * Common HTTP request execution
     */
    protected String executeRequest(Map<String, Object> body) throws Exception {
        okhttp3.Request request = buildRequest(body);

        try (okhttp3.Response response = sharedHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("HTTP Error: " + response.code() + " " + response.message());
            }

            String responseBody = response.body().string();
            return extractResponse(responseBody);
        }
    }

    /**
     * Common request builder
     */
    protected okhttp3.Request buildRequest(Map<String, Object> body) throws Exception {
        okhttp3.Request.Builder builder = new okhttp3.Request.Builder()
                .url(getRequestUrl())
                .post(okhttp3.RequestBody.create(
                        objectMapper.writeValueAsString(body),
                        okhttp3.MediaType.get("application/json")
                ));

        addAuthHeaders(builder);
        addExtraHeaders(builder);

        return builder.build();
    }

    /**
     * Get the request URL - can be overridden by providers that need custom URL building
     */
    protected String getRequestUrl() {
        return baseUrl;
    }

    /**
     * Add authentication headers
     */
    protected abstract void addAuthHeaders(okhttp3.Request.Builder builder);

    /**
     * Override to add provider specific headers
     */
    protected void addExtraHeaders(okhttp3.Request.Builder builder) {
        // Default no extra headers
    }

    @Override
    public Map<String, Object> getCapabilities() {
        return Map.of(
                "model", defaultModel,
                "type", "remote",
                "url", baseUrl
        );
    }
}
