package com.supremeai.model.analysis;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;

/** Represents a single security/quality finding. */
@Document(collectionName = "analysis_findings")
public class AnalysisFinding {
  @DocumentId private String id;
  private String jobId;
  private String severity; // CRITICAL, HIGH, MEDIUM, LOW, INFO
  private String category; // SECRETS, SQL_INJECTION, XSS, PATH_TRAVERSAL, etc.
  private String file;
  private int line;
  private String message;
  private String suggestion;
  private String pattern;
  private String codeSnippet;

  public AnalysisFinding() {}

  public AnalysisFinding(
      String id,
      String jobId,
      String severity,
      String category,
      String file,
      int line,
      String message,
      String suggestion,
      String pattern,
      String codeSnippet) {
    this.id = id;
    this.jobId = jobId;
    this.severity = severity;
    this.category = category;
    this.file = file;
    this.line = line;
    this.message = message;
    this.suggestion = suggestion;
    this.pattern = pattern;
    this.codeSnippet = codeSnippet;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getJobId() {
    return jobId;
  }

  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  public String getSeverity() {
    return severity;
  }

  public void setSeverity(String severity) {
    this.severity = severity;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getFile() {
    return file;
  }

  public void setFile(String file) {
    this.file = file;
  }

  public int getLine() {
    return line;
  }

  public void setLine(int line) {
    this.line = line;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getSuggestion() {
    return suggestion;
  }

  public void setSuggestion(String suggestion) {
    this.suggestion = suggestion;
  }

  public String getPattern() {
    return pattern;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  public String getCodeSnippet() {
    return codeSnippet;
  }

  public void setCodeSnippet(String codeSnippet) {
    this.codeSnippet = codeSnippet;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String id;
    private String jobId;
    private String severity;
    private String category;
    private String file;
    private int line;
    private String message;
    private String suggestion;
    private String pattern;
    private String codeSnippet;

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder jobId(String jobId) {
      this.jobId = jobId;
      return this;
    }

    public Builder severity(String severity) {
      this.severity = severity;
      return this;
    }

    public Builder category(String category) {
      this.category = category;
      return this;
    }

    public Builder file(String file) {
      this.file = file;
      return this;
    }

    public Builder line(int line) {
      this.line = line;
      return this;
    }

    public Builder message(String message) {
      this.message = message;
      return this;
    }

    public Builder suggestion(String suggestion) {
      this.suggestion = suggestion;
      return this;
    }

    public Builder pattern(String pattern) {
      this.pattern = pattern;
      return this;
    }

    public Builder codeSnippet(String codeSnippet) {
      this.codeSnippet = codeSnippet;
      return this;
    }

    public AnalysisFinding build() {
      return new AnalysisFinding(
          id, jobId, severity, category, file, line, message, suggestion, pattern, codeSnippet);
    }
  }
}
