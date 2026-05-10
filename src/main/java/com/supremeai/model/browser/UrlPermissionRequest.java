package com.supremeai.model.browser;

import com.google.cloud.spring.data.firestore.Document;
import java.time.LocalDateTime;

@Document(collectionName = "browser_url_requests")
public class UrlPermissionRequest {
    private String id;
    private String url;
    private String pattern;
    private String status; // pending, approved, denied
    private String reason;
    private String learnedFrom;
    private LocalDateTime requestedAt;

    public UrlPermissionRequest() {
        this.status = "pending";
        this.requestedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getLearnedFrom() { return learnedFrom; }
    public void setLearnedFrom(String learnedFrom) { this.learnedFrom = learnedFrom; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
}
