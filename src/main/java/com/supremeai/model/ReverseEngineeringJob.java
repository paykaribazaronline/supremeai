package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;
import org.springframework.data.annotation.Id;
import java.util.Date;
import java.util.Map;

/**
 * Reverse engineering job stored in Firestore.
 * Collection: "reverse_engineering_jobs"
 */
public class ReverseEngineeringJob {

    @Id
    @DocumentId
    private String jobId;

    private String userId;

    private String websiteUrl;
    
    private String taskType; // REVERSE_ENGINEER, DATA_EXTRACTION, AUTOMATION, SECURITY_AUDIT
    
    private String customInstructions;

    private String status; // PENDING, PROCESSING, COMPLETED, FAILED

    private String errorMessage;

    private Map<String, Object> discoveredApis; // endpoints, methods, params

    private Map<String, Object> scrapedData; // raw scraping results

    private String generatedAppId; // linked generated app

    private Date startedAt; // when processing started

    @ServerTimestamp
    private Date createdAt;

    @ServerTimestamp
    private Date updatedAt;

    public ReverseEngineeringJob() {}

    public ReverseEngineeringJob(String jobId, String userId, String websiteUrl, String taskType) {
        this.jobId = jobId;
        this.userId = userId;
        this.websiteUrl = websiteUrl;
        this.taskType = taskType != null ? taskType : "REVERSE_ENGINEER";
        this.status = "PENDING";
        this.createdAt = new Date();
    }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }

    public String getCustomInstructions() { return customInstructions; }
    public void setCustomInstructions(String customInstructions) { this.customInstructions = customInstructions; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Map<String, Object> getDiscoveredApis() { return discoveredApis; }
    public void setDiscoveredApis(Map<String, Object> discoveredApis) { this.discoveredApis = discoveredApis; }

    public Map<String, Object> getScrapedData() { return scrapedData; }
    public void setScrapedData(Map<String, Object> scrapedData) { this.scrapedData = scrapedData; }

    public String getGeneratedAppId() { return generatedAppId; }
    public void setGeneratedAppId(String generatedAppId) { this.generatedAppId = generatedAppId; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public Date getStartedAt() { return startedAt; }
    public void setStartedAt(Date startedAt) { this.startedAt = startedAt; }
}
