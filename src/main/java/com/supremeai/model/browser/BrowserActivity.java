package com.supremeai.model.browser;

import com.google.cloud.spring.data.firestore.Document;
import java.time.LocalDateTime;

@Document(collectionName = "browser_activities")
public class BrowserActivity {
    private String id;
    private String url;
    private String title;
    private String status; // navigating, loading, completed, paused, error
    private String reasoning; // Why is the system visiting this URL?
    private LocalDateTime timestamp;
    private Long duration;
    private String action; // surf, scrape, login, search
    private Boolean hasAuthRequired;
    private String elementText;

    public BrowserActivity() {}

    public BrowserActivity(String id, String url, String action) {
        this.id = id;
        this.url = url;
        this.action = action;
        this.status = "navigating";
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public Long getDuration() { return duration; }
    public void setDuration(Long duration) { this.duration = duration; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Boolean getHasAuthRequired() { return hasAuthRequired; }
    public void setHasAuthRequired(Boolean hasAuthRequired) { this.hasAuthRequired = hasAuthRequired; }
    public String getElementText() { return elementText; }
    public void setElementText(String elementText) { this.elementText = elementText; }
}
