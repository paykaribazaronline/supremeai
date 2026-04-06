package org.example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.PostConstruct;
import org.example.model.APIProvider;
import org.example.model.APIProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProviderRegistryService {
    private static final String STORE_PATH = "provider-registry.json";

    private final LocalJsonStoreService localJsonStoreService;
    private final Map<String, APIProvider> providers = new ConcurrentHashMap<>();

    public ProviderRegistryService(LocalJsonStoreService localJsonStoreService) {
        this.localJsonStoreService = localJsonStoreService;
    }

    @PostConstruct
    void loadProviders() {
        List<APIProvider> persistedProviders = localJsonStoreService.read(
            STORE_PATH,
            new TypeReference<List<APIProvider>>() {},
            new ArrayList<>()
        );
        providers.clear();
        for (APIProvider provider : persistedProviders) {
            APIProvider normalized = normalize(provider);
            providers.put(normalized.getId(), normalized);
        }
    }

    public List<APIProvider> getAllProviders() {
        return providers.values().stream()
            .sorted(Comparator.comparing(APIProvider::getName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    public List<APIProvider> getActiveProviders() {
        return providers.values().stream()
            .filter(provider -> provider.getStatus() == null || !provider.getStatus().equalsIgnoreCase("inactive"))
            .sorted(Comparator.comparing(APIProvider::getName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    public APIProvider getProvider(String id) {
        return providers.get(id);
    }

    public APIProvider addOrUpdateProvider(APIProvider provider) {
        APIProvider normalized = normalize(provider);
        providers.put(normalized.getId(), normalized);
        persistProviders();
        return normalized;
    }

    public boolean removeProvider(String id) {
        boolean removed = providers.remove(id) != null;
        if (removed) {
            persistProviders();
        }
        return removed;
    }

    public List<String> getActiveProviderIds() {
        List<String> ids = new ArrayList<>();
        for (APIProvider provider : getActiveProviders()) {
            ids.add(provider.getId());
        }
        return ids;
    }

    public int getActiveProviderCount() {
        return getActiveProviders().size();
    }

    public int getTotalProviderCount() {
        return providers.size();
    }

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
        if ("airllm-local".equalsIgnoreCase(normalized.getId()) && normalized.getRateLimitPerMinute() == null) {
            normalized.setRateLimitPerMinute(10);
        }

        return normalized;
    }

    private void persistProviders() {
        localJsonStoreService.write(STORE_PATH, getAllProviders());
    }
}