package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(collectionName = "system_learning")
public class SystemLearning {
    // Firestore document id - do NOT use @DocumentId if 'id' field exists in document data
    // Use document name as id instead
    private String id;
    private String topic;
    private String category;
    private String content;
    private List<String> sources;
    private Double confidenceScore;
    private Map<String, Object> metadata;
    private LocalDateTime learnedAt;
    private boolean permanent;

    // New fields for enhanced learning
    private String learningType; // NLP, MULTIMODAL, ECOSYSTEM, APP_GENERATION, PREDICTIVE
    private Map<String, Object> inputData;
    private Map<String, Object> outputData;
    private Boolean success;
    private Double qualityScore;
    private Integer timesApplied;
    private String relatedProvider;
    private List<String> tags;

    // Additional fields for error tracking and system learning
    private String type;
    private String resolution;
    private String context;
    private Integer errorCount;
    private Boolean resolved;
    private LocalDateTime timestamp;
    private String severity;
    private List<String> solutions;

    public SystemLearning() {}

    public SystemLearning(String id, String topic, String category, String content) {
        this.id = id;
        this.topic = topic;
        this.category = category;
        this.content = content;
        this.learnedAt = LocalDateTime.now();
        this.timesApplied = 0;
        this.learningType = "GENERAL";
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<String> getSources() { return sources; }
    public void setSources(List<String> sources) { this.sources = sources; }
    public Double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(Double confidenceScore) { this.confidenceScore = confidenceScore; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public LocalDateTime getLearnedAt() { return learnedAt; }
    public void setLearnedAt(LocalDateTime learnedAt) { this.learnedAt = learnedAt; }
    public boolean isPermanent() { return permanent; }
    public void setPermanent(boolean permanent) { this.permanent = permanent; }

    // New getters and setters
    public String getLearningType() { return learningType; }
    public void setLearningType(String learningType) { this.learningType = learningType; }
    public Map<String, Object> getInputData() { return inputData; }
    public void setInputData(Map<String, Object> inputData) { this.inputData = inputData; }
    public Map<String, Object> getOutputData() { return outputData; }
    public void setOutputData(Map<String, Object> outputData) { this.outputData = outputData; }
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    public Double getQualityScore() { return qualityScore; }
    public void setQualityScore(Double qualityScore) { this.qualityScore = qualityScore; }
    public Integer getTimesApplied() { return timesApplied; }
    public void setTimesApplied(Integer timesApplied) { this.timesApplied = timesApplied; }
    public String getRelatedProvider() { return relatedProvider; }
    public void setRelatedProvider(String relatedProvider) { this.relatedProvider = relatedProvider; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    // Getters and setters for additional fields
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }
    public Integer getErrorCount() { return errorCount; }
    public void setErrorCount(Integer errorCount) { this.errorCount = errorCount; }
    public Boolean getResolved() { return resolved; }
    public void setResolved(Boolean resolved) { this.resolved = resolved; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public List<String> getSolutions() { return solutions; }
    public void setSolutions(List<String> solutions) { this.solutions = solutions; }

    // Additional fields for enhanced learning
    private String title;
    private Double confidence;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String learnedFrom;
    private LocalDateTime lastUsed;
    private Long useCount;
    private Long successCount;
    private Long failureCount;
    private String lastFeedback;
    private Boolean obsolete;
    private Boolean critical;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getLearnedFrom() { return learnedFrom; }
    public void setLearnedFrom(String learnedFrom) { this.learnedFrom = learnedFrom; }
    public LocalDateTime getLastUsed() { return lastUsed; }
    public void setLastUsed(LocalDateTime lastUsed) { this.lastUsed = lastUsed; }
    public Long getUseCount() { return useCount; }
    public void setUseCount(Long useCount) { this.useCount = useCount; }
    public Long getSuccessCount() { return successCount; }
    public void setSuccessCount(Long successCount) { this.successCount = successCount; }
    public Long getFailureCount() { return failureCount; }
    public void setFailureCount(Long failureCount) { this.failureCount = failureCount; }
    public String getLastFeedback() { return lastFeedback; }
    public void setLastFeedback(String lastFeedback) { this.lastFeedback = lastFeedback; }
    public Boolean getObsolete() { return obsolete; }
    public void setObsolete(Boolean obsolete) { this.obsolete = obsolete; }
    public Boolean getCritical() { return critical; }
    public void setCritical(Boolean critical) { this.critical = critical; }
}
