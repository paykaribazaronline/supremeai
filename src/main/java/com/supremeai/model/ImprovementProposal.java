package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import java.util.Date;
import java.util.UUID;

@Document(collectionName = "improvement_proposals")
public class ImprovementProposal {
  @DocumentId private String proposalId;
  private String title;
  private String description;
  private String category; // e.g., "KNOWLEDGE_BASE", "IMMUNITY_SYSTEM", "AI_PROFILER"
  private String payload; // The actual code or data to be updated
  private Date timestamp;
  private boolean isApproved;

  public ImprovementProposal() {
    this.proposalId = UUID.randomUUID().toString();
    this.timestamp = new Date();
    this.isApproved = false;
  }

  public ImprovementProposal(String title, String description, String category, String payload) {
    this();
    this.title = title;
    this.description = description;
    this.category = category;
    this.payload = payload;
  }

  public void approve() {
    this.isApproved = true;
  }

  // Getters and Setters
  public String getProposalId() {
    return proposalId;
  }

  public void setProposalId(String proposalId) {
    this.proposalId = proposalId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public boolean isApproved() {
    return isApproved;
  }

  public void setApproved(boolean approved) {
    isApproved = approved;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }
}
