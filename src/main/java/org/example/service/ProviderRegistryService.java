package org.example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import jakarta.annotation.PostConstruct;
import org.example.model.APIProvider;
import org.example.security.SecretManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for configured AI provider definitions.
 *
 * Persistence strategy (in priority order):
 *  1. Firebase Realtime Database – survives Cloud Run cold starts, shared across
 *     all instances.  API keys are stored separately in Secret Manager / env vars.
 *  2. Local JSON file – fallback when Firebase is not initialised (local dev).
 *
 * API key resolution:
 *  - When a provider is saved with an apiKey, the key is stored in Secret Manager
 *    (if the backend is configured) so it persists beyond the ephemeral instance.
 *  - On load, the in-memory {@code apiKeys} Spring bean is refreshed so that
 *    {@link AIAPIService} uses the freshly loaded keys immediately.
 */
@Service
public class ProviderRegistryService {
    private static final Logger logger = LoggerFactory.getLogger(ProviderRegistryService.class);
    private static final String STORE_PATH      = "provider-registry.json";
    private static final String FIREBASE_PATH   = "providers";
    private static final String FIREBASE_SECRETS_PATH = "provider_secrets";
    private static final String SECRET_PREFIX   = "provider-apikey-";

    private final LocalJsonStoreService localJsonStoreService;
    private final Map<String, APIProvider> providers = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;

    // Optional – injected when available (not available in test profile)
    @Autowired(required = false)
    private FirebaseService firebaseService;

    @Autowired(required = false)
    private SecretManager secretManager;

    // Live apiKeys map bean – updated whenever a provider key changes
    @Autowired(required = false)
    private Map<String, String> apiKeys;

