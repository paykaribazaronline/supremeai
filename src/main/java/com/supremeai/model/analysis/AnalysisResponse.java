package com.supremeai.model.analysis;

import java.util.List;
import java.util.Map;

/** Response DTO for analysis results. */
public class AnalysisResponse {
  private String jobId;
  private String status;
  private Long durationMs;
  private String project;
  private int filesAnalyzed;
  private int totalFiles;
  private int totalFindings;
  private Map<String, Integer> summary;
  private List<AnalysisFinding> findings;
  private List<AnalysisFix> fixes;
  private boolean completed;
  private String errorMessage;
  private boolean ragUsed;
  private boolean incrementalUsed;
  private int changedFiles;
  private int cachedFindings;

  public AnalysisResponse() {}

  public AnalysisResponse(
      String jobId,
      String status,
      Long durationMs,
      String project,
      int filesAnalyzed,
      int totalFiles,
      int totalFindings,
      Map<String, Integer> summary,
      List<AnalysisFinding> findings,
      List<AnalysisFix> fixes,
      boolean completed,
      String errorMessage,
      boolean ragUsed,
      boolean incrementalUsed,
      int changedFiles,
      int cachedFindings) {
    this.jobId = jobId;
    this.status = status;
    this.durationMs = durationMs;
    this.project = project;
    this.filesAnalyzed = filesAnalyzed;
    this.totalFiles = totalFiles;
    this.totalFindings = totalFindings;
    this.summary = summary;
    this.findings = findings;
    this.fixes = fixes;
    this.completed = completed;
    this.errorMessage = errorMessage;
    this.ragUsed = ragUsed;
    this.incrementalUsed = incrementalUsed;
    this.changedFiles = changedFiles;
    this.cachedFindings = cachedFindings;
  }

  public String getJobId() {
    return jobId;
  }

  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Long getDurationMs() {
    return durationMs;
  }

  public void setDurationMs(Long durationMs) {
    this.durationMs = durationMs;
  }

  public String getProject() {
    return project;
  }

  public void setProject(String project) {
    this.project = project;
  }

  public int getFilesAnalyzed() {
    return filesAnalyzed;
  }

  public void setFilesAnalyzed(int filesAnalyzed) {
    this.filesAnalyzed = filesAnalyzed;
  }

  public int getTotalFiles() {
    return totalFiles;
  }

  public void setTotalFiles(int totalFiles) {
    this.totalFiles = totalFiles;
  }

  public int getTotalFindings() {
    return totalFindings;
  }

  public void setTotalFindings(int totalFindings) {
    this.totalFindings = totalFindings;
  }

  public Map<String, Integer> getSummary() {
    return summary;
  }

  public void setSummary(Map<String, Integer> summary) {
    this.summary = summary;
  }

  public List<AnalysisFinding> getFindings() {
    return findings;
  }

  public void setFindings(List<AnalysisFinding> findings) {
    this.findings = findings;
  }

  public List<AnalysisFix> getFixes() {
    return fixes;
  }

  public void setFixes(List<AnalysisFix> fixes) {
    this.fixes = fixes;
  }

  public boolean isCompleted() {
    return completed;
  }

  public void setCompleted(boolean completed) {
    this.completed = completed;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public boolean isRagUsed() {
    return ragUsed;
  }

  public void setRagUsed(boolean ragUsed) {
    this.ragUsed = ragUsed;
  }

  public boolean isIncrementalUsed() {
    return incrementalUsed;
  }

  public void setIncrementalUsed(boolean incrementalUsed) {
    this.incrementalUsed = incrementalUsed;
  }

  public int getChangedFiles() {
    return changedFiles;
  }

  public void setChangedFiles(int changedFiles) {
    this.changedFiles = changedFiles;
  }

  public int getCachedFindings() {
    return cachedFindings;
  }

  public void setCachedFindings(int cachedFindings) {
    this.cachedFindings = cachedFindings;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String jobId;
    private String status;
    private Long durationMs;
    private String project;
    private int filesAnalyzed;
    private int totalFiles;
    private int totalFindings;
    private Map<String, Integer> summary;
    private List<AnalysisFinding> findings;
    private List<AnalysisFix> fixes;
    private boolean completed;
    private String errorMessage;
    private boolean ragUsed;
    private boolean incrementalUsed;
    private int changedFiles;
    private int cachedFindings;

    public Builder jobId(String jobId) {
      this.jobId = jobId;
      return this;
    }

    public Builder status(String status) {
      this.status = status;
      return this;
    }

    public Builder durationMs(Long durationMs) {
      this.durationMs = durationMs;
      return this;
    }

    public Builder project(String project) {
      this.project = project;
      return this;
    }

    public Builder filesAnalyzed(int filesAnalyzed) {
      this.filesAnalyzed = filesAnalyzed;
      return this;
    }

    public Builder totalFiles(int totalFiles) {
      this.totalFiles = totalFiles;
      return this;
    }

    public Builder totalFindings(int totalFindings) {
      this.totalFindings = totalFindings;
      return this;
    }

    public Builder summary(Map<String, Integer> summary) {
      this.summary = summary;
      return this;
    }

    public Builder findings(List<AnalysisFinding> findings) {
      this.findings = findings;
      return this;
    }

    public Builder fixes(List<AnalysisFix> fixes) {
      this.fixes = fixes;
      return this;
    }

    public Builder completed(boolean completed) {
      this.completed = completed;
      return this;
    }

    public Builder errorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
      return this;
    }

    public Builder ragUsed(boolean ragUsed) {
      this.ragUsed = ragUsed;
      return this;
    }

    public Builder incrementalUsed(boolean incrementalUsed) {
      this.incrementalUsed = incrementalUsed;
      return this;
    }

    public Builder changedFiles(int changedFiles) {
      this.changedFiles = changedFiles;
      return this;
    }

    public Builder cachedFindings(int cachedFindings) {
      this.cachedFindings = cachedFindings;
      return this;
    }

    public AnalysisResponse build() {
      return new AnalysisResponse(
          jobId,
          status,
          durationMs,
          project,
          filesAnalyzed,
          totalFiles,
          totalFindings,
          summary,
          findings,
          fixes,
          completed,
          errorMessage,
          ragUsed,
          incrementalUsed,
          changedFiles,
          cachedFindings);
    }
  }
}
