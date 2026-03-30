package org.example.model;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents a configured API provider for AI models
 */
public class APIProvider {
    private String id;
    private String name;
    private String type; // "LLM", "image", "voice", "embedding", "other"
    private String apiKey;
    private List<String> models;
    private String status; // "active", "inactive", "error"
    private LocalDateTime lastTested;
    private int errorCount;
    private int successCount;

    public APIProvider() {
        this.id = UUID.randomUUID().toString();
        this.models = new ArrayList<>();
        this.status = "active";
        this.errorCount = 0;
        this.successCount = 0;
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

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public List<String> getModels() { return models; }
    public void setModels(List<String> models) { this.models = models; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getLastTested() { return lastTested; }
    public void setLastTested(LocalDateTime lastTested) { this.lastTested = lastTested; }

    public int getErrorCount() { return errorCount; }
    public void setErrorCount(int errorCount) { this.errorCount = errorCount; }

    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }
}
