package org.example.model;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents a chat message in the AI chat interface
 */
public class ChatMessage {
    private String id;
    private String sender; // "user" or "ai"
    private String agent;
    private String content;
    private LocalDateTime timestamp;
    private double confidence;
    private String status; // "pending", "sent", "received", "error"

    public ChatMessage() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.status = "sent";
    }

    public ChatMessage(String sender, String content) {
        this();
        this.sender = sender;
        this.content = content;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getAgent() { return agent; }
    public void setAgent(String agent) { this.agent = agent; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
