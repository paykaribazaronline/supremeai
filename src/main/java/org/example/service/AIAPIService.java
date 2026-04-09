package org.example.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import okhttp3.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.example.model.APIProvider;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AIAPIService {
    private static final Logger logger = LoggerFactory.getLogger(AIAPIService.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final List<String> DEFAULT_FALLBACK_CHAIN = Arrays.asList("GROQ", "DEEPSEEK", "CLAUDE", "GPT4");
    private static final long MIN_BACKOFF_MS = 100L;
    private static final long MAX_BACKOFF_MS = 2_000L;
    private static final Map<String, String> MODEL_ALIASES = buildModelAliases();
    private static final Map<String, List<String>> PROVIDER_FALLBACKS = buildProviderFallbacks();
    private static final Set<String> OPTIONAL_AUTH_MODELS = Set.of("AIRLLM");
    private static final Set<String> NATIVE_MODELS = Set.of(
        "GPT4", "CLAUDE", "GROQ", "DEEPSEEK", "GEMINI", "COHERE", "PERPLEXITY", "LLAMA", "HUGGINGFACE", "XAI", "AIRLLM"
    );

    private final OkHttpClient client;
    private final int timeoutMs;
    private final int maxRetries;
    private final long retryBaseBackoffMs;
    private final int maxPromptTokens;
    private final int maxOutputTokens;
    private final int perProviderRequestsPerMinute;
    private final int circuitFailureThreshold;
    private final long circuitOpenMs;
    private final BlockingQueue<QueuedRequest> slowRequestQueue;
    private final ExecutorService queueExecutor;
    private final Cache<String, String> responseCache;
    private final Map<String, ProviderCircuitState> providerCircuits = new ConcurrentHashMap<>();
    private final Map<String, ProviderWindowCounter> providerWindows = new ConcurrentHashMap<>();
    private final Map<String, String> endpointOverrides = new ConcurrentHashMap<>();

    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong cacheHits = new AtomicLong();
    private final AtomicLong retries = new AtomicLong();
    private final AtomicLong queuedSlowRequests = new AtomicLong();
    private final AtomicLong queueDrops = new AtomicLong();
    private final AtomicLong rateLimitErrors = new AtomicLong();
    private final AtomicLong circuitOpenSkips = new AtomicLong();
    private final AtomicLong successfulResponses = new AtomicLong();
    private final AtomicLong failedResponses = new AtomicLong();

    private ProviderRegistryService providerRegistryService;
    private FallbackConfigService fallbackConfigService;

    /** Called by Spring / TestBeansConfiguration after construction. */
    public void setFallbackConfigService(FallbackConfigService fallbackConfigService) {
        this.fallbackConfigService = fallbackConfigService;
    }
    
    private final Deque<Map<String, Object>> deadLetterLog = new ArrayDeque<>();
    private static final int MAX_DEAD_LETTER = 200;

    // API endpoints and keys (should be moved to config file)
    private final Map<String, String> apiEndpoints = Map.ofEntries(
        Map.entry("DEEPSEEK", "https://api.deepseek.com/v1/chat/completions"),
        Map.entry("GROQ", "https://api.groq.com/openai/v1/chat/completions"),
        Map.entry("CLAUDE", "https://api.anthropic.com/v1/messages"),
        Map.entry("GPT4", "https://api.openai.com/v1/chat/completions"),
        Map.entry("GEMINI", "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"),
        Map.entry("COHERE", "https://api.cohere.com/v2/chat"),
        Map.entry("PERPLEXITY", "https://api.perplexity.ai/chat/completions"),
        Map.entry("LLAMA", "https://api.llama.com/compat/v1/chat/completions"),
        Map.entry("HUGGINGFACE", "https://router.huggingface.co/v1/chat/completions"),
        Map.entry("XAI", "https://api.x.ai/v1/chat/completions"),
        Map.entry("AIRLLM", "https://unsymmetrical-unrepugnant-lilah.ngrok-free.dev/v1/chat/completions")
    );

    private final Map<String, String> defaultModels = Map.ofEntries(
        Map.entry("GPT4", "gpt-4"),
        Map.entry("CLAUDE", "claude-3-sonnet-20240229"),
        Map.entry("GROQ", "mixtral-8x7b-32768"),
        Map.entry("DEEPSEEK", "deepseek-coder"),
        Map.entry("GEMINI", "gemini-1.5-flash"),
        Map.entry("COHERE", "command-r-plus"),
        Map.entry("PERPLEXITY", "sonar-pro"),
        Map.entry("LLAMA", "Llama-4-Scout-17B-16E-Instruct"),
        Map.entry("HUGGINGFACE", "meta-llama/Llama-3.3-70B-Instruct"),
        Map.entry("XAI", "grok-2-latest"),
        Map.entry("AIRLLM", "mistralai/Mistral-7B-Instruct-v0.3")
    );
    
    private final Map<String, String> apiKeys = new HashMap<>();

    public AIAPIService(Map<String, String> keys) {
        this(keys, 7_000, 2, 250, 2_000, 1_500, 60, 3, 30_000, 500, 10, 1_000);
    }

    public AIAPIService(Map<String, String> keys,
                        int timeoutMs,
                        int maxRetries,
                        long retryBaseBackoffMs,
                        int maxPromptTokens,
                        int maxOutputTokens,
                        int perProviderRequestsPerMinute,
                        int circuitFailureThreshold,
                        long circuitOpenMs,
                        int slowQueueCapacity,
                        int cacheTtlMinutes,
                        int cacheMaxSize) {
        this.apiKeys.putAll(keys);
        this.timeoutMs = timeoutMs;
        this.maxRetries = Math.max(0, maxRetries);
        this.retryBaseBackoffMs = Math.max(MIN_BACKOFF_MS, retryBaseBackoffMs);
        this.maxPromptTokens = Math.max(200, maxPromptTokens);
        this.maxOutputTokens = Math.max(200, maxOutputTokens);
        this.perProviderRequestsPerMinute = Math.max(10, perProviderRequestsPerMinute);
        this.circuitFailureThreshold = Math.max(2, circuitFailureThreshold);
        this.circuitOpenMs = Math.max(1_000, circuitOpenMs);
        this.slowRequestQueue = new LinkedBlockingQueue<>(Math.max(50, slowQueueCapacity));
        this.client = new OkHttpClient.Builder()
            .connectTimeout(this.timeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(this.timeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(this.timeoutMs, TimeUnit.MILLISECONDS)
            .callTimeout(this.timeoutMs, TimeUnit.MILLISECONDS)
            .build();
        this.responseCache = Caffeine.newBuilder()
            .maximumSize(Math.max(100, cacheMaxSize))
            .expireAfterWrite(Math.max(1, cacheTtlMinutes), TimeUnit.MINUTES)
            .build();
        this.queueExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ai-slow-request-queue-worker");
            t.setDaemon(true);
            return t;
        });
        startSlowQueueWorker();
    }

    public synchronized void updateApiKey(String provider, String newKey) {
        if (provider == null || provider.isBlank() || newKey == null || newKey.isBlank()) {
            return;
        }
        this.apiKeys.put(provider.trim().toUpperCase(Locale.ROOT), newKey);
    }

    public synchronized void updateProviderEndpoint(String provider, String endpoint) {
        String model = normalizeModelName(provider);
        if (model == null || endpoint == null || endpoint.isBlank()) {
            return;
        }
        endpointOverrides.put(model, endpoint.trim());
    }

    public void setProviderRegistryService(ProviderRegistryService providerRegistryService) {
        this.providerRegistryService = providerRegistryService;
    }
    
    /**
     * Call AI agent with fallback chain support
     * @param role BUILDER, REVIEWER, or ARCHITECT
     * @param prompt The task/prompt
     * @param fallbackChain List of AI models to try in order
     * @return AI response or null if all fail
     */
    public String callAI(String role, String prompt, List<String> fallbackChain) {
        totalRequests.incrementAndGet();
        String boundedPrompt = applyPromptTokenCap(prompt);

        String cacheKey = buildCacheKey(boundedPrompt);
        String cached = responseCache.getIfPresent(cacheKey);
        if (cached != null) {
            cacheHits.incrementAndGet();
            return cached;
        }

        List<String> resolvedChain = resolveEffectiveFallbackChain(fallbackChain);

        for (String aiModel : resolvedChain) {
            try {
                String canonicalModel = normalizeModelName(aiModel);
                if (canonicalModel == null) {
                    continue;
                }

                if (isCircuitOpen(canonicalModel)) {
                    circuitOpenSkips.incrementAndGet();
                    continue;
                }

                if (!allowProviderRequest(canonicalModel)) {
                    rateLimitErrors.incrementAndGet();
                    continue;
                }

                String response = executeWithRetry(canonicalModel, boundedPrompt);
                if (response != null) {
                    String boundedResponse = applyOutputTokenCap(response);
                    successfulResponses.incrementAndGet();
                    responseCache.put(cacheKey, boundedResponse);
                    return boundedResponse;
                }
            } catch (Exception e) {
                failedResponses.incrementAndGet();
                logger.warn("Provider {} failed: {}", aiModel, e.getMessage());
                continue;
            }
        }

        failedResponses.incrementAndGet();
        return null; // All fallbacks failed
    }

    /**
     * Compatibility wrapper for older callers that expect a primary-provider call.
     */
    public String callPrimaryProvider(String prompt) {
        return callAI("PRIMARY", prompt, null); // null → resolveEffectiveFallbackChain() picks DB chain
    }

    public String callProvider(String providerName, String prompt) {
        APIProvider configuredProvider = resolveConfiguredProvider(providerName);
        String configuredModel = configuredProvider == null
            ? null
            : normalizeModelName(firstNonBlank(
                configuredProvider.getId(),
                configuredProvider.getAlias(),
                configuredProvider.getBaseModel(),
                configuredProvider.getName()
            ));

        if (configuredProvider != null) {
            try {
                String response = executeConfiguredProvider(configuredProvider, prompt);
                if (response != null && !response.isBlank()) {
                    return applyOutputTokenCap(response);
                }
            } catch (IOException exception) {
                logger.warn("Configured provider {} failed via saved endpoint: {}", providerName, exception.getMessage());
            }
        }

        List<String> fallbackChain = new ArrayList<>(getFallbackChainForProvider(providerName));
        if (configuredModel != null) {
            fallbackChain.removeIf(configuredModel::equals);
        }
        if (fallbackChain.isEmpty()) {
            fallbackChain = getFallbackChainForProvider(providerName);
        }
        return callAI(providerName, prompt, fallbackChain);
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
            return buildChain(direct, resolveEffectiveFallbackChain(null));
        }

        return resolveEffectiveFallbackChain(null);
    }

    /**
     * Resolve the fallback chain to actually use, in priority order:
     * 1. Caller-supplied chain (if non-empty) — honours explicit per-call override.
     * 2. Admin-configured chain from FallbackConfigService (if non-empty) — data-driven.
     * 3. All active providers registered in ProviderRegistryService (if any) — DB-driven.
     * 4. Hard-coded DEFAULT_FALLBACK_CHAIN — last resort bootstrap when DB is empty.
     */
    private List<String> resolveEffectiveFallbackChain(List<String> callerChain) {
        if (callerChain != null && !callerChain.isEmpty()) {
            return callerChain;
        }
        // Admin-configured order
        if (fallbackConfigService != null) {
            List<String> configured = fallbackConfigService.getFallbackChain();
            if (configured != null && !configured.isEmpty()) {
                return configured;
            }
        }
        // All active DB providers (in registration order)
        if (providerRegistryService != null) {
            List<String> dbIds = providerRegistryService.getActiveProviders().stream()
                .map(APIProvider::getId)
                .filter(id -> id != null && !id.isBlank())
                .toList();
            if (!dbIds.isEmpty()) {
                return dbIds;
            }
        }
        // Bootstrap fallback — only used when no providers have been configured yet.
        // If providers ARE registered in the DB but all lack API keys we still use the
        // DEFAULT_FALLBACK_CHAIN so the system can recover once keys are added.
        // However, when ZERO providers are registered the admin has intentionally chosen
        // to run without external AI: return an empty chain so callAI() returns null
        // immediately instead of spinning through external services that will all fail.
        if (providerRegistryService != null
                && providerRegistryService.getActiveProviders().isEmpty()
                && apiKeys.isEmpty()) {
            return Collections.emptyList();
        }
        return DEFAULT_FALLBACK_CHAIN;
    }
    
    private String executeAPICall(String aiModel, String prompt) throws IOException {
        return executeAPICall(aiModel, prompt, apiKeys.get(aiModel), null);
    }

    private String executeWithRetry(String aiModel, String prompt) throws IOException {
        IOException last = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                String response = executeAPICall(aiModel, prompt);
                onProviderSuccess(aiModel);
                return response;
            } catch (IOException ex) {
                last = ex;
                onProviderFailure(aiModel, ex.getMessage());

                if (isRateLimitError(ex.getMessage())) {
                    rateLimitErrors.incrementAndGet();
                }

                if (isTimeoutException(ex.getMessage())) {
                    enqueueSlowRequest(aiModel, prompt, "timeout");
                }

                if (attempt >= maxRetries || !isRetriable(ex.getMessage())) {
                    break;
                }

                retries.incrementAndGet();
                sleepBackoff(attempt);
            }
        }

        if (last != null) {
            throw last;
        }
        return null;
    }

    private String executeAPICall(String aiModel, String prompt, String apiKeyOverride,
                                  String endpointOverride) throws IOException {
        String endpoint = endpointOverride != null && !endpointOverride.isBlank()
            ? endpointOverride
            : endpointOverrides.getOrDefault(aiModel, apiEndpoints.get(aiModel));
        String apiKey = apiKeyOverride;
        
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("Unknown AI model or missing endpoint: " + aiModel);
        }
        if (requiresApiKeyForModel(aiModel) && (apiKey == null || apiKey.isBlank())) {
            throw new IllegalArgumentException("Missing API key: " + aiModel);
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
            case "AIRLLM":
                return callAirLlm(endpoint, apiKey, prompt);
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
        return callOpenAICompatible(endpoint, apiKey, model, prompt, extraHeaders, false);
    }

    private String callOpenAICompatible(String endpoint, String apiKey, String model,
                                        String prompt, Map<String, String> extraHeaders,
                                        boolean authOptional) throws IOException {
        var root = mapper.createObjectNode();
        root.put("model", model);
        root.put("temperature", 0.7);
        root.put("max_tokens", 2000);
        var messages = root.putArray("messages");
        messages.add(mapper.createObjectNode()
            .put("role", "user")
            .put("content", prompt));

        return makeRequest(endpoint, apiKey, root.toString(), extraHeaders, authOptional);
    }

    private String callAirLlm(String endpoint, String apiKey, String prompt) throws IOException {
        return callOpenAICompatible(
            endpoint,
            apiKey,
            defaultModels.get("AIRLLM"),
            prompt,
            Collections.emptyMap(),
            true
        );
    }
    
    private String makeRequest(String endpoint, String apiKey, String jsonBody) throws IOException {
        return makeRequest(endpoint, apiKey, jsonBody, Collections.emptyMap());
    }

    private String makeRequest(String endpoint, String apiKey, String jsonBody,
                               Map<String, String> extraHeaders) throws IOException {
        return makeRequest(endpoint, apiKey, jsonBody, extraHeaders, false);
    }

    private String makeRequest(String endpoint, String apiKey, String jsonBody,
                               Map<String, String> extraHeaders,
                               boolean authOptional) throws IOException {
        Request.Builder builder = new Request.Builder()
                .url(endpoint)
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .addHeader("Content-Type", "application/json");

        if (apiKey != null && !apiKey.isBlank()) {
            builder.addHeader("Authorization", "Bearer " + apiKey);
        } else if (!authOptional) {
            throw new IOException("Missing API key");
        }

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

    public String applyPromptTokenCap(String prompt) {
        if (prompt == null) {
            return "";
        }
        int estimated = estimateTokens(prompt);
        if (estimated <= maxPromptTokens) {
            return prompt;
        }

        int maxChars = Math.max(500, maxPromptTokens * 4);
        if (prompt.length() <= maxChars) {
            return prompt;
        }
        return prompt.substring(0, maxChars);
    }

    public String applyOutputTokenCap(String output) {
        if (output == null) {
            return null;
        }
        int estimated = estimateTokens(output);
        if (estimated <= maxOutputTokens) {
            return output;
        }

        int maxChars = Math.max(500, maxOutputTokens * 4);
        if (output.length() <= maxChars) {
            return output;
        }
        return output.substring(0, maxChars);
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
        if (model == null) {
            return false;
        }
        if (!requiresApiKey(providerName)) {
            return getDefaultEndpoint(providerName) != null;
        }
        return apiKeys.containsKey(model) && apiKeys.get(model) != null && !apiKeys.get(model).isBlank();
    }

    public boolean requiresApiKey(String providerName) {
        String model = normalizeModelName(providerName);
        return model != null && requiresApiKeyForModel(model);
    }

    public String getDefaultEndpoint(String providerName) {
        String model = normalizeModelName(providerName);
        if (model == null) {
            return null;
        }
        return endpointOverrides.getOrDefault(model, apiEndpoints.get(model));
    }

    public String probeProviderConnection(String providerName, String apiKey, String endpointOverride) throws IOException {
        String providerId = getCanonicalProviderId(providerName);
        String model = normalizeModelName(providerId);
        if (model == null) {
            throw new IOException("Unsupported provider: " + providerName);
        }
        if (requiresApiKey(providerId) && (apiKey == null || apiKey.isBlank())) {
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
        status.put("endpoint", getDefaultEndpoint(providerName));
        status.put("fallbackChain", getFallbackChainForProvider(providerName));
        status.put("defaultModel", normalizedModel == null ? null : defaultModels.get(normalizedModel));
        status.put("requiresApiKey", requiresApiKey(providerName));
        return status;
    }

    public Map<String, Object> getOperationalMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("timeoutMs", timeoutMs);
        metrics.put("maxRetries", maxRetries);
        metrics.put("maxPromptTokens", maxPromptTokens);
        metrics.put("maxOutputTokens", maxOutputTokens);
        metrics.put("perProviderRequestsPerMinute", perProviderRequestsPerMinute);
        metrics.put("circuitFailureThreshold", circuitFailureThreshold);
        metrics.put("circuitOpenMs", circuitOpenMs);
        metrics.put("defaultFallbackChain", DEFAULT_FALLBACK_CHAIN);

        metrics.put("totalRequests", totalRequests.get());
        metrics.put("successfulResponses", successfulResponses.get());
        metrics.put("failedResponses", failedResponses.get());
        metrics.put("cacheHits", cacheHits.get());
        metrics.put("retryAttempts", retries.get());
        metrics.put("rateLimitErrors", rateLimitErrors.get());
        metrics.put("circuitOpenSkips", circuitOpenSkips.get());
        metrics.put("queuedSlowRequests", queuedSlowRequests.get());
        metrics.put("queueDrops", queueDrops.get());
        metrics.put("queueDepth", slowRequestQueue.size());
        synchronized (this) {
            metrics.put("deadLetterCount", deadLetterLog.size());
        }
        metrics.put("providerCircuits", getCircuitStates());
        return metrics;
    }

    private Map<String, Object> getCircuitStates() {
        Map<String, Object> states = new LinkedHashMap<>();
        long now = System.currentTimeMillis();
        for (Map.Entry<String, ProviderCircuitState> entry : providerCircuits.entrySet()) {
            ProviderCircuitState state = entry.getValue();
            states.put(entry.getKey(), Map.of(
                "open", state.isOpen(now),
                "consecutiveFailures", state.consecutiveFailures,
                "openUntilEpochMs", state.openUntilEpochMs
            ));
        }
        return states;
    }

    public List<String> getCanonicalProviderIds() {
        return Arrays.asList(
            "openai-gpt4",
            "anthropic-claude",
            "google-gemini",
            "cohere",
            "perplexity",
            "meta-llama",
            "airllm-local",
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
        if (normalized.contains("airllm")) {
            return "airllm-local";
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
            case "airllm-local":
                return "AirLLM Local";
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
        aliases.put("airllm", "AIRLLM");
        aliases.put("airllm-local", "AIRLLM");
        aliases.put("local-airllm", "AIRLLM");
        aliases.put("huggingface", "HUGGINGFACE");
        aliases.put("xai-grok", "XAI");
        aliases.put("xai", "XAI");
        aliases.put("primary", "GPT4");
        return aliases;
    }

    private static Map<String, List<String>> buildProviderFallbacks() {
        Map<String, List<String>> fallbacks = new HashMap<>();
        fallbacks.put("openai-gpt4", buildChain("GPT4", Arrays.asList("CLAUDE", "DEEPSEEK", "GROQ")));
        fallbacks.put("anthropic-claude", buildChain("CLAUDE", Arrays.asList("DEEPSEEK", "GROQ", "GPT4")));
        fallbacks.put("google-gemini", buildChain("GEMINI", Arrays.asList("DEEPSEEK", "GROQ", "CLAUDE", "GPT4")));
        fallbacks.put("meta-llama", buildChain("LLAMA", Arrays.asList("GROQ", "DEEPSEEK", "CLAUDE", "GPT4")));
        fallbacks.put("airllm-local", buildChain("AIRLLM", Arrays.asList("DEEPSEEK", "GROQ", "CLAUDE", "GPT4")));
        fallbacks.put("mistral", buildChain("GROQ", Arrays.asList("DEEPSEEK", "CLAUDE", "GPT4")));
        fallbacks.put("cohere", buildChain("COHERE", Arrays.asList("DEEPSEEK", "GROQ", "CLAUDE", "GPT4")));
        fallbacks.put("huggingface", buildChain("HUGGINGFACE", Arrays.asList("LLAMA", "GROQ", "DEEPSEEK", "CLAUDE", "GPT4")));
        fallbacks.put("xai-grok", buildChain("XAI", Arrays.asList("DEEPSEEK", "GROQ", "CLAUDE", "GPT4")));
        fallbacks.put("deepseek", buildChain("DEEPSEEK", Arrays.asList("GROQ", "CLAUDE", "GPT4")));
        fallbacks.put("perplexity", buildChain("PERPLEXITY", Arrays.asList("DEEPSEEK", "GROQ", "CLAUDE", "GPT4")));
        return fallbacks;
    }

    private boolean requiresApiKeyForModel(String model) {
        return model != null && !OPTIONAL_AUTH_MODELS.contains(model);
    }

    private APIProvider resolveConfiguredProvider(String providerName) {
        if (providerRegistryService == null) {
            return null;
        }
        String canonicalProviderId = getCanonicalProviderId(providerName);
        if (canonicalProviderId == null) {
            return null;
        }
        return providerRegistryService.getProvider(canonicalProviderId);
    }

    private String executeConfiguredProvider(APIProvider provider, String prompt) throws IOException {
        String providerId = firstNonBlank(
            provider.getId(),
            provider.getAlias(),
            provider.getBaseModel(),
            provider.getName()
        );
        String model = normalizeModelName(providerId);
        if (model == null) {
            throw new IOException("Unsupported provider: " + providerId);
        }

        String endpoint = firstNonBlank(provider.getEndpoint(), getDefaultEndpoint(providerId));
        String apiKey = firstNonBlank(provider.getApiKey(), apiKeys.get(model));
        return executeAPICall(model, prompt, apiKey, endpoint);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
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

    private boolean allowProviderRequest(String provider) {
        ProviderWindowCounter counter = providerWindows.computeIfAbsent(provider, p -> new ProviderWindowCounter());
        return counter.tryAcquire(perProviderRequestsPerMinute);
    }

    private String buildCacheKey(String prompt) {
        return "prompt:" + Integer.toHexString(Objects.hashCode(prompt));
    }

    private void startSlowQueueWorker() {
        queueExecutor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                QueuedRequest current = null;
                try {
                    current = slowRequestQueue.take();
                    if (isCircuitOpen(current.provider)) {
                        continue;
                    }
                    executeWithRetry(current.provider, current.prompt);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (Exception ex) {
                    logger.debug("Slow queue execution failed: {}", ex.getMessage());
                    if (current != null) {
                        recordDeadLetter(current.provider, current.prompt, current.reason, ex.getMessage());
                    }
                }
            }
        });
    }

    private synchronized void recordDeadLetter(String provider, String prompt, String reason, String error) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("provider", provider);
        entry.put("prompt", prompt != null && prompt.length() > 200 ? prompt.substring(0, 200) : prompt);
        entry.put("reason", reason);
        entry.put("error", error);
        entry.put("timestamp", System.currentTimeMillis());
        deadLetterLog.addFirst(entry);
        while (deadLetterLog.size() > MAX_DEAD_LETTER) {
            deadLetterLog.pollLast();
        }
    }

    /** Returns a snapshot of recent dead-letter entries (newest first) and clears the in-memory log. */
    public synchronized List<Map<String, Object>> drainDeadLetterItems() {
        List<Map<String, Object>> items = new ArrayList<>(deadLetterLog);
        deadLetterLog.clear();
        return items;
    }

    private void enqueueSlowRequest(String provider, String prompt, String reason) {
        boolean offered = slowRequestQueue.offer(new QueuedRequest(provider, prompt, reason));
        if (offered) {
            queuedSlowRequests.incrementAndGet();
            return;
        }
        queueDrops.incrementAndGet();
        logger.warn("Slow request queue full, dropping {} request for provider {}", reason, provider);
    }

    private boolean isRetriable(String message) {
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("timeout")
            || lower.contains("timed out")
            || lower.contains("429")
            || lower.contains("503")
            || lower.contains("502")
            || lower.contains("connection reset")
            || lower.contains("temporarily unavailable");
    }

    private boolean isTimeoutException(String message) {
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("timeout") || lower.contains("timed out");
    }

    private boolean isRateLimitError(String message) {
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("429") || lower.contains("rate limited") || lower.contains("quota exceeded");
    }

    private void sleepBackoff(int attempt) {
        long waitMs = Math.min(MAX_BACKOFF_MS, retryBaseBackoffMs * (1L << attempt));
        try {
            Thread.sleep(waitMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void onProviderSuccess(String provider) {
        providerCircuits.compute(provider, (k, state) -> {
            ProviderCircuitState resolved = state == null ? new ProviderCircuitState() : state;
            resolved.onSuccess();
            return resolved;
        });
    }

    private void onProviderFailure(String provider, String reason) {
        providerCircuits.compute(provider, (k, state) -> {
            ProviderCircuitState resolved = state == null ? new ProviderCircuitState() : state;
            resolved.onFailure(circuitFailureThreshold, circuitOpenMs);
            return resolved;
        });
        logger.warn("Provider {} failure recorded: {}", provider, reason);
    }

    private boolean isCircuitOpen(String provider) {
        ProviderCircuitState state = providerCircuits.get(provider);
        return state != null && state.isOpen(System.currentTimeMillis());
    }

    private static final class ProviderCircuitState {
        private int consecutiveFailures;
        private long openUntilEpochMs;

        private void onSuccess() {
            consecutiveFailures = 0;
            openUntilEpochMs = 0L;
        }

        private void onFailure(int threshold, long openMs) {
            consecutiveFailures++;
            if (consecutiveFailures >= threshold) {
                openUntilEpochMs = System.currentTimeMillis() + openMs;
            }
        }

        private boolean isOpen(long nowMs) {
            return openUntilEpochMs > nowMs;
        }
    }

    private static final class ProviderWindowCounter {
        private long windowStartMs = System.currentTimeMillis();
        private int usedInWindow = 0;

        private synchronized boolean tryAcquire(int perMinuteLimit) {
            long now = System.currentTimeMillis();
            if (now - windowStartMs >= 60_000) {
                windowStartMs = now;
                usedInWindow = 0;
            }
            if (usedInWindow >= perMinuteLimit) {
                return false;
            }
            usedInWindow++;
            return true;
        }
    }

    private static final class QueuedRequest {
        private final String provider;
        private final String prompt;
        private final String reason;

        private QueuedRequest(String provider, String prompt, String reason) {
            this.provider = provider;
            this.prompt = prompt;
            this.reason = reason;
        }
    }
}
