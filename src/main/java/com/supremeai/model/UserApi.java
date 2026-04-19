package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import java.time.LocalDateTime;

@Document(collectionName = "user_apis")
public class UserApi {

    @DocumentId
    private String id;

    private String userId;

    private String apiName;

    private String apiKey;

    private String description;

    private UserTier userTier;

    private Long monthlyQuota;

    private Long currentUsage = 0L;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime lastUsedAt;

    private Boolean isActive = true;

    // Constructors
    public UserApi() {}

    public UserApi(String userId, String apiName, String apiKey, String description,
                   UserTier userTier, Long monthlyQuota) {
        this.userId = userId;
        this.apiName = apiName;
        this.apiKey = apiKey;
        this.description = description;
        this.userTier = userTier;
        this.monthlyQuota = monthlyQuota;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getApiName() { return apiName; }
    public void setApiName(String apiName) { this.apiName = apiName; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public UserTier getUserTier() { return userTier; }
    public void setUserTier(UserTier userTier) { this.userTier = userTier; }

    public Long getMonthlyQuota() { return monthlyQuota; }
    public void setMonthlyQuota(Long monthlyQuota) { this.monthlyQuota = monthlyQuota; }

    public Long getCurrentUsage() { return currentUsage; }
    public void setCurrentUsage(Long currentUsage) { this.currentUsage = currentUsage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    // Helper methods
    public boolean hasQuotaRemaining() {
        return currentUsage < monthlyQuota;
    }

    public void incrementUsage() {
        this.currentUsage++;
        this.lastUsedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void resetMonthlyUsage() {
        this.currentUsage = 0L;
        this.updatedAt = LocalDateTime.now();
    }
}