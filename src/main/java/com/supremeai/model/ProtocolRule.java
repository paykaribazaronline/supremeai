package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import java.time.LocalDateTime;

@Document(collectionName = "protocol_rules")
public class ProtocolRule {
  @DocumentId private String id;
  private String name;
  private String type;
  private String pattern;
  private String action;
  private String severity;
  private boolean active;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String lastTriggered;

  public ProtocolRule() {}

  public ProtocolRule(String name, String type, String pattern, String action, String severity) {
    this.name = name;
    this.type = type;
    this.pattern = pattern;
    this.action = action;
    this.severity = severity;
    this.active = true;
    this.createdAt = LocalDateTime.now();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getPattern() {
    return pattern;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getSeverity() {
    return severity;
  }

  public void setSeverity(String severity) {
    this.severity = severity;
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

  public String getLastTriggered() {
    return lastTriggered;
  }

  public void setLastTriggered(String lastTriggered) {
    this.lastTriggered = lastTriggered;
  }
}
