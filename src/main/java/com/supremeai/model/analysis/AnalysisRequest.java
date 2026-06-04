package com.supremeai.model.analysis;

import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

/** Represents an analysis job request. */
public class AnalysisRequest {
  private String projectType;
  private String gitUrl;
  private String branch;
  private MultipartFile zipFile;
  private boolean includeDependencies;
  private Map<String, Boolean> agents;
  private Integer maxFiles;
  private Long maxSizeBytes;
  private boolean ragEnabled;
  private boolean incrementalEnabled;
  private boolean fixesEnabled;
  private String baselineCommit;
  private String projectId;

  public AnalysisRequest() {}

  public AnalysisRequest(
      String projectType,
      String gitUrl,
      String branch,
      MultipartFile zipFile,
      boolean includeDependencies,
      Map<String, Boolean> agents,
      Integer maxFiles,
      Long maxSizeBytes,
      boolean ragEnabled,
      boolean incrementalEnabled,
      boolean fixesEnabled,
      String baselineCommit,
      String projectId) {
    this.projectType = projectType;
    this.gitUrl = gitUrl;
    this.branch = branch;
    this.zipFile = zipFile;
    this.includeDependencies = includeDependencies;
    this.agents = agents;
    this.maxFiles = maxFiles;
    this.maxSizeBytes = maxSizeBytes;
    this.ragEnabled = ragEnabled;
    this.incrementalEnabled = incrementalEnabled;
    this.fixesEnabled = fixesEnabled;
    this.baselineCommit = baselineCommit;
    this.projectId = projectId;
  }

  public String getProjectType() {
    return projectType;
  }

  public void setProjectType(String projectType) {
    this.projectType = projectType;
  }

  public String getGitUrl() {
    return gitUrl;
  }

  public void setGitUrl(String gitUrl) {
    this.gitUrl = gitUrl;
  }

  public String getBranch() {
    return branch;
  }

  public void setBranch(String branch) {
    this.branch = branch;
  }

  public MultipartFile getZipFile() {
    return zipFile;
  }

  public void setZipFile(MultipartFile zipFile) {
    this.zipFile = zipFile;
  }

  public boolean isIncludeDependencies() {
    return includeDependencies;
  }

  public void setIncludeDependencies(boolean includeDependencies) {
    this.includeDependencies = includeDependencies;
  }

  public Map<String, Boolean> getAgents() {
    return agents;
  }

  public void setAgents(Map<String, Boolean> agents) {
    this.agents = agents;
  }

  public Integer getMaxFiles() {
    return maxFiles;
  }

  public void setMaxFiles(Integer maxFiles) {
    this.maxFiles = maxFiles;
  }

  public Long getMaxSizeBytes() {
    return maxSizeBytes;
  }

  public void setMaxSizeBytes(Long maxSizeBytes) {
    this.maxSizeBytes = maxSizeBytes;
  }

  public boolean isRagEnabled() {
    return ragEnabled;
  }

  public void setRagEnabled(boolean ragEnabled) {
    this.ragEnabled = ragEnabled;
  }

  public boolean isIncrementalEnabled() {
    return incrementalEnabled;
  }

  public void setIncrementalEnabled(boolean incrementalEnabled) {
    this.incrementalEnabled = incrementalEnabled;
  }

  public boolean isFixesEnabled() {
    return fixesEnabled;
  }

  public void setFixesEnabled(boolean fixesEnabled) {
    this.fixesEnabled = fixesEnabled;
  }

  public String getBaselineCommit() {
    return baselineCommit;
  }

  public void setBaselineCommit(String baselineCommit) {
    this.baselineCommit = baselineCommit;
  }

  public String getProjectId() {
    return projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String projectType;
    private String gitUrl;
    private String branch;
    private MultipartFile zipFile;
    private boolean includeDependencies;
    private Map<String, Boolean> agents;
    private Integer maxFiles;
    private Long maxSizeBytes;
    private boolean ragEnabled;
    private boolean incrementalEnabled;
    private boolean fixesEnabled;
    private String baselineCommit;
    private String projectId;

    public Builder projectType(String projectType) {
      this.projectType = projectType;
      return this;
    }

    public Builder gitUrl(String gitUrl) {
      this.gitUrl = gitUrl;
      return this;
    }

    public Builder branch(String branch) {
      this.branch = branch;
      return this;
    }

    public Builder zipFile(MultipartFile zipFile) {
      this.zipFile = zipFile;
      return this;
    }

    public Builder includeDependencies(boolean includeDependencies) {
      this.includeDependencies = includeDependencies;
      return this;
    }

    public Builder agents(Map<String, Boolean> agents) {
      this.agents = agents;
      return this;
    }

    public Builder maxFiles(Integer maxFiles) {
      this.maxFiles = maxFiles;
      return this;
    }

    public Builder maxSizeBytes(Long maxSizeBytes) {
      this.maxSizeBytes = maxSizeBytes;
      return this;
    }

    public Builder ragEnabled(boolean ragEnabled) {
      this.ragEnabled = ragEnabled;
      return this;
    }

    public Builder incrementalEnabled(boolean incrementalEnabled) {
      this.incrementalEnabled = incrementalEnabled;
      return this;
    }

    public Builder fixesEnabled(boolean fixesEnabled) {
      this.fixesEnabled = fixesEnabled;
      return this;
    }

    public Builder baselineCommit(String baselineCommit) {
      this.baselineCommit = baselineCommit;
      return this;
    }

    public Builder projectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    public AnalysisRequest build() {
      return new AnalysisRequest(
          projectType,
          gitUrl,
          branch,
          zipFile,
          includeDependencies,
          agents,
          maxFiles,
          maxSizeBytes,
          ragEnabled,
          incrementalEnabled,
          fixesEnabled,
          baselineCommit,
          projectId);
    }
  }
}
