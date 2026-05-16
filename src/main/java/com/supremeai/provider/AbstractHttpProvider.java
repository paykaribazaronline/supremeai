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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Base HTTP provider implementation
 * Eliminates 90% duplicate code across all REST API providers
 */
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

    // SECURITY FIX: ObjectMapper is now provided via constructor injection
    // This ensures proper initialization and testability
    protected final ObjectMapper objectMapper;

    @Autowired
    protected com.supremeai.service.ProviderMetadataService providerMetadataService;

    protected final String apiKey;
    protected final String baseUrl;
    protected final String defaultModel;

    // Constructor for subclasses to call
    protected AbstractHttpProvider(String apiKey, String baseUrl, String defaultModel) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.defaultModel = defaultModel;
        // Initialize ObjectMapper with default instance
        this.objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
    }
    
    // Constructor with ObjectMapper for dependency injection
    protected AbstractHttpProvider(String apiKey, String baseUrl, String defaultModel, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.defaultModel = defaultModel;
        this.objectMapper = objectMapper != null ? objectMapper : new com.fasterxml.jackson.databind.ObjectMapper();
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
     * Each provider can override for custom response extraction.
     * Default implementation handles OpenAI-compatible format (choices[0].message.content).
     * Providers like Gemini or Anthropic that use different formats should override this.
     */
    protected String extractResponse(String responseBody) throws Exception {
        return extractOpenAICompatibleResponse(responseBody, getName());
    }

    /**
     * কমন রেসপন্স এক্সট্রাক্টর — OpenAI-compatible API ফরম্যাটের জন্য
     * (OpenAI, DeepSeek, Groq, Mistral, HuggingFace ইত্যাদি)
     * 
     * রেসপন্স ফরম্যাট: { "choices": [{ "message": { "content": "..." } }] }
     */
    protected final String extractOpenAICompatibleResponse(String responseBody, String providerName) throws Exception {
        if (responseBody == null || responseBody.isBlank()) {
            return "Empty response from " + providerName + ".";
        }

        Map<String, Object> responseMap = objectMapper.readValue(responseBody,
            new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
        if (choices != null && !choices.isEmpty()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            if (message != null) {
                Object content = message.get("content");
                if (content instanceof String text) {
                    return text;
                }
            }
        }
        return "No response from " + providerName + ".";
    }

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
        if (providerMetadataService != null) {
            return providerMetadataService.getBaseUrl(getName(), baseUrl);
        }
        return baseUrl;
    }

    /**
     * Get the default model - can be overridden or dynamically fetched
     */
    protected String getModel() {
        if (providerMetadataService != null) {
            return providerMetadataService.getDefaultModel(getName(), defaultModel);
        }
        return defaultModel;
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
        if (providerMetadataService != null) {
            com.supremeai.model.APIProvider meta = providerMetadataService.getMetadata(getName());
            if (meta != null) {
                java.util.List<String> models = meta.getModels();
                String defaultModelFromMeta = (models != null && !models.isEmpty()) ? models.get(0) : defaultModel;
                return Map.of(
                    "name", meta.getName() != null ? meta.getName() : getName(),
                    "model", defaultModelFromMeta,
                    "models", models != null ? models : List.of(defaultModel),
                    "type", "remote",
                    "url", meta.getBaseUrl() != null ? meta.getBaseUrl() : baseUrl
                );
            }
        }
        // Fallback when no metadata service: provide minimal capabilities
        return Map.of(
            "name", getName(),
            "model", defaultModel,
            "models", List.of(defaultModel),
            "type", "remote",
            "url", baseUrl
        );
    }
}
