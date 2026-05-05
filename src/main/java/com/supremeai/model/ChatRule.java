package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import java.time.LocalDateTime;

@Document(collectionName = "chat_rules")
public class ChatRule {
    private String id;
    private String chatId;
    private String content;
    private Double confidence;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean active;

    public ChatRule() {}

    public ChatRule(String chatId, String content, Double confidence, String createdBy) {
        this.chatId = chatId;
        this.content = content;
        this.confidence = confidence;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.active = true;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
