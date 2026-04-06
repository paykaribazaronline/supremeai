package org.example.service;

import org.example.model.APIProvider;
import org.example.model.ProviderAuditEvent;
import org.example.model.Quota;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProviderManagementService {

    @Autowired
    private ProviderRegistryService providerRegistryService;

    @Autowired
    private AIAPIService aiApiService;

    @Autowired
    private QuotaService quotaService;

    @Autowired
    private ProviderAuditService providerAuditService;

    /**
     * Return only providers that the admin has saved to the registry (data-driven).
     * Hard-coded canonical model names are no longer exposed here.
     * The admin uses the suggestion search in the UI to discover new providers.
     */
    public List<Map<String, Object>> getAvailableProviders() {
        List<Map<String, Object>> providers = new ArrayList<>();
        for (APIProvider provider : providerRegistryService.getAllProviders()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("providerId", provider.getId());
            row.put("displayName", firstNonBlank(provider.getName(),
                aiApiService.getProviderDisplayName(provider.getId()), provider.getId()));
            row.put("baseModel", provider.getBaseModel());
            row.put("configured", true);
            row.put("saved", true);
            row.put("status", firstNonBlank(provider.getStatus(), "active"));
            row.put("nativeConnector", aiApiService.hasNativeConnector(provider.getId()));
            providers.add(row);
        }
        return providers;
    }

    public List<Map<String, Object>> getConfiguredProviders() {
        List<Map<String, Object>> providers = new ArrayList<>();
        for (APIProvider provider : providerRegistryService.getAllProviders()) {
            providers.add(toResponse(provider));
        }
        return providers;
    }

    public Map<String, Object> saveProvider(APIProvider provider) {
        APIProvider existing = provider != null && provider.getId() != null
            ? providerRegistryService.getProvider(provider.getId())
            : null;
        APIProvider saved = providerRegistryService.addOrUpdateProvider(mergeWithExisting(provider, existing));
        quotaService.syncConfiguredProviders();
        providerAuditService.log(
            existing == null ? "CREATE_PROVIDER" : "UPDATE_PROVIDER",
            saved.getId(),
            saved.getCreatedByEmail(),
            "SUCCESS",
            Map.of("displayName", saved.getName(), "endpoint", firstNonBlank(saved.getEndpoint(), "default"))
        );
        return toResponse(saved);
    }

    public Map<String, Object> updateProvider(String id, APIProvider provider) {
        APIProvider incoming = provider == null ? new APIProvider() : provider;
        incoming.setId(id);
        return saveProvider(incoming);
    }

    public Map<String, Object> rotateProvider(String id, Map<String, String> rotationRequest) throws IOException {
        APIProvider provider = providerRegistryService.getProvider(id);
        if (provider == null) {
            throw new IOException("Provider not found: " + id);
        }

        String replacementKey = trimToNull(rotationRequest == null ? null : rotationRequest.get("apiKey"));
        if (replacementKey == null) {
            throw new IOException("Replacement API key is required");
        }

        String rotationReason = trimToNull(rotationRequest.get("reason"));
        String rotationEmail = trimToNull(rotationRequest.get("alertEmail"));
        String replacementAlias = trimToNull(rotationRequest.get("alias"));
        String replacementEndpoint = trimToNull(rotationRequest.get("endpoint"));

        provider.setApiKey(replacementKey);
        if (replacementAlias != null) {
            provider.setAlias(replacementAlias);
        }
        if (replacementEndpoint != null) {
            provider.setEndpoint(replacementEndpoint);
        }
        provider.setStatus("active");
        provider.setLastError(null);
        provider.setNotes(buildRotationNote(provider.getNotes(), rotationReason, rotationEmail));

        APIProvider saved = providerRegistryService.addOrUpdateProvider(provider);
        quotaService.syncConfiguredProviders();
        providerAuditService.log(
            "ROTATE_KEY",
            saved.getId(),
            firstNonBlank(rotationEmail, saved.getCreatedByEmail()),
            "SUCCESS",
            Map.of("reason", firstNonBlank(rotationReason, "unspecified"), "alias", firstNonBlank(saved.getAlias(), "default"))
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("provider", toResponse(saved));
        response.put("rotationReason", rotationReason);
        response.put("alertEmail", rotationEmail);
        response.put("rotatedAt", LocalDateTime.now());
        return response;
    }

    public Map<String, Object> probeProvider(String id) throws IOException {
        APIProvider provider = providerRegistryService.getProvider(id);
        if (provider == null) {
            throw new IOException("Provider not found: " + id);
        }

        if (!isProviderReady(provider)) {
            throw new IOException("Provider is missing required endpoint or API key");
        }

        String targetProvider = firstNonBlank(provider.getId(), provider.getBaseModel(), provider.getAlias(), provider.getName());
        String probeEndpoint = firstNonBlank(provider.getHealthCheckUrl(), provider.getEndpoint());
        try {
            String response = aiApiService.probeProviderConnection(
                targetProvider,
                hasStoredCredentials(provider) ? provider.getApiKey() : null,
                probeEndpoint
            );
            provider.setLastTested(LocalDateTime.now());
            provider.setLastError(null);
            provider.setStatus("active");
            provider.setSuccessCount(provider.getSuccessCount() + 1);
            providerRegistryService.addOrUpdateProvider(provider);
            quotaService.recordUsage(provider.getId(), estimateTokenUsage(response));
            providerAuditService.log(
                "PROBE_PROVIDER",
                provider.getId(),
                firstNonBlank(provider.getCreatedByEmail(), "system"),
                "SUCCESS",
                Map.of("responsePreview", abbreviate(response, 80))
            );

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("status", provider.getStatus());
            result.put("provider", toResponse(provider));
            result.put("responsePreview", abbreviate(response, 160));
            return result;
        } catch (IOException ex) {
            provider.setLastTested(LocalDateTime.now());
            provider.setLastError(ex.getMessage());
            provider.setStatus("error");
            provider.setErrorCount(provider.getErrorCount() + 1);
            providerRegistryService.addOrUpdateProvider(provider);
            providerAuditService.log(
                "PROBE_PROVIDER",
                provider.getId(),
                firstNonBlank(provider.getCreatedByEmail(), "system"),
                "ERROR",
                Map.of("message", ex.getMessage())
            );
            throw ex;
        }
    }

    public boolean removeProvider(String id, String actedBy) {
        boolean removed = providerRegistryService.removeProvider(id);
        if (removed) {
            quotaService.syncConfiguredProviders();
            providerAuditService.log("REMOVE_PROVIDER", id, actedBy, "SUCCESS", Map.of());
        }
        return removed;
    }

    public List<ProviderAuditEvent> getAuditEvents(int limit) {
        return providerAuditService.getRecentEvents(limit);
    }

    private APIProvider mergeWithExisting(APIProvider provider, APIProvider existing) {
        APIProvider merged = provider == null ? new APIProvider() : provider;
        String canonicalId = aiApiService.getCanonicalProviderId(firstNonBlank(
            merged.getId(),
            merged.getBaseModel(),
            merged.getAlias(),
            merged.getName()
        ));

        if (canonicalId != null && !canonicalId.isBlank()) {
            merged.setId(canonicalId);
        }

        if (merged.getName() == null || merged.getName().isBlank()) {
            merged.setName(aiApiService.getProviderDisplayName(firstNonBlank(merged.getId(), canonicalId)));
        }

        if (merged.getBaseModel() == null || merged.getBaseModel().isBlank()) {
            merged.setBaseModel(aiApiService.normalizeModelName(firstNonBlank(merged.getId(), merged.getAlias(), merged.getName())));
        } else {
            merged.setBaseModel(aiApiService.normalizeModelName(merged.getBaseModel()));
        }

        if ((merged.getApiKey() == null || merged.getApiKey().isBlank()) && existing != null) {
            merged.setApiKey(existing.getApiKey());
        }
        if ((merged.getEndpoint() == null || merged.getEndpoint().isBlank()) && existing != null) {
            merged.setEndpoint(existing.getEndpoint());
        }
        if ((merged.getAlias() == null || merged.getAlias().isBlank()) && existing != null) {
            merged.setAlias(existing.getAlias());
        }
        if ((merged.getHealthCheckUrl() == null || merged.getHealthCheckUrl().isBlank()) && existing != null) {
            merged.setHealthCheckUrl(existing.getHealthCheckUrl());
        }
        if ((merged.getCapabilities() == null || merged.getCapabilities().isEmpty()) && existing != null) {
            merged.setCapabilities(existing.getCapabilities());
        }
        if ((merged.getComplexityTier() == null || merged.getComplexityTier().isBlank()) && existing != null) {
            merged.setComplexityTier(existing.getComplexityTier());
        }
        if ((merged.getCreatedByEmail() == null || merged.getCreatedByEmail().isBlank()) && existing != null) {
            merged.setCreatedByEmail(existing.getCreatedByEmail());
        }
        if (merged.getCreatedAt() == null && existing != null) {
            merged.setCreatedAt(existing.getCreatedAt());
        }
        if (merged.getLastTested() == null && existing != null) {
            merged.setLastTested(existing.getLastTested());
        }
        if (merged.getLastError() == null && existing != null) {
            merged.setLastError(existing.getLastError());
        }
        if (merged.getSuccessCount() == 0 && existing != null) {
            merged.setSuccessCount(existing.getSuccessCount());
        }
        if (merged.getErrorCount() == 0 && existing != null) {
            merged.setErrorCount(existing.getErrorCount());
        }
        if ((merged.getModels() == null || merged.getModels().isEmpty()) && merged.getBaseModel() != null) {
            merged.setModels(List.of(merged.getBaseModel()));
        }

        return merged;
    }

    private Map<String, Object> toResponse(APIProvider provider) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", provider.getId());
        response.put("name", provider.getName());
        response.put("displayName", aiApiService.getProviderDisplayName(provider.getId()));
        response.put("baseModel", provider.getBaseModel());
        response.put("type", provider.getType());
        response.put("endpoint", provider.getEndpoint());
        response.put("healthCheckUrl", provider.getHealthCheckUrl());
        response.put("alias", provider.getAlias());
        response.put("notes", provider.getNotes());
        response.put("capabilities", provider.getCapabilities());
        response.put("complexityTier", provider.getComplexityTier());
        response.put("createdByEmail", provider.getCreatedByEmail());
        response.put("createdAt", provider.getCreatedAt());
        response.put("models", provider.getModels());
        response.put("status", provider.getStatus());
        response.put("lastTested", provider.getLastTested());
        response.put("lastError", provider.getLastError());
        response.put("errorCount", provider.getErrorCount());
        response.put("successCount", provider.getSuccessCount());
        response.put("rateLimitPerMinute", provider.getRateLimitPerMinute());
        response.put("monthlyQuota", provider.getMonthlyQuota());
        response.put("freeQuotaPercent", provider.getFreeQuotaPercent());
        response.put("alertThreshold", provider.getAlertThreshold());
        Quota quota = quotaService.getQuotaDetails(provider.getId());
        response.put("requestsUsedThisMonth", quota == null ? 0 : quota.getRequestsUsedThisMonth());
        response.put("monthlyUsagePercent", quota == null ? 0.0 : quota.getUsagePercentage());
        response.put("remainingMonthlyRequests", quota == null ? null : quota.getRemainingMonthlyRequests());
        response.put("hasApiKey", hasStoredCredentials(provider));
        response.put("hasEndpoint", hasEndpoint(provider));
        response.put("requiresApiKey", aiApiService.requiresApiKey(provider.getId()));
        response.put("ready", isProviderReady(provider));
        response.put("maskedApiKey", maskApiKey(provider.getApiKey()));
        response.put("nativeConnector", aiApiService.hasNativeConnector(provider.getId()));
        response.put("configured", aiApiService.isProviderConfigured(provider.getId()) || isProviderReady(provider));
        return response;
    }

    private boolean hasStoredCredentials(APIProvider provider) {
        return provider != null && provider.getApiKey() != null && !provider.getApiKey().isBlank();
    }

    private boolean hasEndpoint(APIProvider provider) {
        return provider != null && provider.getEndpoint() != null && !provider.getEndpoint().isBlank();
    }

    private boolean isProviderReady(APIProvider provider) {
        if (provider == null) {
            return false;
        }

        String providerRef = firstNonBlank(provider.getId(), provider.getBaseModel(), provider.getAlias(), provider.getName());
        if (providerRef == null) {
            return false;
        }

        if (aiApiService.requiresApiKey(providerRef)) {
            return hasStoredCredentials(provider) || aiApiService.isProviderConfigured(providerRef);
        }

        return hasStoredCredentials(provider)
            || hasEndpoint(provider)
            || aiApiService.isProviderConfigured(providerRef);
    }

    private String buildRotationNote(String existingNotes, String reason, String alertEmail) {
        StringBuilder note = new StringBuilder(existingNotes == null ? "" : existingNotes.trim());
        String rotationEntry = "Rotated on " + LocalDateTime.now()
            + (reason == null ? "" : " | reason: " + reason)
            + (alertEmail == null ? "" : " | alert: " + alertEmail);

        if (!note.isEmpty()) {
            note.append(System.lineSeparator());
        }
        note.append(rotationEntry);
        return note.toString();
    }

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        if (apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4);
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    private long estimateTokenUsage(String response) {
        if (response == null || response.isBlank()) {
            return 50;
        }
        return Math.max(50, response.length() / 4L);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }
}