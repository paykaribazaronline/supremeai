package com.supremeai.model.analysis;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;

@Document(collectionName = "analysis_fixes")
public class AnalysisFix {
    @DocumentId
    private String id;
    private String jobId;
    private String findingId;
    private String file;
    private int line;
    private String originalCode;
    private String fixedCode;
    private String explanation;
    private double confidence;
    private boolean applied;
    private String createdAt;

    public AnalysisFix() {}

    public AnalysisFix(String id, String jobId, String findingId, String file, int line, String originalCode, String fixedCode, String explanation, double confidence, boolean applied, String createdAt) {
        this.id = id;
        this.jobId = jobId;
        this.findingId = findingId;
        this.file = file;
        this.line = line;
        this.originalCode = originalCode;
        this.fixedCode = fixedCode;
        this.explanation = explanation;
        this.confidence = confidence;
        this.applied = applied;
        this.createdAt = createdAt;
    }

    public static AnalysisFixBuilder builder() {
        return new AnalysisFixBuilder();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getFindingId() { return findingId; }
    public void setFindingId(String findingId) { this.findingId = findingId; }

    public String getFile() { return file; }
    public void setFile(String file) { this.file = file; }

    public int getLine() { return line; }
    public void setLine(int line) { this.line = line; }

    public String getOriginalCode() { return originalCode; }
    public void setOriginalCode(String originalCode) { this.originalCode = originalCode; }

    public String getFixedCode() { return fixedCode; }
    public void setFixedCode(String fixedCode) { this.fixedCode = fixedCode; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public boolean isApplied() { return applied; }
    public void setApplied(boolean applied) { this.applied = applied; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public static class AnalysisFixBuilder {
        private String id;
        private String jobId;
        private String findingId;
        private String file;
        private int line;
        private String originalCode;
        private String fixedCode;
        private String explanation;
        private double confidence;
        private boolean applied;
        private String createdAt;

        public AnalysisFixBuilder id(String id) { this.id = id; return this; }
        public AnalysisFixBuilder jobId(String jobId) { this.jobId = jobId; return this; }
        public AnalysisFixBuilder findingId(String findingId) { this.findingId = findingId; return this; }
        public AnalysisFixBuilder file(String file) { this.file = file; return this; }
        public AnalysisFixBuilder line(int line) { this.line = line; return this; }
        public AnalysisFixBuilder originalCode(String originalCode) { this.originalCode = originalCode; return this; }
        public AnalysisFixBuilder fixedCode(String fixedCode) { this.fixedCode = fixedCode; return this; }
        public AnalysisFixBuilder explanation(String explanation) { this.explanation = explanation; return this; }
        public AnalysisFixBuilder confidence(double confidence) { this.confidence = confidence; return this; }
        public AnalysisFixBuilder applied(boolean applied) { this.applied = applied; return this; }
        public AnalysisFixBuilder createdAt(String createdAt) { this.createdAt = createdAt; return this; }

        public AnalysisFix build() {
            return new AnalysisFix(id, jobId, findingId, file, line, originalCode, fixedCode, explanation, confidence, applied, createdAt);
        }
    }
}
