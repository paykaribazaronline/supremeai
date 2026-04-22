package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Per-user API key stored in Firestore.
 * Each user can have multiple API keys for different providers.
 */
@Document(collectionName = "user_api_keys")
public class UserApiKey {

    @DocumentId
    private String id;

    private String userId;          // Firebase UID of the owner
    private String provider;        // e.g. "OpenAI", "Google AI", "Anthropic", "Groq"
    private String label;           // e.g. "Production Key", "Dev Key"
    private String apiKey;          // The actual API key (encrypted at rest)
    private String baseUrl;         // Optional override URL
    private List<String> models;    // Optional model restrictions
    private String status;          // "active", "inactive", "error"
    private Long requestCount;      // Total requests made with this key
    private Double estimatedCost;   // Estimated cost in USD
    private LocalDateTime addedAt;
    private LocalDateTime lastTested;
    private LocalDateTime lastUsed;
    private LocalDateTime rotationDueAt; // When this key should be rotated

    public UserApiKey() {}

    public UserApiKey(String userId, String provider, String label, String apiKey) {
        this.userId = userId;
        this.provider = provider;
        this.label = label;
        this.apiKey = apiKey;
        this.status = "active";
        this.requestCount = 0L;
        this.estimatedCost = 0.0;
        this.addedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public List<String> getModels() { return models; }
    public void setModels(List<String> models) { this.models = models; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getRequestCount() { return requestCount; }
    public void setRequestCount(Long requestCount) { this.requestCount = requestCount; }

    public Double getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(Double estimatedCost) { this.estimatedCost = estimatedCost; }

    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }

    public LocalDateTime getLastTested() { return lastTested; }
    public void setLastTested(LocalDateTime lastTested) { this.lastTested = lastTested; }

    public LocalDateTime getLastUsed() { return lastUsed; }
    public void setLastUsed(LocalDateTime lastUsed) { this.lastUsed = lastUsed; }

    public LocalDateTime getRotationDueAt() { return rotationDueAt; }
    public void setRotationDueAt(LocalDateTime rotationDueAt) { this.rotationDueAt = rotationDueAt; }

    /**
     * Returns a masked version of the API key for display.
     */
    public String getMaskedKey() {
        if (apiKey == null || apiKey.length() <= 8) return "********";
        return apiKey.substring(0, 4) + "********" + apiKey.substring(apiKey.length() - 4);
    }

    /**
     * Record a single usage of this key.
     */
    public void recordUsage(double cost) {
        this.requestCount = (this.requestCount != null ? this.requestCount : 0L) + 1;
        this.estimatedCost = (this.estimatedCost != null ? this.estimatedCost : 0.0) + cost;
        this.lastUsed = LocalDateTime.now();
    }

    /**
     * Check if this key needs rotation.
     */
    public boolean needsRotation() {
        return rotationDueAt != null && LocalDateTime.now().isAfter(rotationDueAt);
    }
}
