package com.supremeai.model;

import java.time.LocalDateTime;

public class ChatMessage {
    private String id;
    private String role; // "user" or "ai" or "system"
    private String content;
    private LocalDateTime timestamp;
    private String questionId;

    public ChatMessage() {}

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }
}