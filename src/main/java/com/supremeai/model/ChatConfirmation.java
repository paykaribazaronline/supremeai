package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import java.time.LocalDateTime;

@Document(collectionName = "chat_confirmations")
public class ChatConfirmation {
    private String id;
    private String chatId;
    private String itemType; // "rule", "plan", "command"
    private String itemId;
    private boolean confirmed;
    private String confirmedBy;
    private LocalDateTime confirmedAt;

    public ChatConfirmation() {}

    public ChatConfirmation(String chatId, String itemType, String itemId, boolean confirmed, String confirmedBy) {
        this.chatId = chatId;
        this.itemType = itemType;
        this.itemId = itemId;
        this.confirmed = confirmed;
        this.confirmedBy = confirmedBy;
        this.confirmedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }
    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public boolean isConfirmed() { return confirmed; }
    public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }
    public String getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(String confirmedBy) { this.confirmedBy = confirmedBy; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
}
