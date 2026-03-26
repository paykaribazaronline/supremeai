package org.example.service;

import okhttp3.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.*;

public class AIAPIService {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final OkHttpClient client = new OkHttpClient();
    
    // API endpoints and keys (should be moved to config file)
    private final Map<String, String> apiEndpoints = Map.ofEntries(
        Map.entry("DEEPSEEK", "https://api.deepseek.com/v1/chat/completions"),
        Map.entry("GROQ", "https://api.groq.com/openai/v1/chat/completions"),
        Map.entry("CLAUDE", "https://api.anthropic.com/v1/messages"),
        Map.entry("GPT4", "https://api.openai.com/v1/chat/completions")
    );
    
    private final Map<String, String> apiKeys = new HashMap<>();
    
    public AIAPIService(Map<String, String> keys) {
        this.apiKeys.putAll(keys);
    }
    
    /**
     * Call AI agent with fallback chain support
     * @param role BUILDER, REVIEWER, or ARCHITECT
     * @param prompt The task/prompt
     * @param fallbackChain List of AI models to try in order
     * @return AI response or null if all fail
     */
    public String callAI(String role, String prompt, List<String> fallbackChain) {
        for (String aiModel : fallbackChain) {
            try {
                String response = executeAPICall(aiModel, prompt);
                if (response != null) {
                    return response;
                }
            } catch (Exception e) {
                System.err.println("Failed with " + aiModel + ": " + e.getMessage());
                continue;
            }
        }
        return null; // All fallbacks failed
    }
    
    private String executeAPICall(String aiModel, String prompt) throws IOException {
        String endpoint = apiEndpoints.get(aiModel);
        String apiKey = apiKeys.get(aiModel);
        
        if (endpoint == null || apiKey == null) {
            throw new IllegalArgumentException("Unknown AI model or missing API key: " + aiModel);
        }
        
        switch (aiModel) {
            case "DEEPSEEK":
                return callDeepSeek(endpoint, apiKey, prompt);
            case "GROQ":
                return callGroq(endpoint, apiKey, prompt);
            case "CLAUDE":
                return callClaude(endpoint, apiKey, prompt);
            case "GPT4":
                return callGPT4(endpoint, apiKey, prompt);
            default:
                throw new IllegalArgumentException("Unknown AI model: " + aiModel);
        }
    }
    
    private String callDeepSeek(String endpoint, String apiKey, String prompt) throws IOException {
        var root = mapper.createObjectNode();
        root.put("model", "deepseek-coder");
        var messages = root.putArray("messages");
        var msg = mapper.createObjectNode();
        msg.put("role", "user");
        msg.put("content", prompt);
        messages.add(msg);
        root.put("temperature", 0.7);
        
        String jsonBody = root.toString();
        return makeRequest(endpoint, apiKey, jsonBody);
    }
    
    private String callGroq(String endpoint, String apiKey, String prompt) throws IOException {
        var root = mapper.createObjectNode();
        root.put("model", "mixtral-8x7b-32768");
        var messages = root.putArray("messages");
        var msg = mapper.createObjectNode();
        msg.put("role", "user");
        msg.put("content", prompt);
        messages.add(msg);
        root.put("max_tokens", 2000);
        
        String jsonBody = root.toString();
        return makeRequest(endpoint, apiKey, jsonBody);
    }
    
    private String callClaude(String endpoint, String apiKey, String prompt) throws IOException {
        String jsonBody = mapper.createObjectNode()
                .put("model", "claude-3-sonnet-20240229")
                .put("max_tokens", 2048)
                .putArray("messages")
                    .add(mapper.createObjectNode()
                        .put("role", "user")
                        .put("content", prompt))
                .toString();
        
        Request request = new Request.Builder()
                .url(endpoint)
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonNode json = mapper.readTree(response.body().string());
                return json.path("content").get(0).path("text").asText();
            }
        }
        return null;
    }
    
    private String callGPT4(String endpoint, String apiKey, String prompt) throws IOException {
        var root = mapper.createObjectNode();
        root.put("model", "gpt-4");
        root.put("temperature", 0.7);
        var messages = root.putArray("messages");
        var msg = mapper.createObjectNode();
        msg.put("role", "user");
        msg.put("content", prompt);
        messages.add(msg);
        root.put("max_tokens", 2000);
        
        String jsonBody = root.toString();
        return makeRequest(endpoint, apiKey, jsonBody);
    }
    
    private String makeRequest(String endpoint, String apiKey, String jsonBody) throws IOException {
        Request request = new Request.Builder()
                .url(endpoint)
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                JsonNode json = mapper.readTree(responseBody);
                
                // Extract from different response formats
                if (json.has("choices")) {
                    return json.path("choices").get(0).path("message").path("content").asText();
                } else if (json.has("content")) {
                    return json.path("content").get(0).path("text").asText();
                }
            } else if (response.code() == 429) {
                throw new IOException("Rate limited (429) - trigger rotation");
            } else if (response.code() == 403) {
                throw new IOException("Forbidden/Quota exceeded (403) - trigger rotation");
            }
        }
        return null;
    }
    
    /**
     * Count tokens in prompt (rough estimate)
     * Real implementation would use model-specific tokenizers
     */
    public int estimateTokens(String text) {
        // Rough rule: 1 token ≈ 4 characters
        return (text.length() / 4) + 1;
    }
    
    /**
     * Check quota remaining for an AI model
     */
    public int getQuotaRemaining(String aiModel) {
        // This would fetch from real quota tracking service
        // For now, return simulated value
        return 1000; // Simulated
    }
}
