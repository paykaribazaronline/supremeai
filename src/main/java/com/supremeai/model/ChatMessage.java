package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import java.time.LocalDateTime;

@Document(collectionName = "chat_messages")
public class ChatMessage {
    private String id;
    private String role; // "user" or "ai" or "system"
    private String content;
    private LocalDateTime timestamp;
    private String questionId;
    private String userId;  // Additional field for user identification
    private boolean isAdmin; // Additional flag

    public ChatMessage() {}

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public ChatMessage(String userId, String message, boolean isAdmin) {
        this.userId = userId;
        this.content = message;
        this.isAdmin = isAdmin;
        this.role = isAdmin ? "ai" : "user";
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
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public boolean isAdmin() { return isAdmin; }
    public void setAdmin(boolean admin) { isAdmin = admin; }
}
