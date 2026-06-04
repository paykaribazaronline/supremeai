package com.supremeai.model;

import java.util.Map;

public class WorkflowStep {
  private String id;
  private String agent; // ReverseEngineeringAgent, CodeGenerationAgent, etc.
  private Map<String, Object> input;
  private String output; // Key to store result for next steps

  public WorkflowStep() {}

  public WorkflowStep(String id, String agent, Map<String, Object> input, String output) {
    this.id = id;
    this.agent = agent;
    this.input = input;
    this.output = output;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getAgent() {
    return agent;
  }

  public void setAgent(String agent) {
    this.agent = agent;
  }

  public Map<String, Object> getInput() {
    return input;
  }

  public void setInput(Map<String, Object> input) {
    this.input = input;
  }

  public String getOutput() {
    return output;
  }

  public void setOutput(String output) {
    this.output = output;
  }
}
