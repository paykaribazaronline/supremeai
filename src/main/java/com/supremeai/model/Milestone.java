package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import java.util.List;

@Document(collectionName = "milestones")
public class Milestone {
    @DocumentId
    private String id;
    private String name;
    private String title;
    private String timeline;
    private List<String> details;
    private Integer progress;
    private String color;
    private String icon;
    private Integer order;

    public Milestone() {}

    public Milestone(String id, String title, String timeline, Integer progress, String color, String icon, Integer order) {
        this.id = id;
        this.title = title;
        this.timeline = timeline;
        this.progress = progress;
        this.color = color;
        this.icon = icon;
        this.order = order;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getTimeline() { return timeline; }
    public void setTimeline(String timeline) { this.timeline = timeline; }
    public List<String> getDetails() { return details; }
    public void setDetails(List<String> details) { this.details = details; }
    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Integer getOrder() { return order; }
    public void setOrder(Integer order) { this.order = order; }
}
