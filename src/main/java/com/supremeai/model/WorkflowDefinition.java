package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import java.util.List;
import java.util.Map;

@Document(collectionName = "workflow_definitions")
public class WorkflowDefinition {
  @DocumentId private String id;
  private String name;
  private String description;
  private String trigger; // manual, scheduled, webhook
  private List<WorkflowStep> steps;
  private Map<String, Object> outputs;

  public WorkflowDefinition() {}

  public WorkflowDefinition(
      String id,
      String name,
      String description,
      String trigger,
      List<WorkflowStep> steps,
      Map<String, Object> outputs) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.trigger = trigger;
    this.steps = steps;
    this.outputs = outputs;
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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getTrigger() {
    return trigger;
  }

  public void setTrigger(String trigger) {
    this.trigger = trigger;
  }

  public List<WorkflowStep> getSteps() {
    return steps;
  }

  public void setSteps(List<WorkflowStep> steps) {
    this.steps = steps;
  }

  public Map<String, Object> getOutputs() {
    return outputs;
  }

  public void setOutputs(Map<String, Object> outputs) {
    this.outputs = outputs;
  }
}
