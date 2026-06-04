package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

/** Daily health report for all stored API keys. Helps administrators identify dead/expired keys. */
public class APIHealthReport {

  @DocumentId private String id;

  private int totalKeysTested;
  private int activeKeys;
  private int deadKeys;
  private int rotationDueKeys;

  private List<Map<String, Object>> deadKeyDetails; // [{id, label, provider, error}]

  @ServerTimestamp private Date createdAt;

  public APIHealthReport() {}

  public APIHealthReport(String id, int total, int active, int dead, int rotationDue) {
    this.id = id;
    this.totalKeysTested = total;
    this.activeKeys = active;
    this.deadKeys = dead;
    this.rotationDueKeys = rotationDue;
    this.createdAt = new Date();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public int getTotalKeysTested() {
    return totalKeysTested;
  }

  public void setTotalKeysTested(int totalKeysTested) {
    this.totalKeysTested = totalKeysTested;
  }

  public int getActiveKeys() {
    return activeKeys;
  }

  public void setActiveKeys(int activeKeys) {
    this.activeKeys = activeKeys;
  }

  public int getDeadKeys() {
    return deadKeys;
  }

  public void setDeadKeys(int deadKeys) {
    this.deadKeys = deadKeys;
  }

  public int getRotationDueKeys() {
    return rotationDueKeys;
  }

  public void setRotationDueKeys(int rotationDueKeys) {
    this.rotationDueKeys = rotationDueKeys;
  }

  // Convenience aliases for SelfHealingService
  public int getTotalCount() {
    return totalKeysTested;
  }

  public int getActiveCount() {
    return activeKeys;
  }

  public int getDeadCount() {
    return deadKeys;
  }

  public List<Map<String, Object>> getDeadKeyDetails() {
    return deadKeyDetails;
  }

  public void setDeadKeyDetails(List<Map<String, Object>> deadKeyDetails) {
    this.deadKeyDetails = deadKeyDetails;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }
}
