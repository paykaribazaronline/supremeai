package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import java.time.LocalDateTime;

@Document(collectionName = "reasoning_logs")
public class ReasoningLog {
    @DocumentId
    private String id;
    private String taskId;
    private String decision;
    private String reason;
    private String modelName;
    private String status;
    private LocalDateTime timestamp;
    private String additionalMetadata;

    public ReasoningLog() {}

    public ReasoningLog(String id, String taskId, String decision, String reason, String modelName, String status, LocalDateTime timestamp, String additionalMetadata) {
        this.id = id;
        this.taskId = taskId;
        this.decision = decision;
        this.reason = reason;
        this.modelName = modelName;
        this.status = status;
        this.timestamp = timestamp;
        this.additionalMetadata = additionalMetadata;
    }

    public static ReasoningLogBuilder builder() {
        return new ReasoningLogBuilder();
    }

    public static class ReasoningLogBuilder {
        private String id;
        private String taskId;
        private String decision;
        private String reason;
        private String modelName;
        private String status;
        private LocalDateTime timestamp;
        private String additionalMetadata;

        public ReasoningLogBuilder id(String id) { this.id = id; return this; }
        public ReasoningLogBuilder taskId(String taskId) { this.taskId = taskId; return this; }
        public ReasoningLogBuilder decision(String decision) { this.decision = decision; return this; }
        public ReasoningLogBuilder reason(String reason) { this.reason = reason; return this; }
        public ReasoningLogBuilder modelName(String modelName) { this.modelName = modelName; return this; }
        public ReasoningLogBuilder status(String status) { this.status = status; return this; }
        public ReasoningLogBuilder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }
        public ReasoningLogBuilder additionalMetadata(String additionalMetadata) { this.additionalMetadata = additionalMetadata; return this; }

        public ReasoningLog build() {
            return new ReasoningLog(id, taskId, decision, reason, modelName, status, timestamp, additionalMetadata);
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getAdditionalMetadata() { return additionalMetadata; }
    public void setAdditionalMetadata(String additionalMetadata) { this.additionalMetadata = additionalMetadata; }
}
