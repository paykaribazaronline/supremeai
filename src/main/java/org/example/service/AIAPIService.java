package org.example.service;

import okhttp3.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.*;

public class AIAPIService {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final OkHttpClient client = new OkHttpClient();
    private static final List<String> DEFAULT_FALLBACK_CHAIN = Arrays.asList("GPT4", "CLAUDE", "GROQ", "DEEPSEEK");
    private static final Map<String, String> MODEL_ALIASES = buildModelAliases();
    private static final Map<String, List<String>> PROVIDER_FALLBACKS = buildProviderFallbacks();
    private static final Set<String> NATIVE_MODELS = Set.of(
        "GPT4", "CLAUDE", "GROQ", "DEEPSEEK", "GEMINI", "COHERE", "PERPLEXITY", "LLAMA", "HUGGINGFACE", "XAI"
    );
    
    // API endpoints and keys (should be moved to config file)
    private final Map<String, String> apiEndpoints = Map.ofEntries(
        Map.entry("DEEPSEEK", "https://api.deepseek.com/v1/chat/completions"),
        Map.entry("GROQ", "https://api.groq.com/openai/v1/chat/completions"),
        Map.entry("CLAUDE", "https://api.anthropic.com/v1/messages"),
        Map.entry("GPT4", "https://api.openai.com/v1/chat/completions"),
        Map.entry("GEMINI", "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent"),
        Map.entry("COHERE", "https://api.cohere.com/v2/chat"),
        Map.entry("PERPLEXITY", "https://api.perplexity.ai/chat/completions"),
        Map.entry("LLAMA", "https://api.llama.com/compat/v1/chat/completions"),
        Map.entry("HUGGINGFACE", "https://router.huggingface.co/v1/chat/completions"),
        Map.entry("XAI", "https://api.x.ai/v1/chat/completions")
    );

    private final Map<String, String> defaultModels = Map.ofEntries(
        Map.entry("GPT4", "gpt-4"),
        Map.entry("CLAUDE", "claude-3-sonnet-20240229"),
        Map.entry("GROQ", "mixtral-8x7b-32768"),
        Map.entry("DEEPSEEK", "deepseek-coder"),
        Map.entry("GEMINI", "gemini-1.5-pro"),
        Map.entry("COHERE", "command-r-plus"),
        Map.entry("PERPLEXITY", "sonar-pro"),
        Map.entry("LLAMA", "Llama-4-Scout-17B-16E-Instruct"),
        Map.entry("HUGGINGFACE", "meta-llama/Llama-3.3-70B-Instruct"),
        Map.entry("XAI", "grok-2-latest")
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
        List<String> resolvedChain = fallbackChain == null || fallbackChain.isEmpty()
            ? DEFAULT_FALLBACK_CHAIN
            : fallbackChain;

        for (String aiModel : resolvedChain) {
            try {
                String canonicalModel = normalizeModelName(aiModel);
                if (canonicalModel == null) {
                    continue;
                }

                String response = executeAPICall(canonicalModel, prompt);
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

    /**
     * Compatibility wrapper for older callers that expect a primary-provider call.
     */
    public String callPrimaryProvider(String prompt) {
        return callAI("PRIMARY", prompt, DEFAULT_FALLBACK_CHAIN);
    }

    public String callProvider(String providerName, String prompt) {
        return callAI(providerName, prompt, getFallbackChainForProvider(providerName));
    }

    public List<String> getFallbackChainForProvider(String providerName) {
        String normalizedProvider = providerName == null
            ? ""
            : providerName.trim().toLowerCase(Locale.ROOT);

        List<String> fallbacks = PROVIDER_FALLBACKS.get(normalizedProvider);
        if (fallbacks != null && !fallbacks.isEmpty()) {
            return fallbacks;
        }

        String direct = normalizeModelName(providerName);
        if (direct != null) {
            return buildChain(direct, DEFAULT_FALLBACK_CHAIN);
        }

        return DEFAULT_FALLBACK_CHAIN;
    }
    
    private String executeAPICall(String aiModel, String prompt) throws IOException {
        return executeAPICall(aiModel, prompt, apiKeys.get(aiModel), null);
    }

    private String executeAPICall(String aiModel, String prompt, String apiKeyOverride,
                                  String endpointOverride) throws IOException {
        String endpoint = endpointOverride != null && !endpointOverride.isBlank()
            ? endpointOverride
            : apiEndpoints.get(aiModel);
        String apiKey = apiKeyOverride;
        
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
            case "GEMINI":
                return callGemini(endpoint, apiKey, prompt);
            case "COHERE":
                return callCohere(endpoint, apiKey, prompt);
            case "PERPLEXITY":
                return callOpenAICompatible(endpoint, apiKey, defaultModels.get("PERPLEXITY"), prompt, Collections.emptyMap());
            case "LLAMA":
                return callOpenAICompatible(endpoint, apiKey, defaultModels.get("LLAMA"), prompt, Collections.emptyMap());
            case "HUGGINGFACE":
                return callOpenAICompatible(endpoint, apiKey, defaultModels.get("HUGGINGFACE"), prompt, Collections.emptyMap());
            case "XAI":
                return callOpenAICompatible(endpoint, apiKey, defaultModels.get("XAI"), prompt, Collections.emptyMap());
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
        return callOpenAICompatible(endpoint, apiKey, defaultModels.get("GPT4"), prompt, Collections.emptyMap());
    }

    private String callGemini(String endpoint, String apiKey, String prompt) throws IOException {
        var root = mapper.createObjectNode();
        var contents = root.putArray("contents");
        var content = mapper.createObjectNode();
        var parts = content.putArray("parts");
        parts.add(mapper.createObjectNode().put("text", prompt));
        contents.add(content);
        root.putObject("generationConfig")
            .put("temperature", 0.7)
            .put("maxOutputTokens", 2048);

        Request request = new Request.Builder()
            .url(endpoint + "?key=" + apiKey)
            .post(RequestBody.create(root.toString(), MediaType.parse("application/json")))
            .addHeader("Content-Type", "application/json")
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonNode json = mapper.readTree(response.body().string());
                JsonNode candidates = json.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    JsonNode partsNode = candidates.get(0).path("content").path("parts");
                    if (partsNode.isArray() && !partsNode.isEmpty()) {
                        return partsNode.get(0).path("text").asText();
                    }
                }
            }
            handleErrorResponse(response);
        }
        return null;
    }

