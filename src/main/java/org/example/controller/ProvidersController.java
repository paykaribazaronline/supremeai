package org.example.controller;

import org.example.model.APIProvider;
import org.example.service.AIAPIService;
import org.example.service.FallbackConfigService;
import org.example.service.ProviderAuditService;
import org.example.service.ProviderManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * API Providers Controller
 * Manages AI API provider configurations
 */
@RestController
@RequestMapping("/api/providers")
@CrossOrigin(origins = "*")
public class ProvidersController {
    private static final Logger logger = LoggerFactory.getLogger(ProvidersController.class);
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    // Cache for dynamic model search (refreshed every 30 minutes)
    private volatile List<Map<String, String>> dynamicModelCache = null;
    private volatile long cacheTimestamp = 0;
    private static final long CACHE_TTL_MS = 30 * 60 * 1000; // 30 minutes

    @Autowired
    private ProviderManagementService providerManagementService;

    @Autowired
    private FallbackConfigService fallbackConfigService;

    @Autowired
    private AIAPIService aiApiService;

    @Autowired
    private ProviderAuditService providerAuditService;

    @GetMapping("/available")
    public ResponseEntity<?> getAvailableProviders() {
        try {
            return ResponseEntity.ok(providerManagementService.getAvailableProviders());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/configured")
    public ResponseEntity<?> getConfiguredProviders() {
        try {
            return ResponseEntity.ok(providerManagementService.getConfiguredProviders());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addProvider(@RequestBody APIProvider provider) {
        try {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "provider", providerManagementService.saveProvider(provider)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProvider(@PathVariable("id") String id, @RequestBody APIProvider provider) {
        try {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "provider", providerManagementService.updateProvider(id, provider)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/test/{id}")
    public ResponseEntity<?> testProvider(@PathVariable("id") String id) {
        try {
            return ResponseEntity.ok(providerManagementService.probeProvider(id));
        } catch (Exception e) {
            String message = e.getMessage() == null ? "Provider probe failed" : e.getMessage();
            if (message.contains("not found")) {
                return ResponseEntity.status(404).body(Map.of("error", message));
            }
            return ResponseEntity.status(500).body(Map.of("error", message));
        }
    }

    @PostMapping("/probe/{id}")
    public ResponseEntity<?> probeProvider(@PathVariable("id") String id) {
        return testProvider(id);
    }

    @PostMapping("/rotate/{id}")
    public ResponseEntity<?> rotateProvider(@PathVariable("id") String id, @RequestBody Map<String, String> request) {
        try {
            return ResponseEntity.ok(providerManagementService.rotateProvider(id, request));
        } catch (Exception e) {
            String message = e.getMessage() == null ? "Provider rotation failed" : e.getMessage();
            if (message.contains("not found")) {
                return ResponseEntity.status(404).body(Map.of("error", message));
            }
            return ResponseEntity.status(500).body(Map.of("error", message));
        }
    }

    @PostMapping("/remove")
    public ResponseEntity<?> removeProvider(@RequestBody Map<String, String> request) {
        try {
            String id = request.get("id");
            boolean removed = providerManagementService.removeProvider(id, request.get("actedBy"));
            return ResponseEntity.ok(Map.of("success", removed));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProvider(@PathVariable("id") String id) {
        try {
            return ResponseEntity.ok(Map.of("success", providerManagementService.removeProvider(id, "system")));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/audit")
    public ResponseEntity<?> getAuditEvents(@RequestParam(name = "limit", defaultValue = "100") int limit) {
        try {
            return ResponseEntity.ok(providerManagementService.getAuditEvents(limit));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── Fallback chain management ────────────────────────────────────────────

    /**
     * GET /api/providers/fallback-chain
     * Returns the admin-configured fallback order.
     * Empty list = "use all active DB providers in registration order".
     */
    @GetMapping("/fallback-chain")
    public ResponseEntity<?> getFallbackChain() {
        try {
            return ResponseEntity.ok(Map.of("fallbackChain", fallbackConfigService.getFallbackChain()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/providers/fallback-chain
     * Body: { "fallbackChain": ["GROQ", "CLAUDE", "GPT4"] }
     * Saves the admin-configured fallback order.
     */
    @PostMapping("/fallback-chain")
    public ResponseEntity<?> setFallbackChain(@RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            List<String> chain = (List<String>) body.get("fallbackChain");
            fallbackConfigService.setFallbackChain(chain);
            return ResponseEntity.ok(Map.of("success", true, "fallbackChain", fallbackConfigService.getFallbackChain()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── Model suggestions (DYNAMIC — fetches from real APIs) ───────────────

    /**
     * GET /api/providers/suggest?q=...
     * Searches real AI model registries (OpenRouter, HuggingFace) dynamically.
     * Discovers new models automatically — no hardcoded lists.
     * Falls back to cached results if APIs are unreachable.
     */
    @GetMapping("/suggest")
    public ResponseEntity<?> suggestModels(@RequestParam(name = "q", defaultValue = "") String query) {
        try {
            List<Map<String, String>> all = getDynamicModelSuggestions();
            String q = query.trim().toLowerCase(Locale.ROOT);
            List<Map<String, String>> filtered = q.isEmpty()
                ? all.stream().limit(20).toList()
                : all.stream()
                    .filter(m -> (m.getOrDefault("id","")).toLowerCase(Locale.ROOT).contains(q)
                              || (m.getOrDefault("name","")).toLowerCase(Locale.ROOT).contains(q)
                              || (m.getOrDefault("provider","")).toLowerCase(Locale.ROOT).contains(q)
                              || (m.getOrDefault("model","")).toLowerCase(Locale.ROOT).contains(q))
                    .limit(15)
                    .toList();
            return ResponseEntity.ok(filtered);
        } catch (Exception e) {
            logger.error("Model suggestion search failed: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Fetches models dynamically from OpenRouter API + known free-tier endpoints.
     * Cache refreshes every 30 minutes so new models appear without code changes.
     */
    private List<Map<String, String>> getDynamicModelSuggestions() {
        long now = System.currentTimeMillis();
        if (dynamicModelCache != null && (now - cacheTimestamp) < CACHE_TTL_MS) {
            return dynamicModelCache;
        }

        List<Map<String, String>> results = new ArrayList<>();

        // 1) Fetch from OpenRouter API (real-time model catalog)
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://openrouter.ai/api/v1/models"))
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(8))
                    .GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                results.addAll(parseOpenRouterModels(resp.body()));
                logger.info("✅ Fetched {} models from OpenRouter", results.size());
            }
        } catch (Exception e) {
            logger.warn("OpenRouter model fetch failed: {}", e.getMessage());
        }

        // 2) Add well-known free-tier provider endpoints (these are API gateways, not hardcoded models)
        // The actual model IDs come from the OpenRouter fetch above
        addKnownEndpoints(results);

        // 3) Cache the result
        if (!results.isEmpty()) {
            dynamicModelCache = Collections.unmodifiableList(results);
            cacheTimestamp = now;
        } else if (dynamicModelCache != null) {
            // Keep old cache if fetch failed
            logger.info("Using cached model list ({} models)", dynamicModelCache.size());
            return dynamicModelCache;
        }

        return results.isEmpty() ? List.of() : results;
    }

    /** Parse OpenRouter /v1/models response into suggestion format */
    @SuppressWarnings("unchecked")
    private List<Map<String, String>> parseOpenRouterModels(String json) {
        List<Map<String, String>> models = new ArrayList<>();
        try {
            // Simple JSON parsing — extract model entries from {"data": [...]}
            int dataIdx = json.indexOf("\"data\"");
            if (dataIdx < 0) return models;

            // Use basic string parsing for the model IDs and names
            int pos = 0;
            while (pos < json.length()) {
                int idIdx = json.indexOf("\"id\"", pos);
                if (idIdx < 0) break;

                String modelId = extractJsonString(json, idIdx);
                if (modelId == null) { pos = idIdx + 4; continue; }

                String modelName = null;
                int nameIdx = json.indexOf("\"name\"", idIdx);
                int nextIdIdx = json.indexOf("\"id\"", idIdx + 4);
                if (nameIdx > 0 && (nextIdIdx < 0 || nameIdx < nextIdIdx)) {
                    modelName = extractJsonString(json, nameIdx);
                }

                if (modelId.contains("/")) {
                    String provider = modelId.substring(0, modelId.indexOf("/"));
                    String shortModel = modelId.substring(modelId.indexOf("/") + 1);
                    String displayName = modelName != null ? modelName : shortModel;

                    Map<String, String> m = new LinkedHashMap<>();
                    m.put("id", "openrouter-" + shortModel.toLowerCase().replaceAll("[^a-z0-9]", "-"));
                    m.put("name", displayName + " (via OpenRouter)");
                    m.put("provider", capitalize(provider));
                    m.put("model", modelId);
                    m.put("endpoint", "https://openrouter.ai/api/v1/chat/completions");
                    models.add(m);
                }

                pos = idIdx + 4;
            }
        } catch (Exception e) {
            logger.warn("OpenRouter JSON parse error: {}", e.getMessage());
        }
        return models;
    }

    /** Extract a JSON string value after a key position */
    private String extractJsonString(String json, int keyPos) {
        int colonIdx = json.indexOf(":", keyPos);
        if (colonIdx < 0) return null;
        int quoteStart = json.indexOf("\"", colonIdx + 1);
        if (quoteStart < 0 || quoteStart > colonIdx + 10) return null;
        int quoteEnd = json.indexOf("\"", quoteStart + 1);
        if (quoteEnd < 0) return null;
        return json.substring(quoteStart + 1, quoteEnd);
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    /**
     * Add known API gateway endpoints so users can also use direct provider APIs.
     * These are ENDPOINTS (not models) — the model names come from the dynamic fetch.
     */
    private void addKnownEndpoints(List<Map<String, String>> results) {
        // Check if we already have models from these providers via OpenRouter
        Set<String> existingProviders = new HashSet<>();
        for (Map<String, String> m : results) {
            existingProviders.add(m.getOrDefault("provider", "").toLowerCase());
        }

        // Only add direct-API entries for providers not already covered
        if (!existingProviders.contains("groq")) {
            addEndpoint(results, "groq-direct", "Groq (Direct API - Free tier)", "Groq",
                    "Search groq.com/docs for latest models", "https://api.groq.com/openai/v1/chat/completions");
        }
        if (!existingProviders.contains("deepseek")) {
            addEndpoint(results, "deepseek-direct", "DeepSeek (Direct API)", "DeepSeek",
                    "deepseek-chat", "https://api.deepseek.com/v1/chat/completions");
        }
        if (!existingProviders.contains("google")) {
            addEndpoint(results, "google-gemini", "Google Gemini (Direct API)", "Google",
                    "See aistudio.google.com for latest models", "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions");
        }
        if (!existingProviders.contains("cohere")) {
            addEndpoint(results, "cohere-direct", "Cohere (Direct API)", "Cohere",
                    "command-r-plus", "https://api.cohere.com/v2/chat");
        }
    }

    private void addEndpoint(List<Map<String, String>> list,
                              String id, String name, String provider,
                              String model, String endpoint) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("provider", provider);
        m.put("model", model);
        m.put("endpoint", endpoint);
        list.add(m);
    }

    // ─── AirLLM dynamic endpoint management ──────────────────────────────────

    @GetMapping("/airllm/config")
    public ResponseEntity<?> getAirllmConfig() {
        try {
            String endpoint = aiApiService.getDefaultEndpoint("AIRLLM");
            Map<String, Object> config = new LinkedHashMap<>();
            config.put("endpoint", endpoint != null ? endpoint : "");
            config.put("model", "mistralai/Mistral-7B-Instruct-v0.3");
            config.put("status", endpoint != null && !endpoint.isBlank() ? "configured" : "not_configured");
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/airllm/config")
    public ResponseEntity<?> updateAirllmConfig(@RequestBody Map<String, String> body) {
        try {
            String endpoint = body.get("endpoint");
            if (endpoint == null || endpoint.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "endpoint is required"));
            }
            endpoint = endpoint.trim();
            if (!endpoint.startsWith("https://") && !endpoint.startsWith("http://")) {
                return ResponseEntity.badRequest().body(Map.of("error", "endpoint must start with http:// or https://"));
            }
            if (!endpoint.contains("/v1/chat/completions")) {
                endpoint = endpoint.replaceAll("/+$", "") + "/v1/chat/completions";
            }
            aiApiService.updateProviderEndpoint("AIRLLM", endpoint);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "endpoint", endpoint,
                "message", "AirLLM endpoint updated (runtime). No redeploy needed."
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/airllm/health")
    public ResponseEntity<?> checkAirllmHealth() {
        try {
            String endpoint = aiApiService.getDefaultEndpoint("AIRLLM");
            if (endpoint == null || endpoint.isBlank()) {
                return ResponseEntity.ok(Map.of("healthy", false, "error", "No endpoint configured"));
            }
            String healthUrl = endpoint.replace("/v1/chat/completions", "/health");
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(5)).build();
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(healthUrl))
                .timeout(java.time.Duration.ofSeconds(10))
                .GET().build();
            java.net.http.HttpResponse<String> resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
            boolean healthy = resp.statusCode() == 200;
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("healthy", healthy);
            result.put("statusCode", resp.statusCode());
            result.put("healthUrl", healthUrl);
            if (healthy) result.put("body", resp.body());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("healthy", false, "error", e.getMessage()));
        }
    }

    /**
     * POST /api/providers/airllm/auto-refresh
     * Token-protected endpoint for Colab/automation to refresh rotating ngrok URLs.
     */
    @PostMapping("/airllm/auto-refresh")
    public ResponseEntity<?> autoRefreshAirllm(
        @RequestHeader(value = "X-Setup-Token", required = false) String setupToken,
        @RequestBody Map<String, Object> body
    ) {
        String actor = trimToNull(body == null ? null : asString(body.get("actor")));
        String source = trimToNull(body == null ? null : asString(body.get("source")));
        if (actor == null) {
            actor = "airllm-automation";
        }
        if (source == null) {
            source = "colab";
        }

        if (!isSetupTokenAuthorized(setupToken)) {
            providerAuditService.log(
                "AIRLLM_AUTO_REFRESH",
                "AIRLLM",
                actor,
                "ERROR",
                Map.of("reason", "unauthorized", "source", source)
            );
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "Invalid or missing setup token"
            ));
        }

        try {
            String endpoint = normalizeAirllmEndpoint(body);
            if (endpoint == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Provide endpoint or baseUrl"
                ));
            }

            boolean verifyHealth = booleanValue(body.get("verifyHealth"), true);
            boolean rollbackOnFailure = booleanValue(body.get("rollbackOnFailure"), true);
            String previousEndpoint = aiApiService.getDefaultEndpoint("AIRLLM");

            aiApiService.updateProviderEndpoint("AIRLLM", endpoint);
            Map<String, Object> health = verifyHealth ? probeAirllmHealth(endpoint) : Map.of("healthy", true, "skipped", true);
            boolean healthy = Boolean.TRUE.equals(health.get("healthy"));

            if (verifyHealth && !healthy && rollbackOnFailure && previousEndpoint != null && !previousEndpoint.isBlank()) {
                aiApiService.updateProviderEndpoint("AIRLLM", previousEndpoint);
                providerAuditService.log(
                    "AIRLLM_AUTO_REFRESH",
                    "AIRLLM",
                    actor,
                    "ERROR",
                    Map.of(
                        "source", source,
                        "result", "rolled_back",
                        "newEndpoint", endpoint,
                        "previousEndpoint", previousEndpoint,
                        "health", health
                    )
                );
                return ResponseEntity.status(502).body(Map.of(
                    "success", false,
                    "rolledBack", true,
                    "message", "Health check failed; reverted to previous endpoint",
                    "endpoint", previousEndpoint,
                    "failedEndpoint", endpoint,
                    "health", health
                ));
            }

            providerAuditService.log(
                "AIRLLM_AUTO_REFRESH",
                "AIRLLM",
                actor,
                healthy ? "SUCCESS" : "WARNING",
                Map.of(
                    "source", source,
                    "newEndpoint", endpoint,
                    "previousEndpoint", firstNonBlank(previousEndpoint, "none"),
                    "health", health
                )
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "endpoint", endpoint,
                "previousEndpoint", firstNonBlank(previousEndpoint, ""),
                "health", health,
                "message", "AirLLM endpoint refreshed successfully"
            ));
        } catch (Exception e) {
            providerAuditService.log(
                "AIRLLM_AUTO_REFRESH",
                "AIRLLM",
                actor,
                "ERROR",
                Map.of(
                    "source", firstNonBlank(source, "unknown"),
                    "error", firstNonBlank(e.getMessage(), e.getClass().getSimpleName())
                )
            );
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", firstNonBlank(e.getMessage(), "Internal server error")
            ));
        }
    }

    private String normalizeAirllmEndpoint(Map<String, Object> body) {
        String endpoint = trimToNull(asString(body == null ? null : body.get("endpoint")));
        if (endpoint == null) {
            endpoint = trimToNull(asString(body == null ? null : body.get("baseUrl")));
        }
        if (endpoint == null) {
            return null;
        }
        if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
            return null;
        }
        if (!endpoint.contains("/v1/chat/completions")) {
            endpoint = endpoint.replaceAll("/+$", "") + "/v1/chat/completions";
        }
        return endpoint;
    }

    private Map<String, Object> probeAirllmHealth(String endpoint) {
        try {
            String healthUrl = endpoint.replace("/v1/chat/completions", "/health");
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(5)).build();
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(healthUrl))
                .timeout(java.time.Duration.ofSeconds(10))
                .GET().build();
            java.net.http.HttpResponse<String> resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
            boolean healthy = resp.statusCode() == 200;
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("healthy", healthy);
            result.put("statusCode", resp.statusCode());
            result.put("healthUrl", healthUrl);
            if (healthy) {
                result.put("body", resp.body());
            }
            return result;
        } catch (Exception e) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("healthy", false);
            result.put("error", firstNonBlank(e.getMessage(), e.getClass().getSimpleName()));
            return result;
        }
    }

    private boolean isSetupTokenAuthorized(String setupToken) {
        if (setupToken == null || setupToken.isBlank()) {
            return false;
        }
        String expected = System.getenv("SUPREMEAI_SETUP_TOKEN");
        return expected != null && !expected.isBlank() && expected.equals(setupToken);
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean booleanValue(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(text) || "1".equals(text) || "yes".equalsIgnoreCase(text);
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
}
