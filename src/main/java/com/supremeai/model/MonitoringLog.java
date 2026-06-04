package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;

@Document(collectionName = "monitoring_logs")
public class MonitoringLog {
  @DocumentId private String id;
  private String level; // INFO, WARN, ERROR, SUCCESS, ALERT
  private String component; // e.g., GitHub, Monitoring, Auth
  private String message;
  private long timestamp;

  // Constructors
  public MonitoringLog() {}

  public MonitoringLog(String id, String level, String component, String message, long timestamp) {
    this.id = id;
    this.level = level;
    this.component = component;
    this.message = message;
    this.timestamp = timestamp;
  }

  // Getters and Setters
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getLevel() {
    return level;
  }

  public void setLevel(String level) {
    this.level = level;
  }

  public String getComponent() {
    return component;
  }

  public void setComponent(String component) {
    this.component = component;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  // Builder
  public static MonitoringLogBuilder builder() {
    return new MonitoringLogBuilder();
  }

  public static class MonitoringLogBuilder {
    private String id;
    private String level;
    private String component;
    private String message;
    private long timestamp;

    public MonitoringLogBuilder id(String id) {
      this.id = id;
      return this;
    }

    public MonitoringLogBuilder level(String level) {
      this.level = level;
      return this;
    }

    public MonitoringLogBuilder component(String component) {
      this.component = component;
      return this;
    }

    public MonitoringLogBuilder message(String message) {
      this.message = message;
      return this;
    }

    public MonitoringLogBuilder timestamp(long timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    public MonitoringLog build() {
      return new MonitoringLog(id, level, component, message, timestamp);
    }
  }
}
