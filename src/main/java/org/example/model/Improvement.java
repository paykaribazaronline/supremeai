package org.example.model;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents an improvement proposed by AI
 */
public class Improvement {
    private String id;
    private String title;
    private String description;
    private String proposedBy;
    private String status; // "proposed", "in-progress", "completed", "rejected"
    private String category;
    private double estimatedImpact;
    private LocalDateTime createdAt;
    private LocalDateTime completedDate;

    public Improvement() {
        this.id = UUID.randomUUID().toString();
        this.status = "proposed";
        this.createdAt = LocalDateTime.now();
    }

    public Improvement(String title, String proposedBy) {
        this();
        this.title = title;
        this.proposedBy = proposedBy;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getProposedBy() { return proposedBy; }
    public void setProposedBy(String proposedBy) { this.proposedBy = proposedBy; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getEstimatedImpact() { return estimatedImpact; }
    public void setEstimatedImpact(double estimatedImpact) { this.estimatedImpact = estimatedImpact; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getCompletedDate() { return completedDate; }
    public void setCompletedDate(LocalDateTime completedDate) { this.completedDate = completedDate; }
}
