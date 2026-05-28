package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

@Document(collectionName = "api_providers")
public class APIProvider {
    @DocumentId
    private String documentId;

    @com.google.cloud.firestore.annotation.PropertyName("id")
    private String idField;
    private String name;
    private String type;
    private String modelName;
    private String status;
    private String description;
    private String baseUrl;
    private String apiKey;
    private Double usageLimit;
    private Double currentUsage;
    
    private Object lastCheck;

    private String creatorEmail;
    private String accountEmail;

    private java.util.List<String> models = new java.util.ArrayList<>();
    private java.util.List<String> capabilities = new java.util.ArrayList<>();
    private java.util.List<String> languages = new java.util.ArrayList<>();
    private Integer priority = 10;
    
    private boolean canCommunicate = true;
    private boolean canExecuteTasks = true;
    private boolean canParticipateInVoting = true;
    private String deploymentSource = "api"; // api, gcloud, local, ollama
    private java.util.List<String> assignedRoles = new java.util.ArrayList<>();

    /**
     * Auto-discovered capability scores (0.0 - 1.0)
     * Populated by ProviderCapabilityAnalyzer on registration
     * Key: task type, Value: capability score
     */
    private java.util.Map<String, Double> capabilityScores = new java.util.HashMap<>();

    private Object lastBenchmarkedAt;

    /** Number of times this provider has been benchmarked */
    private Integer benchmarkCount = 0;

    // Auto-validation tracking fields
    private Integer consecutiveErrorDays;
    
    private Object lastValidated;
    
    private Object lastErrorDate;
    
    private String deadReason;
    
    private Object deadAt;
    private Long lastLatency;
    private String lastErrorMessage;
    
    private Object lastTested;
    
    private Object addedAt;
    private String hints;
    private java.util.Map<String, Object> config = new java.util.HashMap<>();

    public APIProvider() {
        this.addedAt = LocalDateTime.now();
    }

    public APIProvider(String id, String name, String type, String status) {
        this.idField = id;
        this.name = name;
        this.type = type;
        this.status = status;
        this.lastCheck = LocalDateTime.now();
    }

    // Getters and Setters
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    @com.google.cloud.firestore.annotation.PropertyName("id")
    public String getId() { return idField != null ? idField : documentId; }

    @com.google.cloud.firestore.annotation.PropertyName("id")
    public void setId(String id) { this.idField = id; }
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
    public LocalDateTime getLastCheck() { return convertToLocalDateTime(lastCheck); }
    public void setLastCheck(Object lastCheck) { this.lastCheck = lastCheck; }

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

    public LocalDateTime getLastValidated() { return convertToLocalDateTime(lastValidated); }
    public void setLastValidated(Object lastValidated) { this.lastValidated = lastValidated; }

    public LocalDateTime getLastErrorDate() { return convertToLocalDateTime(lastErrorDate); }
    public void setLastErrorDate(Object lastErrorDate) { this.lastErrorDate = lastErrorDate; }

    public String getDeadReason() { return deadReason; }
    public void setDeadReason(String deadReason) { this.deadReason = deadReason; }

    public java.util.List<String> getAssignedRoles() { return assignedRoles; }
    public void setAssignedRoles(java.util.List<String> assignedRoles) { this.assignedRoles = assignedRoles; }

    public LocalDateTime getDeadAt() { return convertToLocalDateTime(deadAt); }
    public void setDeadAt(Object deadAt) { this.deadAt = deadAt; }

    public java.util.Map<String, Double> getCapabilityScores() { return capabilityScores; }
    public void setCapabilityScores(java.util.Map<String, Double> capabilityScores) { this.capabilityScores = capabilityScores; }

    public LocalDateTime getLastBenchmarkedAt() { return convertToLocalDateTime(lastBenchmarkedAt); }
    public void setLastBenchmarkedAt(Object lastBenchmarkedAt) { this.lastBenchmarkedAt = lastBenchmarkedAt; }

    public Integer getBenchmarkCount() { return benchmarkCount; }
    public void setBenchmarkCount(Integer benchmarkCount) { this.benchmarkCount = benchmarkCount; }

    public Long getLastLatency() { return lastLatency; }
    public void setLastLatency(Long lastLatency) { this.lastLatency = lastLatency; }

    public String getLastErrorMessage() { return lastErrorMessage; }
    public void setLastErrorMessage(String lastErrorMessage) { this.lastErrorMessage = lastErrorMessage; }

    public LocalDateTime getLastTested() { return convertToLocalDateTime(lastTested); }
    public void setLastTested(Object lastTested) { this.lastTested = lastTested; }

    public LocalDateTime getAddedAt() { return convertToLocalDateTime(addedAt); }
    public void setAddedAt(Object addedAt) { this.addedAt = addedAt; }

    public String getHints() { return hints; }
    public void setHints(String hints) { this.hints = hints; }

    public java.util.Map<String, Object> getConfig() { return config; }
    public void setConfig(java.util.Map<String, Object> config) { this.config = config; }

    public boolean isActive() {
        return ProviderStatus.ACTIVE.equalsIgnoreCase(status);
    }

    public void setActive(boolean active) {
        this.status = active ? ProviderStatus.ACTIVE : ProviderStatus.INACTIVE;
    }

    public boolean isValidated() {
        return lastValidated != null;
    }

    public void setValidated(boolean validated) {
        this.lastValidated = validated ? LocalDateTime.now() : null;
    }

    private LocalDateTime convertToLocalDateTime(Object obj) {
        if (obj == null) return null;
        if (obj instanceof LocalDateTime) return (LocalDateTime) obj;
        if (obj instanceof java.util.Date) return ((java.util.Date) obj).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        if (obj instanceof com.google.cloud.Timestamp) {
            com.google.cloud.Timestamp t = (com.google.cloud.Timestamp) obj;
            return LocalDateTime.ofInstant(t.toDate().toInstant(), java.time.ZoneId.systemDefault());
        }
        if (obj instanceof Long) return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli((Long) obj), java.time.ZoneId.systemDefault());
        if (obj instanceof String) {
            try {
                return LocalDateTime.from(java.time.OffsetDateTime.parse((String) obj));
            } catch (Exception e) {
                try {
                    return LocalDateTime.parse((String) obj);
                } catch (Exception ex) {
                    return null;
                }
            }
        }
        if (obj instanceof java.util.Map) {
            try {
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) obj;
                Object seconds = map.get("seconds");
                Object nanos = map.get("nanoseconds");
                if (seconds instanceof Number) {
                    long sec = ((Number) seconds).longValue();
                    int nan = (nanos instanceof Number) ? ((Number) nanos).intValue() : 0;
                    return LocalDateTime.ofInstant(com.google.cloud.Timestamp.ofTimeSecondsAndNanos(sec, nan).toDate().toInstant(), java.time.ZoneId.systemDefault());
                }
            } catch (Exception e) {}
        }
        return null;
    }
}
