package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;
import com.google.cloud.spring.data.firestore.Document;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model representing the results of a simulation execution. Stored in Firestore collection:
 * "simulation_results"
 */
@Document(collectionName = "simulation_results")
public class SimulationResult {

  @DocumentId private String resultId;

  private String scenarioId;
  private String userId;
  private String appId;
  private String status = "PENDING"; // PENDING, RUNNING, SUCCESS, FAILED

  /**
   * Performance metrics (e.g. loadTimeMs, cpuUsagePct, memoryUsageMb, errorRatePct, responseTimeMs)
   */
  private Map<String, Object> metrics = new HashMap<>();

  /** AI jetted optimization recommendations based on simulation result */
  private List<String> recommendations = new ArrayList<>();

  /** Execution console logs */
  private List<String> logs = new ArrayList<>();

  @ServerTimestamp private LocalDateTime executedAt;

  public SimulationResult() {}

  public SimulationResult(String resultId, String scenarioId, String userId, String appId) {
    this.resultId = resultId;
    this.scenarioId = scenarioId;
    this.userId = userId;
    this.appId = appId;
    this.status = "PENDING";
    this.executedAt = LocalDateTime.now();
  }

  // Getters & Setters
  public String getResultId() {
    return resultId;
  }

  public void setResultId(String resultId) {
    this.resultId = resultId;
  }

  public String getScenarioId() {
    return scenarioId;
  }

  public void setScenarioId(String scenarioId) {
    this.scenarioId = scenarioId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Map<String, Object> getMetrics() {
    return metrics;
  }

  public void setMetrics(Map<String, Object> metrics) {
    this.metrics = metrics;
  }

  public List<String> getRecommendations() {
    return recommendations;
  }

  public void setRecommendations(List<String> recommendations) {
    this.recommendations = recommendations;
  }

  public List<String> getLogs() {
    return logs;
  }

  public void setLogs(List<String> logs) {
    this.logs = logs;
  }

  public LocalDateTime getExecutedAt() {
    return executedAt;
  }

  public void setExecutedAt(LocalDateTime executedAt) {
    this.executedAt = executedAt;
  }
}
