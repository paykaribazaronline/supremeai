package com.supremeai.model.browser;

import com.google.cloud.spring.data.firestore.Document;
import java.time.LocalDateTime;
import java.util.List;

@Document(collectionName = "browser_tasks")
public class BrowserTask {
    private String id;
    private String goal;
    private String status; // active, completed, failed
    private Integer progress; // 0-100
    private List<String> findings;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public BrowserTask() {
        this.status = "active";
        this.progress = 0;
        this.startedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }
    public List<String> getFindings() { return findings; }
    public void setFindings(List<String> findings) { this.findings = findings; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
