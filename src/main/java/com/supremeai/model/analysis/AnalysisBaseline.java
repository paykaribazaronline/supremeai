package com.supremeai.model.analysis;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import java.util.List;

@Document(collectionName = "analysis_baselines")
public class AnalysisBaseline {
    @DocumentId
    private String id;
    private String projectId;
    private String commitHash;
    private String findingsHash;
    private List<AnalysisFinding> findings;
    private String createdAt;

    public AnalysisBaseline() {}

    public AnalysisBaseline(String id, String projectId, String commitHash, String findingsHash, List<AnalysisFinding> findings, String createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.commitHash = commitHash;
        this.findingsHash = findingsHash;
        this.findings = findings;
        this.createdAt = createdAt;
    }

    public static AnalysisBaselineBuilder builder() {
        return new AnalysisBaselineBuilder();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getCommitHash() { return commitHash; }
    public void setCommitHash(String commitHash) { this.commitHash = commitHash; }

    public String getFindingsHash() { return findingsHash; }
    public void setFindingsHash(String findingsHash) { this.findingsHash = findingsHash; }

    public List<AnalysisFinding> getFindings() { return findings; }
    public void setFindings(List<AnalysisFinding> findings) { this.findings = findings; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public static class AnalysisBaselineBuilder {
        private String id;
        private String projectId;
        private String commitHash;
        private String findingsHash;
        private List<AnalysisFinding> findings;
        private String createdAt;

        public AnalysisBaselineBuilder id(String id) { this.id = id; return this; }
        public AnalysisBaselineBuilder projectId(String projectId) { this.projectId = projectId; return this; }
        public AnalysisBaselineBuilder commitHash(String commitHash) { this.commitHash = commitHash; return this; }
        public AnalysisBaselineBuilder findingsHash(String findingsHash) { this.findingsHash = findingsHash; return this; }
        public AnalysisBaselineBuilder findings(List<AnalysisFinding> findings) { this.findings = findings; return this; }
        public AnalysisBaselineBuilder createdAt(String createdAt) { this.createdAt = createdAt; return this; }

        public AnalysisBaseline build() {
            return new AnalysisBaseline(id, projectId, commitHash, findingsHash, findings, createdAt);
        }
    }
}
