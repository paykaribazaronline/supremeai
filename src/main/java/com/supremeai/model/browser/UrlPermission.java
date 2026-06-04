package com.supremeai.model.browser;

import com.google.cloud.spring.data.firestore.Document;
import java.time.LocalDateTime;

@Document(collectionName = "browser_url_permissions")
public class UrlPermission {
  private String id;
  private String userId;
  private String url;
  private String pattern;
  private String type; // allowed, denied, allowAll
  private String reason;
  private LocalDateTime createdAt;

  public UrlPermission() {
    this.createdAt = LocalDateTime.now();
  }

  public UrlPermission(String url, String pattern, String type) {
    this();
    this.url = url;
    this.pattern = pattern;
    this.type = type;
  }

  // Getters and Setters
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getPattern() {
    return pattern;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
