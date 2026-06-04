package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import java.util.List;

@Document(collectionName = "system_instructions")
public class SystemInstruction {
  @DocumentId private String id; // e.g., "app_generation_rules", "code_review_guidelines"
  private String title;
  private String content;
  private List<String> applicableTaskTypes;
  private boolean isActive;
  private int priority;

  public SystemInstruction() {}

  public SystemInstruction(
      String id,
      String title,
      String content,
      List<String> applicableTaskTypes,
      boolean isActive,
      int priority) {
    this.id = id;
    this.title = title;
    this.content = content;
    this.applicableTaskTypes = applicableTaskTypes;
    this.isActive = isActive;
    this.priority = priority;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
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

  public List<String> getApplicableTaskTypes() {
    return applicableTaskTypes;
  }

  public void setApplicableTaskTypes(List<String> applicableTaskTypes) {
    this.applicableTaskTypes = applicableTaskTypes;
  }

  public boolean isActive() {
    return isActive;
  }

  public void setActive(boolean active) {
    isActive = active;
  }

  public int getPriority() {
    return priority;
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }
}