    public ProviderRegistryService(LocalJsonStoreService localJsonStoreService) {
        this.localJsonStoreService = localJsonStoreService;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @PostConstruct
    void loadProviders() {
        // Try Firebase first; fall back to local JSON.
        if (firebaseService != null && firebaseService.isInitialized()) {
            loadFromFirebase();
        } else {
            loadFromLocalJson();
        }

        // Warn loudly on startup when no providers are available, since all AI
        // features (self-improvement, project improvement, consensus) depend on
        // having at least one configured provider.
        if (providers.isEmpty()) {
            logger.warn("⚠️⚠️⚠️ NO AI PROVIDERS CONFIGURED — all AI features will be unavailable until " +
                    "an admin adds at least one provider via POST /api/providers");
        } else {
            logger.info("✅ Loaded {} AI provider(s) on startup", providers.size());
        }
    }

    // ── read ──────────────────────────────────────────────────────────────────

    public List<APIProvider> getAllProviders() {
        return providers.values().stream()
            .sorted(Comparator.comparing(APIProvider::getName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    public List<APIProvider> getActiveProviders() {
        return providers.values().stream()
            .filter(p -> p.getStatus() == null || !p.getStatus().equalsIgnoreCase("inactive"))
            .sorted(Comparator.comparing(APIProvider::getName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    public APIProvider getProvider(String id) {
        return providers.get(id);
    }

    public List<String> getActiveProviderIds() {
        List<String> ids = new ArrayList<>();
        for (APIProvider p : getActiveProviders()) {
            ids.add(p.getId());
        }
        return ids;
    }

    public int getActiveProviderCount()  { return getActiveProviders().size(); }
    public int getTotalProviderCount()   { return providers.size(); }

    // ── write ─────────────────────────────────────────────────────────────────

    public APIProvider addOrUpdateProvider(APIProvider provider) {
        APIProvider normalized = normalize(provider);

        // Persist API key to Secret Manager so it survives restarts
        persistApiKeyToSecret(normalized);

        providers.put(normalized.getId(), normalized);
        refreshApiKeysBean(normalized);
        persistProviders();
        return normalized;
    }

    public boolean removeProvider(String id) {
        boolean removed = providers.remove(id) != null;
        if (removed) {
            persistProviders();
            // Clean up Firebase secret backup
            if (firebaseService != null && firebaseService.isInitialized()) {
                try {
                    firebaseService.getDatabase()
                        .getReference(FIREBASE_SECRETS_PATH)
                        .child(sanitizeId(id))
                        .removeValueAsync();
                } catch (Exception e) {
                    logger.warn("⚠️ Could not remove API key from Firebase secrets for {}: {}", id, e.getMessage());
                }
            }
        }
        return removed;
    }

    // ── persistence ───────────────────────────────────────────────────────────

    private void loadFromLocalJson() {
        List<APIProvider> persisted = localJsonStoreService.read(
            STORE_PATH,
            new TypeReference<List<APIProvider>>() {},
            new ArrayList<>()
        );
        providers.clear();
        for (APIProvider p : persisted) {
            APIProvider normalized = normalize(p);
            // Restore key from Secret Manager if available
            restoreApiKeyFromSecret(normalized);
            providers.put(normalized.getId(), normalized);
            refreshApiKeysBean(normalized);
        }
        logger.info("✅ ProviderRegistry: loaded {} provider(s) from local JSON", providers.size());
    }

    private void loadFromFirebase() {
        try {
            DatabaseReference ref = firebaseService.getDatabase().getReference(FIREBASE_PATH);
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    providers.clear();
                    if (snapshot.exists()) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            try {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> raw = (Map<String, Object>) child.getValue();
                                if (raw == null) continue;
                                APIProvider p = mapper.convertValue(raw, APIProvider.class);
                                APIProvider normalized = normalize(p);
                                // Keys are stored in Secret Manager, not Firebase
                                restoreApiKeyFromSecret(normalized);
                                providers.put(normalized.getId(), normalized);
                                refreshApiKeysBean(normalized);
                            } catch (Exception e) {
                                logger.warn("⚠️ Failed to parse provider from Firebase: {}", e.getMessage());
                            }
                        }
                    }
                    logger.info("✅ ProviderRegistry: loaded {} provider(s) from Firebase", providers.size());
                    // Restore API keys from Firebase fallback (covers env/dev deployments
                    // where GCP Secret Manager is not configured)
                    restoreApiKeysFromFirebase();
                    // Also keep local JSON in sync as cold-start fallback
                    localJsonStoreService.write(STORE_PATH, getAllProviders());
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    logger.warn("⚠️ Firebase provider load cancelled: {} – falling back to local JSON",
                        error.getMessage());
                    loadFromLocalJson();
                }
            });
        } catch (Exception e) {
            logger.warn("⚠️ Firebase provider load failed: {} – falling back to local JSON", e.getMessage());
            loadFromLocalJson();
        }
    }

    private void persistProviders() {
        List<APIProvider> all = getAllProviders();

        // Always update local JSON (fast, reliable fallback)
        localJsonStoreService.write(STORE_PATH, all);

        // Persist metadata (without raw API keys) to Firebase
        if (firebaseService != null && firebaseService.isInitialized()) {
            try {
                DatabaseReference ref = firebaseService.getDatabase().getReference(FIREBASE_PATH);
                for (APIProvider p : all) {
                    // Convert to map and redact key before writing to Firebase
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = mapper.convertValue(p, Map.class);
                    data.put("apiKey", null); // never store raw key in Firebase
                    ref.child(p.getId()).setValueAsync(data);
                }
            } catch (Exception e) {
                logger.warn("⚠️ Could not persist providers to Firebase: {}", e.getMessage());
            }
        }
    }

    /**
     * Store the provider's API key in Secret Manager under a deterministic name
     * so it can be retrieved across Cloud Run restarts.
     * Also writes a Firebase RTDB backup so keys survive restarts even when
     * GCP Secret Manager is not configured (SECRET_MANAGER_BACKEND=env).
     */
    private void persistApiKeyToSecret(APIProvider provider) {
        String key = provider.getApiKey();
        if (key == null || key.isBlank()) return;

        if (secretManager != null) {
            try {
                String secretName = SECRET_PREFIX + sanitizeId(provider.getId());
                secretManager.updateSecret(secretName, key);
            } catch (Exception e) {
                logger.warn("⚠️ Could not store API key in Secret Manager for {}: {}", provider.getId(), e.getMessage());
            }
        }

        // Firebase fallback — ensures key survives restarts when GCP Secret Manager is not configured
        if (firebaseService != null && firebaseService.isInitialized()) {
            try {
                firebaseService.getDatabase()
                    .getReference(FIREBASE_SECRETS_PATH)
                    .child(sanitizeId(provider.getId()))
                    .setValueAsync(key);
                logger.debug("🔑 API key backed up to Firebase for provider {}", provider.getId());
            } catch (Exception e) {
                logger.warn("⚠️ Could not back up API key to Firebase for {}: {}", provider.getId(), e.getMessage());
            }
        }
    }

    /**
     * Restore the API key from Secret Manager if the provider record doesn't
     * already carry one (e.g. loaded from Firebase where key is redacted).
     */
    private void restoreApiKeyFromSecret(APIProvider provider) {
        if (secretManager == null) return;
        if (provider.getApiKey() != null && !provider.getApiKey().isBlank()) return;
        try {
            String secretName = SECRET_PREFIX + sanitizeId(provider.getId());
            String key = secretManager.getSecret(secretName);
            if (key != null && !key.isBlank()) {
                provider.setApiKey(key);
            }
        } catch (Exception e) {
            logger.debug("Could not restore API key from Secret Manager for {}: {}", provider.getId(), e.getMessage());
        }
    }

    /**
     * Asynchronously restores API keys from the Firebase {@code provider_secrets} path
     * for providers that were loaded from Firebase without a key (Firebase deliberately
     * stores only metadata, never raw keys).  This is the primary key-recovery path
     * when GCP Secret Manager is not configured ({@code SECRET_MANAGER_BACKEND=env}).
     */
    private void restoreApiKeysFromFirebase() {
        if (firebaseService == null || !firebaseService.isInitialized()) return;
        try {
            firebaseService.getDatabase()
                .getReference(FIREBASE_SECRETS_PATH)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (!snapshot.exists()) return;
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String id = child.getKey();
                            String key = child.getValue(String.class);
                            if (id == null || key == null || key.isBlank()) continue;
                            providers.values().stream()
                                .filter(p -> sanitizeId(p.getId()).equals(id))
                                .forEach(p -> {
                                    if (p.getApiKey() == null || p.getApiKey().isBlank()) {
                                        p.setApiKey(key);
                                        refreshApiKeysBean(p);
                                        logger.debug("🔑 API key restored from Firebase fallback for provider {}", p.getId());
                                    }
                                });
                        }
                        logger.info("✅ ProviderRegistry: API keys restored from Firebase fallback");
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        logger.warn("⚠️ Could not restore API keys from Firebase secrets: {}", error.getMessage());
                    }
                });
        } catch (Exception e) {
            logger.warn("⚠️ Could not restore API keys from Firebase: {}", e.getMessage());
        }
    }

    /**
     * Keep the live {@code apiKeys} Spring bean up to date so that
     * {@link AIAPIService} uses the correct key immediately after a save.
     */
    private void refreshApiKeysBean(APIProvider provider) {
        if (apiKeys == null) return;
        String key = provider.getApiKey();
        if (key == null || key.isBlank()) return;
        // Map the provider's canonical / base-model name to the key
        String canonical = canonicalName(provider);
        if (canonical != null) {
            apiKeys.put(canonical, key);
            logger.debug("🔑 apiKeys bean refreshed: {} → ***", canonical);
        }
    }

    /** Returns the uppercase canonical provider identifier used by AIAPIService.
     *  Handles known names + dynamic fallback for any new provider admin adds. */
    private String canonicalName(APIProvider provider) {
        String id = provider.getId();
        if (id == null) return null;
        String lower = id.toLowerCase();
        // Known canonical IDs (hint-based, not a restriction)
        switch (lower) {
            case "openai-gpt4":    case "gpt4":    return "GPT4";
            case "anthropic-claude": case "claude": return "CLAUDE";
            case "google-gemini":  case "gemini":  return "GEMINI";
            case "deepseek":                       return "DEEPSEEK";
            case "groq":                           return "GROQ";
            case "cohere":                         return "COHERE";
            case "perplexity":                     return "PERPLEXITY";
            case "huggingface":                    return "HUGGINGFACE";
            case "xai":                            return "XAI";
            case "llama":          case "meta":    return "LLAMA";
            default:
                // Dynamic: any new provider admin adds gets its ID uppercased as canonical name
                String base = provider.getBaseModel();
                if (base != null && !base.isBlank()) return base.toUpperCase();
                return id.toUpperCase();
        }
    }

    private static String sanitizeId(String id) {
        return id == null ? "unknown" : id.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    // ── normalization ─────────────────────────────────────────────────────────

    private APIProvider normalize(APIProvider provider) {
        APIProvider normalized = provider == null ? new APIProvider() : provider;

        if (normalized.getId() == null || normalized.getId().isBlank()) {
            normalized.setId(UUID.randomUUID().toString());
        }
        if (normalized.getStatus() == null || normalized.getStatus().isBlank()) {
            normalized.setStatus("active");
        }
        if (normalized.getCapabilities() == null) {
            normalized.setCapabilities(new ArrayList<>());
        }
        if (normalized.getModels() == null) {
            normalized.setModels(new ArrayList<>());
        }
        if (normalized.getName() == null || normalized.getName().isBlank()) {
            normalized.setName("Provider " + normalized.getId());
        }
        if (normalized.getCreatedAt() == null) {
            normalized.setCreatedAt(java.time.LocalDateTime.now());
        }
        if (normalized.getRateLimitPerMinute() == null) {
            normalized.setRateLimitPerMinute(100);
        }
        if (normalized.getMonthlyQuota() == null) {
            normalized.setMonthlyQuota(1000);
        }
        if (normalized.getFreeQuotaPercent() == null) {
            normalized.setFreeQuotaPercent(80);
        }
        if (normalized.getAlertThreshold() == null) {
            normalized.setAlertThreshold(75);
        }

        return normalized;
    }
}