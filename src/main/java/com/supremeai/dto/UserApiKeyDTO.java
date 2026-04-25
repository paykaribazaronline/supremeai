
package com.supremeai.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for UserApiKey.
 * Used to avoid exposing sensitive API key data and optimize query performance.
 */
public class UserApiKeyDTO {
    private String id;
    private String userId;
    private String provider;
    private String label;
    private String maskedKey;
    private String baseUrl;
    private List<String> models;
    private String status;
    private Long requestCount;
    private Double estimatedCost;
    private LocalDateTime addedAt;
    private LocalDateTime lastTested;
    private LocalDateTime lastUsed;
    private LocalDateTime rotationDueAt;
    private boolean needsRotation;

    // Constructors
    public UserApiKeyDTO() {}

    public UserApiKeyDTO(String id, String userId, String provider, String label, String maskedKey,
                        String status, Long requestCount, Double estimatedCost, boolean needsRotation) {
        this.id = id;
        this.userId = userId;
        this.provider = provider;
        this.label = label;
        this.maskedKey = maskedKey;
        this.status = status;
        this.requestCount = requestCount;
        this.estimatedCost = estimatedCost;
        this.needsRotation = needsRotation;
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

    public String getMaskedKey() { return maskedKey; }
    public void setMaskedKey(String maskedKey) { this.maskedKey = maskedKey; }

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

    public boolean isNeedsRotation() { return needsRotation; }
    public void setNeedsRotation(boolean needsRotation) { this.needsRotation = needsRotation; }
}
