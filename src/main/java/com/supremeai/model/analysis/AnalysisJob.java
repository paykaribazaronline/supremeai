package com.supremeai.model.analysis;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;

import java.time.Instant;
import java.util.Map;

/**
 * Represents an analysis job entity for Firestore storage.
 */
@Document(collectionName = "analysis_jobs")
public class AnalysisJob {
    @DocumentId
    private String id;
    private String projectName;
    private String projectType;
    private String gitUrl;
    private String status; // PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    private Instant startTime;
    private Instant endTime;
    private Long durationMs;
    private String errorMessage;
    private int filesAnalyzed;
    private int totalFindings;
    private Map<String, Integer> findingsBySeverity;
    private boolean completed;
    private String initiatedBy;

    public AnalysisJob() {}

    public AnalysisJob(String id, String projectName, String projectType, String gitUrl, String status, Instant startTime, Instant endTime, Long durationMs, String errorMessage, int filesAnalyzed, int totalFindings, Map<String, Integer> findingsBySeverity, boolean completed, String initiatedBy) {
        this.id = id;
        this.projectName = projectName;
        this.projectType = projectType;
        this.gitUrl = gitUrl;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMs = durationMs;
        this.errorMessage = errorMessage;
        this.filesAnalyzed = filesAnalyzed;
        this.totalFindings = totalFindings;
        this.findingsBySeverity = findingsBySeverity;
        this.completed = completed;
        this.initiatedBy = initiatedBy;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getProjectType() { return projectType; }
    public void setProjectType(String projectType) { this.projectType = projectType; }
    public String getGitUrl() { return gitUrl; }
    public void setGitUrl(String gitUrl) { this.gitUrl = gitUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public int getFilesAnalyzed() { return filesAnalyzed; }
    public void setFilesAnalyzed(int filesAnalyzed) { this.filesAnalyzed = filesAnalyzed; }
    public int getTotalFindings() { return totalFindings; }
    public void setTotalFindings(int totalFindings) { this.totalFindings = totalFindings; }
    public Map<String, Integer> getFindingsBySeverity() { return findingsBySeverity; }
    public void setFindingsBySeverity(Map<String, Integer> findingsBySeverity) { this.findingsBySeverity = findingsBySeverity; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public String getInitiatedBy() { return initiatedBy; }
    public void setInitiatedBy(String initiatedBy) { this.initiatedBy = initiatedBy; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String projectName;
        private String projectType;
        private String gitUrl;
        private String status;
        private Instant startTime;
        private Instant endTime;
        private Long durationMs;
        private String errorMessage;
        private int filesAnalyzed;
        private int totalFindings;
        private Map<String, Integer> findingsBySeverity;
        private boolean completed;
        private String initiatedBy;

        public Builder id(String id) { this.id = id; return this; }
        public Builder projectName(String projectName) { this.projectName = projectName; return this; }
        public Builder projectType(String projectType) { this.projectType = projectType; return this; }
        public Builder gitUrl(String gitUrl) { this.gitUrl = gitUrl; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder startTime(Instant startTime) { this.startTime = startTime; return this; }
        public Builder endTime(Instant endTime) { this.endTime = endTime; return this; }
        public Builder durationMs(Long durationMs) { this.durationMs = durationMs; return this; }
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public Builder filesAnalyzed(int filesAnalyzed) { this.filesAnalyzed = filesAnalyzed; return this; }
        public Builder totalFindings(int totalFindings) { this.totalFindings = totalFindings; return this; }
        public Builder findingsBySeverity(Map<String, Integer> findingsBySeverity) { this.findingsBySeverity = findingsBySeverity; return this; }
        public Builder completed(boolean completed) { this.completed = completed; return this; }
        public Builder initiatedBy(String initiatedBy) { this.initiatedBy = initiatedBy; return this; }

        public AnalysisJob build() {
            return new AnalysisJob(id, projectName, projectType, gitUrl, status, startTime, endTime, durationMs, errorMessage, filesAnalyzed, totalFindings, findingsBySeverity, completed, initiatedBy);
        }
    }
}
