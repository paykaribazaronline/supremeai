package org.supremeai.sdk;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * SupremeAI Java SDK
 * Auto-generated API client for SupremeAI Platform
 *
 * Maven Dependency:
 * <dependency>
 *     <groupId>org.supremeai</groupId>
 *     <artifactId>supremeai-sdk</artifactId>
 *     <version>1.0.0</version>
 * </dependency>
 *
 * Usage:
 * SupremeAIClient client = new SupremeAIClient("your-jwt-token");
 * Map<String, Object> webhooks = client.listWebhooks();
 */
public class SupremeAIClient {
    private final String baseUrl;
    private final String token;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SupremeAIClient(String token) {
        this(token, "https://api.supremeai.example.com/api");
    }

    public SupremeAIClient(String token, String baseUrl) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("JWT token is required");
        }
        this.token = token;
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    private Map<String, Object> request(String method, String path, Object body) throws Exception {
        URI uri = URI.create(baseUrl + path);
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30));

        if ("GET".equals(method)) {
            builder.GET();
        } else if ("POST".equals(method)) {
            String bodyJson = body != null ? objectMapper.writeValueAsString(body) : "{}";
            builder.POST(HttpRequest.BodyPublishers.ofString(bodyJson));
        } else if ("DELETE".equals(method)) {
            builder.DELETE();
        }

        HttpRequest request = builder.build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }

        return objectMapper.readValue(response.body(), Map.class);
    }

    // API Information
    public Map<String, Object> getAPIInfo() throws Exception {
        return request("GET", "/", null);
    }

    public Map<String, Object> getAPIInfoV1() throws Exception {
        return request("GET", "/v1/info", null);
    }

    public Map<String, Object> getAPIInfoV2() throws Exception {
        return request("GET", "/v2/info", null);
    }

    // Webhooks
    public Map<String, Object> registerWebhook(String projectId, String url, List<String> events,
                                               String secretKey) throws Exception {
        Map<String, Object> payload = Map.of(
                "projectId", projectId,
                "url", url,
                "events", events,
                "secretKey", secretKey
        );
        return request("POST", "/v2/webhooks", payload);
    }

    public Map<String, Object> getWebhook(String webhookId) throws Exception {
        return request("GET", "/v2/webhooks/" + webhookId, null);
    }

    public Object listWebhooks() throws Exception {
        return request("GET", "/v2/webhooks", null);
    }

    public Map<String, Object> testWebhook(String webhookId, Map<String, Object> payload)
            throws Exception {
        Map<String, Object> testPayload = payload != null ? new java.util.HashMap<>(payload) : new java.util.HashMap<>();
        testPayload.put("test", true);
        return request("POST", "/v2/webhooks/" + webhookId + "/test", testPayload);
    }

    public void deleteWebhook(String webhookId) throws Exception {
        request("DELETE", "/v2/webhooks/" + webhookId, null);
    }

    // Batch Operations
    public Map<String, Object> createBatch(String name) throws Exception {
        return request("POST", "/v2/batch", Map.of("name", name));
    }

    public Map<String, Object> getBatch(String batchId) throws Exception {
        return request("GET", "/v2/batch/" + batchId, null);
    }

    public Object listBatches() throws Exception {
        return request("GET", "/v2/batch", null);
    }

    public Map<String, Object> addRequestToBatch(String batchId, Map<String, Object> request)
            throws Exception {
        return this.request("POST", "/v2/batch/" + batchId + "/requests", request);
    }

    public Map<String, Object> executeBatch(String batchId) throws Exception {
        return request("POST", "/v2/batch/" + batchId + "/execute", null);
    }

    public Map<String, Object> cancelBatch(String batchId) throws Exception {
        return request("POST", "/v2/batch/" + batchId + "/cancel", null);
    }

    public Map<String, Object> clearCompletedBatches() throws Exception {
        return request("DELETE", "/v2/batch/completed", null);
    }
}
