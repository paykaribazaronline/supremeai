package org.example.model;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents a configured API provider for AI models
 */
public class APIProvider {
    private String id;
    private String name;
    private String baseModel;
    private String type; // "LLM", "image", "voice", "embedding", "other"
    private String apiKey;
    private String endpoint;
    private String alias;
    private String notes;
    private String createdByEmail;
    private LocalDateTime createdAt;
    private List<String> models;
    private String status; // "active", "inactive", "error"
    private LocalDateTime lastTested;
    private String lastError;
    private int errorCount;
    private int successCount;
    private Integer rateLimitPerMinute;
    private Integer monthlyQuota;
    private Integer freeQuotaPercent;
    private Integer alertThreshold;

    public APIProvider() {
        this.id = UUID.randomUUID().toString();
        this.models = new ArrayList<>();
        this.status = "active";
        this.errorCount = 0;
        this.successCount = 0;
        this.createdAt = LocalDateTime.now();
    }

    public APIProvider(String name, String type, String apiKey) {
        this();
        this.name = name;
        this.type = type;
        this.apiKey = apiKey;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBaseModel() { return baseModel; }
    public void setBaseModel(String baseModel) { this.baseModel = baseModel; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCreatedByEmail() { return createdByEmail; }
    public void setCreatedByEmail(String createdByEmail) { this.createdByEmail = createdByEmail; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<String> getModels() { return models; }
    public void setModels(List<String> models) { this.models = models; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getLastTested() { return lastTested; }
    public void setLastTested(LocalDateTime lastTested) { this.lastTested = lastTested; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public int getErrorCount() { return errorCount; }
    public void setErrorCount(int errorCount) { this.errorCount = errorCount; }

    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }

    public Integer getRateLimitPerMinute() { return rateLimitPerMinute; }
    public void setRateLimitPerMinute(Integer rateLimitPerMinute) { this.rateLimitPerMinute = rateLimitPerMinute; }

    public Integer getMonthlyQuota() { return monthlyQuota; }
    public void setMonthlyQuota(Integer monthlyQuota) { this.monthlyQuota = monthlyQuota; }

    public Integer getFreeQuotaPercent() { return freeQuotaPercent; }
    public void setFreeQuotaPercent(Integer freeQuotaPercent) { this.freeQuotaPercent = freeQuotaPercent; }

    public Integer getAlertThreshold() { return alertThreshold; }
    public void setAlertThreshold(Integer alertThreshold) { this.alertThreshold = alertThreshold; }
}
