package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collectionName = "system_learning/domains")
public class KnowledgeDomain {
    @DocumentId
    private String id;
    private String name;
    private List<String> keywords;
    private Status status;
    private LocalDateTime lastUpdated;
    private Integer depth;
    private Integer nodesDiscovered;
    private Double averageConfidence;
    private List<String> sources;
    private Map<String, Object> metadata;

    public enum Status {
        IDLE, LEARNING, COMPLETE, PAUSED, ERROR
    }

    public KnowledgeDomain() {}

    public KnowledgeDomain(String name, List<String> keywords) {
        this.id = name.toLowerCase().replace(" ", "_");
        this.name = name;
        this.keywords = keywords;
        this.status = Status.IDLE;
        this.lastUpdated = LocalDateTime.now();
        this.depth = 0;
        this.nodesDiscovered = 0;
        this.averageConfidence = 0.0;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
    public Integer getDepth() { return depth; }
    public void setDepth(Integer depth) { this.depth = depth; }
    public Integer getNodesDiscovered() { return nodesDiscovered; }
    public void setNodesDiscovered(Integer nodesDiscovered) { this.nodesDiscovered = nodesDiscovered; }
    public Double getAverageConfidence() { return averageConfidence; }
    public void setAverageConfidence(Double averageConfidence) { this.averageConfidence = averageConfidence; }
    public List<String> getSources() { return sources; }
    public void setSources(List<String> sources) { this.sources = sources; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}