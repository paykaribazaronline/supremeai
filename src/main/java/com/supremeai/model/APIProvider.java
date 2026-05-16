package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import java.util.List;

@Document(collectionName = "api_providers")
public class APIProvider {
    @DocumentId
    private String id;
    private String name;
    private String type;
    private String modelName;
    private String status;
    private String description;
    private String baseUrl;
    private String apiKey;
    private Double usageLimit;
    private Double currentUsage;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private Date lastCheck;

    private String creatorEmail;
    private String accountEmail;

    private java.util.List<String> models = new java.util.ArrayList<>();
    private java.util.List<String> capabilities = new java.util.ArrayList<>();
    private java.util.List<String> languages = new java.util.ArrayList<>();
    private Integer priority = 10;
    
    private boolean canCommunicate = true;
    private boolean canExecuteTasks = true;
    private boolean canParticipateInVoting = true;
    private String deploymentSource = "API"; // API, GCLOUD, LOCAL, OLLAMA
    private java.util.List<String> assignedRoles = new java.util.ArrayList<>();

    /**
     * Auto-discovered capability scores (0.0 - 1.0)
     * Populated by ProviderCapabilityAnalyzer on registration
     * Key: task type, Value: capability score
     */
    private java.util.Map<String, Double> capabilityScores = new java.util.HashMap<>();

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private java.util.Date lastBenchmarkedAt;

    /** Number of times this provider has been benchmarked */
    private Integer benchmarkCount = 0;

    // Auto-validation tracking fields
    private Integer consecutiveErrorDays;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private Date lastValidated;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private Date lastErrorDate;
    
    private String deadReason;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private Date deadAt;
    private Long lastLatency;
    private String lastErrorMessage;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private Date lastTested;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private Date addedAt;
    private String hints;
    private java.util.Map<String, Object> config = new java.util.HashMap<>();

    public APIProvider() {
        this.addedAt = new Date();
    }

    public APIProvider(String id, String name, String type, String status) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.status = status;
        this.lastCheck = new Date();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public void setProviderType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public Double getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Double usageLimit) { this.usageLimit = usageLimit; }
    public Double getCurrentUsage() { return currentUsage; }
    public void setCurrentUsage(Double currentUsage) { this.currentUsage = currentUsage; }
    public Date getLastCheck() { return lastCheck; }
    public void setLastCheck(Date lastCheck) { this.lastCheck = lastCheck; }

    public java.util.List<String> getModels() { return models; }
    public void setModels(java.util.List<String> models) { this.models = models; }

    public java.util.List<String> getCapabilities() { return capabilities; }
    public void setCapabilities(java.util.List<String> capabilities) { this.capabilities = capabilities; }

    public java.util.List<String> getLanguages() { return languages; }
    public void setLanguages(java.util.List<String> languages) { this.languages = languages; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public String getCreatorEmail() { return creatorEmail; }
    public void setCreatorEmail(String creatorEmail) { this.creatorEmail = creatorEmail; }

    public String getAccountEmail() { return accountEmail; }
    public void setAccountEmail(String accountEmail) { this.accountEmail = accountEmail; }

    public boolean isCanCommunicate() { return canCommunicate; }
    public void setCanCommunicate(boolean canCommunicate) { this.canCommunicate = canCommunicate; }

    public boolean isCanExecuteTasks() { return canExecuteTasks; }
    public void setCanExecuteTasks(boolean canExecuteTasks) { this.canExecuteTasks = canExecuteTasks; }

    public boolean isCanParticipateInVoting() { return canParticipateInVoting; }
    public void setCanParticipateInVoting(boolean canParticipateInVoting) { this.canParticipateInVoting = canParticipateInVoting; }

    public String getDeploymentSource() { return deploymentSource; }
    public void setDeploymentSource(String deploymentSource) { this.deploymentSource = deploymentSource; }

    public Integer getConsecutiveErrorDays() { return consecutiveErrorDays; }
    public void setConsecutiveErrorDays(Integer consecutiveErrorDays) { this.consecutiveErrorDays = consecutiveErrorDays; }

    public Date getLastValidated() { return lastValidated; }
    public void setLastValidated(Date lastValidated) { this.lastValidated = lastValidated; }

    public Date getLastErrorDate() { return lastErrorDate; }
    public void setLastErrorDate(Date lastErrorDate) { this.lastErrorDate = lastErrorDate; }

    public String getDeadReason() { return deadReason; }
    public void setDeadReason(String deadReason) { this.deadReason = deadReason; }

    public java.util.List<String> getAssignedRoles() { return assignedRoles; }
    public void setAssignedRoles(java.util.List<String> assignedRoles) { this.assignedRoles = assignedRoles; }

    public Date getDeadAt() { return deadAt; }
    public void setDeadAt(Date deadAt) { this.deadAt = deadAt; }

    public java.util.Map<String, Double> getCapabilityScores() { return capabilityScores; }
    public void setCapabilityScores(java.util.Map<String, Double> capabilityScores) { this.capabilityScores = capabilityScores; }

    public java.util.Date getLastBenchmarkedAt() { return lastBenchmarkedAt; }
    public void setLastBenchmarkedAt(java.util.Date lastBenchmarkedAt) { this.lastBenchmarkedAt = lastBenchmarkedAt; }

    public Integer getBenchmarkCount() { return benchmarkCount; }
    public void setBenchmarkCount(Integer benchmarkCount) { this.benchmarkCount = benchmarkCount; }

    public Long getLastLatency() { return lastLatency; }
    public void setLastLatency(Long lastLatency) { this.lastLatency = lastLatency; }

    public String getLastErrorMessage() { return lastErrorMessage; }
    public void setLastErrorMessage(String lastErrorMessage) { this.lastErrorMessage = lastErrorMessage; }

    public Date getLastTested() { return lastTested; }
    public void setLastTested(Date lastTested) { this.lastTested = lastTested; }

    public Date getAddedAt() { return addedAt; }
    public void setAddedAt(Date addedAt) { this.addedAt = addedAt; }

    public String getHints() { return hints; }
    public void setHints(String hints) { this.hints = hints; }

    public java.util.Map<String, Object> getConfig() { return config; }
    public void setConfig(java.util.Map<String, Object> config) { this.config = config; }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    public void setActive(boolean active) {
        this.status = active ? "ACTIVE" : "INACTIVE";
    }

    public boolean isValidated() {
        return lastValidated != null;
    }

    public void setValidated(boolean validated) {
        this.lastValidated = validated ? new java.util.Date() : null;
    }
}
