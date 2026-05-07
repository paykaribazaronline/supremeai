package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;

@Document(collectionName = "api_providers")
public class APIProvider {
    @DocumentId
    private String id;
    private String name;
    private String type;
    private String status;
    private String baseUrl;
    private String apiKey;
    private Double usageLimit;
    private Double currentUsage;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Date lastCheck;

    private java.util.List<String> models = new java.util.ArrayList<>();
    private java.util.List<String> capabilities = new java.util.ArrayList<>();
    private java.util.List<String> languages = new java.util.ArrayList<>();
    private Integer priority = 10;

    public APIProvider() {}

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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
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
}
