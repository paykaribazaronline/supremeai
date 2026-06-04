package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;

@Document(collectionName = "provider_task_performance")
public class ProviderTaskPerformance {
  @DocumentId private String id; // Format: provider_taskType
  private String provider;
  private String taskType;
  private double successRate;
  private double averageResponseTimeMs;
  private double averageQualityScore;
  private int totalTasks;
  private long lastUsed;

  public ProviderTaskPerformance() {}

  public ProviderTaskPerformance(
      String provider,
      String taskType,
      double successRate,
      double averageResponseTimeMs,
      double averageQualityScore,
      int totalTasks,
      long lastUsed) {
    this.id = provider + "_" + taskType;
    this.provider = provider;
    this.taskType = taskType;
    this.successRate = successRate;
    this.averageResponseTimeMs = averageResponseTimeMs;
    this.averageQualityScore = averageQualityScore;
    this.totalTasks = totalTasks;
    this.lastUsed = lastUsed;
  }

  // Getters and Setters
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public String getTaskType() {
    return taskType;
  }

  public void setTaskType(String taskType) {
    this.taskType = taskType;
  }

  public double getSuccessRate() {
    return successRate;
  }

  public void setSuccessRate(double successRate) {
    this.successRate = successRate;
  }

  public double getAverageResponseTimeMs() {
    return averageResponseTimeMs;
  }

  public void setAverageResponseTimeMs(double averageResponseTimeMs) {
    this.averageResponseTimeMs = averageResponseTimeMs;
  }

  public double getAverageQualityScore() {
    return averageQualityScore;
  }

  public void setAverageQualityScore(double averageQualityScore) {
    this.averageQualityScore = averageQualityScore;
  }

  public int getTotalTasks() {
    return totalTasks;
  }

  public void setTotalTasks(int totalTasks) {
    this.totalTasks = totalTasks;
  }

  public long getLastUsed() {
    return lastUsed;
  }

  public void setLastUsed(long lastUsed) {
    this.lastUsed = lastUsed;
  }
}
