package org.example.service;

import org.example.model.APIProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Computes actionable system alerts that need admin attention.
 * Each alert includes a Bangla "how-to" guide so admins know what to do.
 */
@Service
public class SystemAlertsService {

    @Autowired
    private ProviderRegistryService providerRegistryService;

    @Autowired
    private FallbackConfigService fallbackConfigService;

    /**
     * Compute and return all current system alerts.
     * Each alert: { id, level, title, message, banglaGuide, action, timestamp }
     */
    public List<Map<String, Object>> computeAlerts() {
        List<Map<String, Object>> alerts = new ArrayList<>();

        List<APIProvider> all = providerRegistryService.getAllProviders();
        List<APIProvider> active = providerRegistryService.getActiveProviders();

        // Alert: No providers configured at all
        if (all.isEmpty()) {
            alerts.add(alert(
                "no-providers",
                "critical",
                "কোনো AI মডেল যোগ করা হয়নি",
                "No AI provider has been configured. The system cannot call any AI model.",
                "👉 কীভাবে ঠিক করবেন:\n" +
                "১. বাম দিকের মেনু থেকে 'API Key Manager' এ যান।\n" +
                "২. '➕ Add New API Key' বাটনে ক্লিক করুন।\n" +
                "৩. যেকোনো একটি AI Provider বেছে নিন (যেমন Groq - বিনামূল্যে পাওয়া যায়)।\n" +
                "৪. API Key দিন এবং Save করুন।\n" +
                "৫. Groq-এর জন্য বিনামূল্যে key পেতে যান: https://console.groq.com",
                "go_to_api_keys"
            ));
            return alerts; // Further checks meaningless without any provider
        }

        // Alert: No ACTIVE providers (all inactive/error)
        if (active.isEmpty()) {
            alerts.add(alert(
                "no-active-providers",
                "critical",
                "সব AI মডেল নিষ্ক্রিয়",
                "All configured providers are inactive or in error state. AI calls will fail.",
                "👉 কীভাবে ঠিক করবেন:\n" +
                "১. 'API Key Manager' এ যান।\n" +
                "২. Error দেখানো Provider গুলো দেখুন।\n" +
                "৩. 'Test' বাটন দিয়ে কোনটা কাজ করছে চেক করুন।\n" +
                "৪. প্রয়োজনে নতুন API Key দিয়ে 'Rotate' করুন।",
                "go_to_api_keys"
            ));
        }

        // Alert: Providers with error status
        List<String> errorProviders = all.stream()
            .filter(p -> "error".equalsIgnoreCase(p.getStatus()))
            .map(p -> firstNonBlank(p.getName(), p.getId()))
            .toList();
        if (!errorProviders.isEmpty()) {
            alerts.add(alert(
                "providers-in-error",
                "warning",
                "কিছু AI মডেলে সমস্যা আছে",
                "These providers are in error state: " + String.join(", ", errorProviders),
                "👉 কীভাবে ঠিক করবেন:\n" +
                "১. 'API Key Manager' এ যান।\n" +
                "২. লাল 'error' দেখানো Provider খুঁজুন।\n" +
                "৩. 'Test' বাটন চাপুন — কারণ দেখাবে।\n" +
                "৪. API Key মেয়াদ শেষ হলে নতুন Key দিয়ে 'Rotate' করুন।\n" +
                "৫. Provider এর ওয়েবসাইটে গিয়ে নতুন Key তৈরি করুন।",
                "go_to_api_keys"
            ));
        }

        // Alert: Providers without an API key stored
        List<String> noKeyProviders = active.stream()
            .filter(p -> p.getApiKey() == null || p.getApiKey().isBlank())
            .filter(p -> p.getEndpoint() == null || p.getEndpoint().isBlank())
            .map(p -> firstNonBlank(p.getName(), p.getId()))
            .toList();
        if (!noKeyProviders.isEmpty()) {
            alerts.add(alert(
                "providers-missing-key",
                "warning",
                "কিছু Provider-এ API Key নেই",
                "These providers have no API key or endpoint stored: " + String.join(", ", noKeyProviders),
                "👉 কীভাবে ঠিক করবেন:\n" +
                "১. 'API Key Manager' এ যান।\n" +
                "২. সংশ্লিষ্ট Provider এ ক্লিক করুন এবং API Key যোগ করুন।",
                "go_to_api_keys"
            ));
        }

        // Alert: Fallback chain configured but references unknown/inactive providers
        List<String> configuredChain = fallbackConfigService.getFallbackChain();
        if (!configuredChain.isEmpty()) {
            List<String> activeIds = active.stream().map(APIProvider::getId).toList();
            List<String> broken = configuredChain.stream()
                .filter(id -> !activeIds.contains(id))
                .toList();
            if (!broken.isEmpty()) {
                alerts.add(alert(
                    "fallback-chain-broken",
                    "warning",
                    "Fallback Chain-এ অকার্যকর Provider আছে",
                    "These provider IDs in your fallback chain are not active: " + String.join(", ", broken),
                    "👉 কীভাবে ঠিক করবেন:\n" +
                    "১. 'API Key Manager' পেজে নিচে 'Fallback Chain' সেকশনে যান।\n" +
                    "২. অকার্যকর Provider গুলো সরিয়ে দিন অথবা সক্রিয় Provider দিয়ে প্রতিস্থাপন করুন।",
                    "go_to_fallback_chain"
                ));
            }
        }

        // Alert: No fallback chain at all and only 1 active provider
        if (configuredChain.isEmpty() && active.size() == 1) {
            alerts.add(alert(
                "single-provider-no-fallback",
                "info",
                "মাত্র একটি AI Provider সক্রিয়",
                "Only one provider is active and no fallback chain is configured. If it fails, all AI calls will fail.",
                "👉 পরামর্শ:\n" +
                "আরও একটি AI Provider যোগ করুন (যেমন Groq বিনামূল্যে পাওয়া যায়)।\n" +
                "তারপর 'Fallback Chain' সেট করুন যাতে প্রথমটি ব্যর্থ হলে দ্বিতীয়টি ব্যবহার হয়।",
                "go_to_api_keys"
            ));
        }

        return alerts;
    }

    public int countCriticalAlerts() {
        return (int) computeAlerts().stream()
            .filter(a -> "critical".equals(a.get("level")))
            .count();
    }

    public int countTotalAlerts() {
        return computeAlerts().size();
    }

    private Map<String, Object> alert(String id, String level, String title,
                                       String message, String banglaGuide, String action) {
        Map<String, Object> a = new LinkedHashMap<>();
        a.put("id", id);
        a.put("level", level);         // critical / warning / info
        a.put("title", title);
        a.put("message", message);
        a.put("banglaGuide", banglaGuide);
        a.put("action", action);
        a.put("timestamp", LocalDateTime.now().toString());
        return a;
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
