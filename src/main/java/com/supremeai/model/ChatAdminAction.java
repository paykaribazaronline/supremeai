package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import java.time.LocalDateTime;

public class ChatAdminAction {
  @DocumentId private String id;
  private String chatId;
  private String actionType; // ADD_API, LEARN_WEBSITE, TEST_API, RUN_AUDIT
  private String content;
  private double confidence;
  private String userId;
  private boolean active;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public ChatAdminAction() {}

  public ChatAdminAction(
      String chatId, String actionType, String content, double confidence, String userId) {
    this.chatId = chatId;
    this.actionType = actionType;
    this.content = content;
    this.confidence = confidence;
    this.userId = userId;
    this.active = true;
    this.createdAt = LocalDateTime.now();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getChatId() {
    return chatId;
  }

  public void setChatId(String chatId) {
    this.chatId = chatId;
  }

  public String getActionType() {
    return actionType;
  }

  public void setActionType(String actionType) {
    this.actionType = actionType;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public double getConfidence() {
    return confidence;
  }

  public void setConfidence(double confidence) {
    this.confidence = confidence;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
