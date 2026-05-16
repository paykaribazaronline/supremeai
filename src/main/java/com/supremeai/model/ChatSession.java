package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Document(collectionName = "chat_sessions")
public class ChatSession {
    @DocumentId
    private String id;
    private String name;
    private String userId;
    private List<ChatMessage> messages = new ArrayList<>();
    private Date createdAt;

    public ChatSession() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = new Date();
    }

    public ChatSession(String name, String userId) {
        this();
        this.name = name;
        this.userId = userId;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public List<ChatMessage> getMessages() { return messages; }
    public void setMessages(List<ChatMessage> messages) { this.messages = messages; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
