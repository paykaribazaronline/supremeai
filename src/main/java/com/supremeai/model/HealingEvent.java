package com.supremeai.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class HealingEvent {
  private String id;
  private String errorType;
  private String errorMessage;
  private String strategyApplied;
  private String fixAction;
  private boolean success;
  private String reasoning;
  private LocalDateTime timestamp;
  private String component;
  private boolean canRollback;
  private String rollbackAction;

  public HealingEvent() {
    this.id = UUID.randomUUID().toString();
    this.timestamp = LocalDateTime.now();
  }

  public HealingEvent(
      String errorType,
      String errorMessage,
      String strategyApplied,
      String fixAction,
      boolean success,
      String reasoning,
      String component) {
    this();
    this.errorType = errorType;
    this.errorMessage = errorMessage;
    this.strategyApplied = strategyApplied;
    this.fixAction = fixAction;
    this.success = success;
    this.reasoning = reasoning;
    this.component = component;
  }

  // Getters and Setters
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getErrorType() {
    return errorType;
  }

  public void setErrorType(String errorType) {
    this.errorType = errorType;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String getStrategyApplied() {
    return strategyApplied;
  }

  public void setStrategyApplied(String strategyApplied) {
    this.strategyApplied = strategyApplied;
  }

  public String getFixAction() {
    return fixAction;
  }

  public void setFixAction(String fixAction) {
    this.fixAction = fixAction;
  }

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public String getReasoning() {
    return reasoning;
  }

  public void setReasoning(String reasoning) {
    this.reasoning = reasoning;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(LocalDateTime timestamp) {
    this.timestamp = timestamp;
  }

  public String getComponent() {
    return component;
  }

  public void setComponent(String component) {
    this.component = component;
  }

  public boolean isCanRollback() {
    return canRollback;
  }

  public void setCanRollback(boolean canRollback) {
    this.canRollback = canRollback;
  }

  public String getRollbackAction() {
    return rollbackAction;
  }

  public void setRollbackAction(String rollbackAction) {
    this.rollbackAction = rollbackAction;
  }
}
