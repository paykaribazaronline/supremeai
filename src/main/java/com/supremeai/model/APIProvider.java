package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import java.time.LocalDateTime;

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
    private LocalDateTime lastCheck;

    public APIProvider() {}

    public APIProvider(String id, String name, String type, String status) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.status = status;
        this.lastCheck = LocalDateTime.now();
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
    public LocalDateTime getLastCheck() { return lastCheck; }
    public void setLastCheck(LocalDateTime lastCheck) { this.lastCheck = lastCheck; }
}
