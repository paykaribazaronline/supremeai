package com.supremeai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Base HTTP provider implementation
 * Eliminates 90% duplicate code across all REST API providers
 */
public abstract class AbstractHttpProvider implements AIProvider {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected static final OkHttpClient sharedHttpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    protected static final ObjectMapper sharedObjectMapper = new ObjectMapper();

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
    public String generate(String prompt) {
        try {
            Map<String, Object> body = createRequestBody(prompt);
            return executeRequest(body);
        } catch (Exception e) {
            logger.error("Generation failed: {}", e.getMessage());
            throw new RuntimeException("Generation failed", e);
        }
    }

    /**
     * Each provider implements their specific request body format
     */
    protected abstract Map<String, Object> createRequestBody(String prompt);

    /**
     * Each provider implements their specific response extraction
     */
    protected abstract String extractResponse(String responseBody);

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
                .url(baseUrl)
                .post(okhttp3.RequestBody.create(
                        sharedObjectMapper.writeValueAsString(body),
                        okhttp3.MediaType.get("application/json")
                ));

        addAuthHeaders(builder);
        addExtraHeaders(builder);

        return builder.build();
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
