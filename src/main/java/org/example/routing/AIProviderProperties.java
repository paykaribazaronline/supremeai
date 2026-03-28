package org.example.routing;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Externalized configuration for every AI provider.
 *
 * <p>Properties are grouped under {@code ai.providers.<name>.*} so adding a new
 * provider only requires new properties, not code changes.
 *
 * <p>Example {@code application.properties} snippet:
 * <pre>
 * ai.priority.order=kimi,deepseek,gemini
 *
 * ai.providers.kimi.base-url=https://api.moonshot.cn/v1/chat/completions
 * ai.providers.kimi.model=kimi-k2.5
 * ai.providers.kimi.api-key=${AI_PROVIDERS_KIMI_API_KEY:}
 * ai.providers.kimi.enabled=true
 *
 * ai.providers.deepseek.base-url=https://api.deepseek.com/v1/chat/completions
 * ai.providers.deepseek.model=deepseek-coder
 * ai.providers.deepseek.api-key=${AI_PROVIDERS_DEEPSEEK_API_KEY:}
 * ai.providers.deepseek.enabled=true
 *
 * ai.providers.gemini.base-url=https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent
 * ai.providers.gemini.model=gemini-pro
 * ai.providers.gemini.api-key=${AI_PROVIDERS_GEMINI_API_KEY:}
 * ai.providers.gemini.enabled=true
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "ai")
public class AIProviderProperties {

    /** Default fallback routing order when no config is present. */
    private static final String DEFAULT_ORDER = "kimi,deepseek,gemini";

    /** Comma-separated priority order, e.g. "kimi,deepseek,gemini". */
    private String priorityOrder = DEFAULT_ORDER;

    /** Per-provider config keyed by lower-case provider name. */
    private Map<String, ProviderConfig> providers = new LinkedHashMap<>();

    // ---- root getters/setters ----

    public String getPriorityOrder() {
        return priorityOrder;
    }

    public void setPriorityOrder(String priorityOrder) {
        this.priorityOrder = (priorityOrder != null && !priorityOrder.isBlank())
                ? priorityOrder
                : DEFAULT_ORDER;
    }

    public Map<String, ProviderConfig> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, ProviderConfig> providers) {
        this.providers = providers != null ? providers : new LinkedHashMap<>();
    }

    // ---- inner config class ----

    public static class ProviderConfig {

        private String baseUrl = "";
        private String model = "";
        /** API key — sourced from env var or Secret Manager; never logged. */
        private String apiKey = "";
        private boolean enabled = true;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
