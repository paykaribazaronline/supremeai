package com.supremeai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supremeai.service.ProviderMetadataService;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    /**
     * Sets the provider metadata service after construction.
     * Used by AIProviderFactory instead of reflection-based field injection.
     * Subclasses must provide this setter callable or set the field directly.
     */
    public void setProviderMetadataService(ProviderMetadataService providerMetadataService) {
        this.providerMetadataService = providerMetadataService;
    }

    protected ProviderMetadataService providerMetadataService;

    protected final ObjectMapper objectMapper;
    protected final String apiKey;
    protected final String baseUrl;
    protected final String defaultModel;

    protected AbstractHttpProvider(String apiKey, String baseUrl, String defaultModel) {
        this(apiKey, baseUrl, defaultModel, null);
    }
    
    protected AbstractHttpProvider(String apiKey, String baseUrl, String defaultModel, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.defaultModel = defaultModel;
        this.objectMapper = objectMapper != null ? objectMapper : new com.fasterxml.jackson.databind.ObjectMapper();
    }

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

    protected abstract Map<String, Object> createRequestBody(String prompt);

    protected String extractResponse(String responseBody) throws Exception {
        return extractOpenAICompatibleResponse(responseBody, getName());
    }

    protected final String extractOpenAICompatibleResponse(String responseBody, String providerName) throws Exception {
        if (responseBody == null || responseBody.isBlank()) {
            return "Empty response from " + providerName + ".";
        }

        JsonNode rootNode = objectMapper.readTree(responseBody);
        JsonNode choices = rootNode.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            JsonNode message = choices.get(0).path("message");
            JsonNode content = message.path("content");
            if (content.isTextual()) {
                return content.asText();
            }
        }
        return "No response from " + providerName + ".";
    }

    protected String executeRequest(Map<String, Object> body) throws Exception {
        okhttp3.Request request = buildRequest(body);
        logger.info("[HTTP] Calling URL: {}", request.url());

        try (okhttp3.Response response = sharedHttpClient.newCall(request).execute()) {
            logger.info("[HTTP] Response code: {} for URL: {}", response.code(), request.url());
            if (!response.isSuccessful()) {
                String errBody = response.body() != null ? response.body().string() : "(empty body)";
                logger.error("[HTTP] HTTP Error {} {} for URL: {}. Body: {}", response.code(), response.message(), request.url(), errBody);
                throw new RuntimeException("HTTP Error: " + response.code() + " " + response.message() + " for " + request.url() + ". Body: " + errBody);
            }

            String responseBody = response.body().string();
            return extractResponse(responseBody);
        }
    }

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

    protected String getRequestUrl() {
        if (providerMetadataService != null) {
            return providerMetadataService.getBaseUrl(getName(), baseUrl);
        }
        return baseUrl;
    }

    protected String getModel() {
        if (providerMetadataService != null) {
            return providerMetadataService.getDefaultModel(getName(), defaultModel);
        }
        return defaultModel;
    }

    protected abstract void addAuthHeaders(okhttp3.Request.Builder builder);

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
        return Map.of(
            "name", getName(),
            "model", defaultModel,
            "models", List.of(defaultModel),
            "type", "remote",
            "url", baseUrl
        );
    }
}