    private String callCohere(String endpoint, String apiKey, String prompt) throws IOException {
        var root = mapper.createObjectNode();
        root.put("model", defaultModels.get("COHERE"));
        var messages = root.putArray("messages");
        messages.add(mapper.createObjectNode()
            .put("role", "user")
            .put("content", prompt));

        Request request = new Request.Builder()
            .url(endpoint)
            .post(RequestBody.create(root.toString(), MediaType.parse("application/json")))
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                JsonNode json = mapper.readTree(response.body().string());
                JsonNode message = json.path("message").path("content");
                if (message.isArray() && !message.isEmpty()) {
                    JsonNode first = message.get(0);
                    if (first.has("text")) {
                        return first.path("text").asText();
                    }
                }
            }
            handleErrorResponse(response);
        }
        return null;
    }

    private String callOpenAICompatible(String endpoint, String apiKey, String model,
                                        String prompt, Map<String, String> extraHeaders) throws IOException {
        var root = mapper.createObjectNode();
        root.put("model", model);
        root.put("temperature", 0.7);
        root.put("max_tokens", 2000);
        var messages = root.putArray("messages");
        messages.add(mapper.createObjectNode()
            .put("role", "user")
            .put("content", prompt));

        return makeRequest(endpoint, apiKey, root.toString(), extraHeaders);
    }
    
    private String makeRequest(String endpoint, String apiKey, String jsonBody) throws IOException {
        return makeRequest(endpoint, apiKey, jsonBody, Collections.emptyMap());
    }

    private String makeRequest(String endpoint, String apiKey, String jsonBody,
                               Map<String, String> extraHeaders) throws IOException {
        Request.Builder builder = new Request.Builder()
                .url(endpoint)
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json");

        if (extraHeaders != null) {
            for (Map.Entry<String, String> header : extraHeaders.entrySet()) {
                builder.addHeader(header.getKey(), header.getValue());
            }
        }
        Request builtRequest = builder.build();
        
        try (Response response = client.newCall(builtRequest).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                JsonNode json = mapper.readTree(responseBody);
                
                // Extract from different response formats
                if (json.has("choices")) {
                    return json.path("choices").get(0).path("message").path("content").asText();
                } else if (json.has("content")) {
                    return json.path("content").get(0).path("text").asText();
                }
            }
            handleErrorResponse(response);
        }
        return null;
    }

    private void handleErrorResponse(Response response) throws IOException {
        if (response == null) {
            return;
        }
        if (response.code() == 429) {
            throw new IOException("Rate limited (429) - trigger rotation");
        }
        if (response.code() == 403) {
            throw new IOException("Forbidden/Quota exceeded (403) - trigger rotation");
        }
        if (!response.isSuccessful()) {
            throw new IOException("API request failed with status " + response.code());
        }
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

    public boolean hasNativeConnector(String providerName) {
        String model = normalizeModelName(providerName);
        return model != null && NATIVE_MODELS.contains(model);
    }

    public boolean isProviderConfigured(String providerName) {
        String model = normalizeModelName(providerName);
        return model != null && apiKeys.containsKey(model) && apiKeys.get(model) != null && !apiKeys.get(model).isBlank();
    }

    public String probeProviderConnection(String providerName, String apiKey, String endpointOverride) throws IOException {
        String providerId = getCanonicalProviderId(providerName);
        String model = normalizeModelName(providerId);
        if (model == null) {
            throw new IOException("Unsupported provider: " + providerName);
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IOException("Missing API key");
        }

        String response = executeAPICall(
            model,
            "Reply with a short one-line health check confirmation.",
            apiKey,
            endpointOverride
        );
        if (response == null || response.isBlank()) {
            throw new IOException("Provider returned an empty response");
        }
        return response;
    }

    public Map<String, Object> getProviderConnectorStatus(String providerName) {
        String normalizedModel = normalizeModelName(providerName);
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("provider", providerName);
        status.put("canonicalModel", normalizedModel);
        status.put("nativeConnector", normalizedModel != null && NATIVE_MODELS.contains(normalizedModel));
        status.put("configured", isProviderConfigured(providerName));
        status.put("endpoint", normalizedModel == null ? null : apiEndpoints.get(normalizedModel));
        status.put("fallbackChain", getFallbackChainForProvider(providerName));
        status.put("defaultModel", normalizedModel == null ? null : defaultModels.get(normalizedModel));
        return status;
    }

    public List<String> getCanonicalProviderIds() {
        return Arrays.asList(
            "openai-gpt4",
            "anthropic-claude",
            "google-gemini",
            "cohere",
            "perplexity",
            "meta-llama",
            "huggingface",
            "xai-grok",
            "deepseek",
            "mistral"
        );
    }

    public String getCanonicalProviderId(String providerName) {
        if (providerName == null || providerName.isBlank()) {
            return null;
        }

        String normalized = providerName.trim().toLowerCase(Locale.ROOT);
        if (getCanonicalProviderIds().contains(normalized)) {
            return normalized;
        }
        if (normalized.contains("openai") || normalized.contains("gpt")) {
            return "openai-gpt4";
        }
        if (normalized.contains("claude") || normalized.contains("anthropic")) {
            return "anthropic-claude";
        }
        if (normalized.contains("gemini") || normalized.contains("google")) {
            return "google-gemini";
        }
        if (normalized.contains("cohere")) {
            return "cohere";
        }
        if (normalized.contains("perplexity")) {
            return "perplexity";
        }
        if (normalized.contains("llama") || normalized.contains("meta")) {
            return "meta-llama";
        }
        if (normalized.contains("huggingface") || normalized.equals("hf")) {
            return "huggingface";
        }
        if (normalized.contains("xai") || normalized.contains("grok")) {
            return "xai-grok";
        }
        if (normalized.contains("deepseek")) {
            return "deepseek";
        }
        if (normalized.contains("mistral")) {
            return "mistral";
        }
        return normalized;
    }

    public String getProviderDisplayName(String providerName) {
        if (providerName == null || providerName.isBlank()) {
            return "Unknown Provider";
        }

        switch (providerName) {
            case "openai-gpt4":
                return "OpenAI GPT-4";
            case "anthropic-claude":
                return "Anthropic Claude";
            case "google-gemini":
                return "Google Gemini";
            case "cohere":
                return "Cohere";
            case "perplexity":
                return "Perplexity";
            case "meta-llama":
                return "Meta / Llama";
            case "huggingface":
                return "HuggingFace";
            case "xai-grok":
                return "XAI Grok";
            case "deepseek":
                return "DeepSeek";
            case "mistral":
                return "Mistral";
            default:
                return providerName;
        }
    }

    public String normalizeModelName(String aiModel) {
        if (aiModel == null || aiModel.isBlank()) {
            return null;
        }
        String normalized = aiModel.trim().toLowerCase(Locale.ROOT);
        return MODEL_ALIASES.getOrDefault(normalized, apiEndpoints.containsKey(aiModel) ? aiModel : null);
    }

    private static Map<String, String> buildModelAliases() {
        Map<String, String> aliases = new HashMap<>();
        aliases.put("gpt4", "GPT4");
        aliases.put("gpt-4", "GPT4");
        aliases.put("openai-gpt4", "GPT4");
        aliases.put("openai", "GPT4");
        aliases.put("claude", "CLAUDE");
        aliases.put("claude-3", "CLAUDE");
        aliases.put("anthropic", "CLAUDE");
        aliases.put("anthropic-claude", "CLAUDE");
        aliases.put("groq", "GROQ");
        aliases.put("meta-llama", "LLAMA");
        aliases.put("mistral", "GROQ");
        aliases.put("huggingface", "GROQ");
        aliases.put("xai-grok", "GROQ");
        aliases.put("grok", "GROQ");
        aliases.put("deepseek", "DEEPSEEK");
        aliases.put("google-gemini", "GEMINI");
        aliases.put("gemini", "GEMINI");
        aliases.put("cohere", "COHERE");
        aliases.put("perplexity", "PERPLEXITY");
        aliases.put("meta", "LLAMA");
        aliases.put("llama", "LLAMA");
        aliases.put("meta-llama-native", "LLAMA");
        aliases.put("huggingface", "HUGGINGFACE");
        aliases.put("xai-grok", "XAI");
        aliases.put("xai", "XAI");
        aliases.put("primary", "GPT4");
        return aliases;
    }

    private static Map<String, List<String>> buildProviderFallbacks() {
        Map<String, List<String>> fallbacks = new HashMap<>();
        fallbacks.put("openai-gpt4", buildChain("GPT4", DEFAULT_FALLBACK_CHAIN));
        fallbacks.put("anthropic-claude", buildChain("CLAUDE", DEFAULT_FALLBACK_CHAIN));
        fallbacks.put("google-gemini", buildChain("GEMINI", Arrays.asList("GPT4", "CLAUDE", "GROQ", "DEEPSEEK")));
        fallbacks.put("meta-llama", buildChain("LLAMA", Arrays.asList("GROQ", "DEEPSEEK", "GPT4", "CLAUDE")));
        fallbacks.put("mistral", buildChain("GROQ", Arrays.asList("DEEPSEEK", "GPT4", "CLAUDE")));
        fallbacks.put("cohere", buildChain("COHERE", Arrays.asList("CLAUDE", "GPT4", "GROQ", "DEEPSEEK")));
        fallbacks.put("huggingface", buildChain("HUGGINGFACE", Arrays.asList("LLAMA", "GROQ", "DEEPSEEK", "GPT4", "CLAUDE")));
        fallbacks.put("xai-grok", buildChain("XAI", Arrays.asList("GPT4", "CLAUDE", "DEEPSEEK", "GROQ")));
        fallbacks.put("deepseek", buildChain("DEEPSEEK", Arrays.asList("GPT4", "CLAUDE", "GROQ")));
        fallbacks.put("perplexity", buildChain("PERPLEXITY", Arrays.asList("GPT4", "CLAUDE", "GROQ", "DEEPSEEK")));
        return fallbacks;
    }

    private static List<String> buildChain(String primary, List<String> fallbacks) {
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        if (primary != null && !primary.isBlank()) {
            ordered.add(primary);
        }
        if (fallbacks != null) {
            ordered.addAll(fallbacks);
        }
        return new ArrayList<>(ordered);
    }
}
