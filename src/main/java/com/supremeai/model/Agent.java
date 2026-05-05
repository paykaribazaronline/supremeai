package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import java.time.LocalDateTime;

@Document(collectionName = "ai_agents")
public class Agent {
    @DocumentId
    private String id;
    private String name;
    private String type;
    private String status;
    private String uptime;
    private LocalDateTime lastActive;

    public Agent() {}

    public Agent(String id, String name, String type, String status) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.status = status;
        this.uptime = "0h";
        this.lastActive = LocalDateTime.now();
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
    public String getUptime() { return uptime; }
    public void setUptime(String uptime) { this.uptime = uptime; }
    public LocalDateTime getLastActive() { return lastActive; }
    public void setLastActive(LocalDateTime lastActive) { this.lastActive = lastActive; }
}
