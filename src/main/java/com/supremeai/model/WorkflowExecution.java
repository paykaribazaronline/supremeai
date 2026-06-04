package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;
import com.google.cloud.spring.data.firestore.Document;
import java.util.Date;
import java.util.Map;
import org.springframework.data.annotation.Id;

@Document(collectionName = "workflow_executions")
public class WorkflowExecution {
  @Id @DocumentId private String executionId;
  private String workflowId;
  private String status; // RUNNING, COMPLETED, FAILED
  private int currentStepIndex;
  private Map<String, Object> stepResults;

  @ServerTimestamp private Date startedAt;

  @ServerTimestamp private Date completedAt;
  private String errorMessage;

  public WorkflowExecution() {}

  public WorkflowExecution(String executionId, String workflowId, String status) {
    this.executionId = executionId;
    this.workflowId = workflowId;
    this.status = status;
    this.startedAt = new Date();
  }

  public WorkflowExecution(
      String executionId,
      String workflowId,
      String status,
      int currentStepIndex,
      Map<String, Object> stepResults,
      Date startedAt,
      Date completedAt,
      String errorMessage) {
    this.executionId = executionId;
    this.workflowId = workflowId;
    this.status = status;
    this.currentStepIndex = currentStepIndex;
    this.stepResults = stepResults;
    this.startedAt = startedAt;
    this.completedAt = completedAt;
    this.errorMessage = errorMessage;
  }

  public String getExecutionId() {
    return executionId;
  }

  public void setExecutionId(String executionId) {
    this.executionId = executionId;
  }

  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public int getCurrentStepIndex() {
    return currentStepIndex;
  }

  public void setCurrentStepIndex(int currentStepIndex) {
    this.currentStepIndex = currentStepIndex;
  }

  public Map<String, Object> getStepResults() {
    return stepResults;
  }

  public void setStepResults(Map<String, Object> stepResults) {
    this.stepResults = stepResults;
  }

  public Date getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(Date startedAt) {
    this.startedAt = startedAt;
  }

  public Date getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(Date completedAt) {
    this.completedAt = completedAt;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }
}
