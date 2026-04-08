package org.example.controller;

import org.example.model.APIProvider;
import org.example.service.AIAPIService;
import org.example.service.FallbackConfigService;
import org.example.service.ProviderAuditService;
import org.example.service.ProviderManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * API Providers Controller
 * Manages AI API provider configurations
 */
@RestController
@RequestMapping("/api/providers")
@CrossOrigin(origins = "*")
public class ProvidersController {
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

    // ─── Model suggestions (curated, no external HTTP) ────────────────────────

    /**
     * GET /api/providers/suggest?q=...
     * Returns a curated static list of well-known free/affordable AI models
     * filtered by the search query.  These are *suggestions only* — they are NOT
     * enabled models.  Admin must save one with an API key to make it active.
     */
    @GetMapping("/suggest")
    public ResponseEntity<?> suggestModels(@RequestParam(name = "q", defaultValue = "") String query) {
        try {
            List<Map<String, String>> all = getCuratedModelSuggestions();
            String q = query.trim().toLowerCase(Locale.ROOT);
            List<Map<String, String>> filtered = q.isEmpty()
                ? all
                : all.stream()
                    .filter(m -> m.get("id").toLowerCase(Locale.ROOT).contains(q)
                              || m.get("name").toLowerCase(Locale.ROOT).contains(q)
                              || m.get("provider").toLowerCase(Locale.ROOT).contains(q))
                    .toList();
            return ResponseEntity.ok(filtered);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /** Curated list of popular / free-tier AI models. Kept in-memory — no external HTTP. */
    private List<Map<String, String>> getCuratedModelSuggestions() {
        List<Map<String, String>> list = new ArrayList<>();
        addSuggestion(list, "groq-llama3-70b",     "Llama 3 70B (Groq - Free tier)",  "Groq",       "meta-llama/Meta-Llama-3-70B-Instruct",     "https://api.groq.com/openai/v1/chat/completions");
        addSuggestion(list, "groq-mixtral",         "Mixtral 8x7B (Groq - Free tier)", "Groq",       "mixtral-8x7b-32768",                       "https://api.groq.com/openai/v1/chat/completions");
        addSuggestion(list, "groq-gemma2-9b",       "Gemma 2 9B (Groq - Free tier)",   "Groq",       "gemma2-9b-it",                             "https://api.groq.com/openai/v1/chat/completions");
        addSuggestion(list, "deepseek-v3",          "DeepSeek V3 (Low cost)",           "DeepSeek",   "deepseek-chat",                            "https://api.deepseek.com/v1/chat/completions");
        addSuggestion(list, "deepseek-r1",          "DeepSeek R1 (Reasoning)",          "DeepSeek",   "deepseek-reasoner",                        "https://api.deepseek.com/v1/chat/completions");
        addSuggestion(list, "google-gemini-flash",  "Gemini 1.5 Flash (Google Free)",   "Google",     "gemini-1.5-flash",                         "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent");
        addSuggestion(list, "google-gemini-pro",    "Gemini 1.5 Pro (Google)",          "Google",     "gemini-1.5-pro",                           "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent");
        addSuggestion(list, "openai-gpt4o-mini",    "GPT-4o Mini (OpenAI cheap)",       "OpenAI",     "gpt-4o-mini",                              "https://api.openai.com/v1/chat/completions");
        addSuggestion(list, "openai-gpt4o",         "GPT-4o (OpenAI)",                  "OpenAI",     "gpt-4o",                                   "https://api.openai.com/v1/chat/completions");
        addSuggestion(list, "anthropic-claude-haiku","Claude 3 Haiku (Anthropic cheap)", "Anthropic", "claude-3-haiku-20240307",                  "https://api.anthropic.com/v1/messages");
        addSuggestion(list, "anthropic-claude-sonnet","Claude 3.5 Sonnet (Anthropic)",  "Anthropic",  "claude-3-5-sonnet-20241022",               "https://api.anthropic.com/v1/messages");
        addSuggestion(list, "together-llama3-8b",   "Llama 3 8B (Together AI Free)",    "Together AI","meta-llama/Meta-Llama-3-8B-Instruct-Turbo","https://api.together.xyz/v1/chat/completions");
        addSuggestion(list, "together-mistral-7b",  "Mistral 7B (Together AI Free)",    "Together AI","mistralai/Mistral-7B-Instruct-v0.2",       "https://api.together.xyz/v1/chat/completions");
        addSuggestion(list, "cohere-command-r",     "Command R (Cohere)",               "Cohere",     "command-r",                                "https://api.cohere.com/v2/chat");
        addSuggestion(list, "perplexity-sonar",     "Sonar (Perplexity)",               "Perplexity", "sonar",                                    "https://api.perplexity.ai/chat/completions");
        addSuggestion(list, "xai-grok-2",           "Grok 2 (xAI)",                     "xAI",        "grok-2-latest",                            "https://api.x.ai/v1/chat/completions");
        addSuggestion(list, "hf-llama3-70b",        "Llama 3 70B (HuggingFace Router)", "HuggingFace","meta-llama/Llama-3.3-70B-Instruct",        "https://router.huggingface.co/v1/chat/completions");
        addSuggestion(list, "openrouter-free",      "Free models via OpenRouter",        "OpenRouter", "openrouter/auto",                          "https://openrouter.ai/api/v1/chat/completions");
        return list;
    }

    private void addSuggestion(List<Map<String, String>> list,
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
