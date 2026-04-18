package com.supremeai.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs")
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String action;
    private String user;
    private String category;
    private String severity; // info, warning, critical
    
    @Column(length = 1000)
    private String details;
    
    private LocalDateTime timestamp;
    private String ip;
    private String outcome; // success, failure, pending

    public ActivityLog() {
        this.timestamp = LocalDateTime.now();
    }

    public ActivityLog(String action, String user, String category, String severity, String details, String outcome, String ip) {
        this();
        this.action = action;
        this.user = user;
        this.category = category;
        this.severity = severity;
        this.details = details;
        this.outcome = outcome;
        this.ip = ip;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
}
