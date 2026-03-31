package org.example.model;

import java.time.Instant;

/**
 * Webhook Model
 * Represents a registered webhook with event subscriptions
 */
public class Webhook {
    private String id;
    private String projectId;
    private String url;
    private String[] events;
    private String secretKey;
    private boolean active;
    private Instant createdAt;
    private Instant lastDeliveryAt;
    private String lastDeliveryStatus;
    private int retryAttempts;
    private int successfulDeliveries;
    private int failedDeliveries;
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getProjectId() {
        return projectId;
    }
    
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String[] getEvents() {
        return events;
    }
    
    public void setEvents(String[] events) {
        this.events = events;
    }
    
    public String getSecretKey() {
        return secretKey;
    }
    
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getLastDeliveryAt() {
        return lastDeliveryAt;
    }
    
    public void setLastDeliveryAt(Instant lastDeliveryAt) {
        this.lastDeliveryAt = lastDeliveryAt;
    }
    
    public String getLastDeliveryStatus() {
        return lastDeliveryStatus;
    }
    
    public void setLastDeliveryStatus(String lastDeliveryStatus) {
        this.lastDeliveryStatus = lastDeliveryStatus;
    }
    
    public int getRetryAttempts() {
        return retryAttempts;
    }
    
    public void setRetryAttempts(int retryAttempts) {
        this.retryAttempts = retryAttempts;
    }
    
    public int getSuccessfulDeliveries() {
        return successfulDeliveries;
    }
    
    public void setSuccessfulDeliveries(int successfulDeliveries) {
        this.successfulDeliveries = successfulDeliveries;
    }
    
    public int getFailedDeliveries() {
        return failedDeliveries;
    }
    
    public void setFailedDeliveries(int failedDeliveries) {
        this.failedDeliveries = failedDeliveries;
    }
    
    @Override
    public String toString() {
        return "Webhook{" +
                "id='" + id + '\'' +
                ", projectId='" + projectId + '\'' +
                ", url='" + url + '\'' +
                ", active=" + active +
                ", successfulDeliveries=" + successfulDeliveries +
                ", failedDeliveries=" + failedDeliveries +
                '}';
    }
}
