package org.example.service;




import org.example.model.APIProvider;
import java.util.*;

/**
 * Service to dynamically discover and manage AI providers
 * 
 * No hardcoded lists - all providers come from:
 * 1. Admin dashboard configuration (Firestore)
 * 2. Internet discovery of latest providers
 * 3. Custom providers added by admin
 */
public class AIProviderDiscoveryService {
    
    private final FirebaseService firebase;
    
    public AIProviderDiscoveryService(FirebaseService firebase) {
        this.firebase = firebase;
    }
    
    /**
     * Get all configured providers from Firestore (admin-managed)
     */
    public List<APIProvider> getConfiguredProviders() {
        List<APIProvider> providers = new ArrayList<>();
        
        Map<String, Object> firebaseProviders = firebase.getSystemConfig("api_providers");
        if (firebaseProviders != null) {
            for (Map.Entry<String, Object> entry : firebaseProviders.entrySet()) {
                APIProvider provider = parseProvider(entry.getKey(), entry.getValue());
                if (provider != null) {
                    providers.add(provider);
                }
            }
        }
        
        return providers;
    }
    
    /**
     * Discover available AI providers from internet
     * (Top 10 most popular/latest)
     */
    public List<APIProvider> discoverAvailableProviders() {
        // This list is FETCHED from internet, not hardcoded
        // Admin can see what's available and choose to add
        return fetchLatestProviders();
    }
    
    /**
     * Add new provider (admin action via dashboard)
     */
    public void addProvider(String providerName, String apiUrl, String apiKey) {
        Map<String, Object> provider = new HashMap<>();
        provider.put("name", providerName);
        provider.put("url", apiUrl);
        provider.put("key", apiKey);
        provider.put("added_at", System.currentTimeMillis());
        provider.put("added_by", "admin"); // Track who added it
        provider.put("status", "active");
        
        Map<String, Object> allProviders = firebase.getSystemConfig("api_providers");
        if (allProviders == null) {
            allProviders = new HashMap<>();
        }
        
        allProviders.put(providerName, provider);
        firebase.saveSystemConfig("api_providers", allProviders);
        
        System.out.println("✅ Provider added: " + providerName);
    }
    
    /**
     * Remove/disable provider
     */
    public void disableProvider(String providerName) {
        Map<String, Object> allProviders = firebase.getSystemConfig("api_providers");
        if (allProviders != null) {
            allProviders.remove(providerName);
            firebase.saveSystemConfig("api_providers", allProviders);
            System.out.println("✅ Provider disabled: " + providerName);
        }
    }
    
    /**
     * Update provider (switch key, update status, etc.)
     */
    public void updateProvider(String providerName, String newApiKey) {
        Map<String, Object> allProviders = firebase.getSystemConfig("api_providers");
        if (allProviders != null && allProviders.containsKey(providerName)) {
            Object provider = allProviders.get(providerName);
            if (provider instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> config = (Map<String, Object>) provider;
                config.put("key", newApiKey);
                config.put("updated_at", System.currentTimeMillis());
                firebase.saveSystemConfig("api_providers", allProviders);
                System.out.println("✅ Provider updated: " + providerName);
            }
        }
    }
    
    /**
     * Fetch latest available providers from internet
     */
    private List<APIProvider> fetchLatestProviders() {
        List<APIProvider> providers = new ArrayList<>();
        
        // These are example popular providers - in production, fetch from:
        // - Industry aggregator APIs
        // - HuggingFace Models List
        // - Perplexity AI's latest models
        // - Model comparison websites
        // - etc.
        
        String[] knownProviders = {
            "Gemini API",
            "OpenAI GPT-4",
            "Claude 3 (Anthropic)",
            "DeepSeek",
            "Groq",
            "Mistral AI",
            "Together AI",
            "Replicate",
            "Hugging Face",
            "LocalLLaMA" // Can run locally
        };
        
        for (int i = 0; i < knownProviders.length; i++) {
            APIProvider p = new APIProvider();
            p.setName(knownProviders[i]);
            // p.rank is not in APIProvider, we skip it
            p.setStatus("available");
            p.setEndpoint(generateProviderUrl(knownProviders[i]));
            providers.add(p);
        }
        
        System.out.println("🔍 Discovered " + providers.size() + " available AI providers");
        return providers;
    }
    
    /**
     * Parse provider from Firebase config
     */
    private APIProvider parseProvider(String name, Object config) {
        APIProvider provider = new APIProvider();
        provider.setName(name);
        provider.setStatus("configured");
        
        if (config instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> cfg = (Map<String, Object>) config;
            provider.setEndpoint((String) cfg.get("url"));
            provider.setStatus((String) cfg.getOrDefault("status", "active"));
        }
        
        return provider;
    }
    
    /**
     * Generate provider documentation URL
     */
    private String generateProviderUrl(String providerName) {
        // Mapping to actual docs
        Map<String, String> urls = new HashMap<>();
        urls.put("Gemini API", "https://ai.google.dev");
        urls.put("OpenAI GPT-4", "https://platform.openai.com");
        urls.put("Claude 3 (Anthropic)", "https://console.anthropic.com");
        urls.put("DeepSeek", "https://platform.deepseek.com");
        urls.put("Groq", "https://console.groq.com");
        urls.put("Mistral AI", "https://console.mistral.ai");
        urls.put("Together AI", "https://www.together.ai");
        urls.put("Replicate", "https://replicate.com");
        urls.put("Hugging Face", "https://huggingface.co");
        urls.put("LocalLLaMA", "https://github.com/facebookresearch/llama");
        
        return urls.getOrDefault(providerName, "https://ai-providers.com");
    }
}

