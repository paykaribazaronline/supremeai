package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;
import com.google.cloud.spring.data.firestore.Document;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Model representing a simulation scenario configuration. Stored in Firestore collection:
 * "simulation_scenarios"
 */
@Document(collectionName = "simulation_scenarios")
public class SimulationScenario {

  @DocumentId private String scenarioId;

  private String userId;
  private String appId;
  private String name;
  private String description;
  private String deviceProfile = "PIXEL_6";
  private Map<String, Object> deviceConfig = new HashMap<>();

  /** Dynamic simulation parameters (e.g. networkSpeed, memoryLimit, cpuThrottling, concurrency) */
  private Map<String, Object> parameters = new HashMap<>();

  @ServerTimestamp private LocalDateTime createdAt;

  @ServerTimestamp private LocalDateTime updatedAt;

  public SimulationScenario() {}

  public SimulationScenario(
      String scenarioId, String userId, String appId, String name, String description) {
    this.scenarioId = scenarioId;
    this.userId = userId;
    this.appId = appId;
    this.name = name;
    this.description = description;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  // Getters & Setters
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

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDeviceProfile() {
    return deviceProfile;
  }

  public void setDeviceProfile(String deviceProfile) {
    this.deviceProfile = deviceProfile;
  }

  public Map<String, Object> getDeviceConfig() {
    return deviceConfig;
  }

  public void setDeviceConfig(Map<String, Object> deviceConfig) {
    this.deviceConfig = deviceConfig;
  }

  public Map<String, Object> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, Object> parameters) {
    this.parameters = parameters;
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
