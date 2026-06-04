package com.supremeai.model.browser;

import com.google.cloud.spring.data.firestore.Document;
import java.time.LocalDateTime;
import java.util.Map;

@Document(collectionName = "browser_findings")
public class BrowserFinding {
  private String id;
  private String taskId;
  private String url;
  private String type; // VERSION, PRICE, SNIPPET, CONCLUSION, WARNING
  private String title;
  private String content;
  private Double confidence;
  private Map<String, Object> metadata;
  private LocalDateTime foundAt;

  public BrowserFinding() {
    this.foundAt = LocalDateTime.now();
  }

  // Getters and Setters
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTaskId() {
    return taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public Double getConfidence() {
    return confidence;
  }

  public void setConfidence(Double confidence) {
    this.confidence = confidence;
  }

  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, Object> metadata) {
    this.metadata = metadata;
  }

  public LocalDateTime getFoundAt() {
    return foundAt;
  }

  public void setFoundAt(LocalDateTime foundAt) {
    this.foundAt = foundAt;
  }
}
