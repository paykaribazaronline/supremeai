package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.ServerTimestamp;
import java.time.LocalDateTime;
import java.util.Map;

/** Generated application stored in Firestore. Collection: "generated_apps" */
public class GeneratedApp {

  @DocumentId private String appId;

  private String userId;

  private String name;

  private String description;

  private String platform; // WEB, IOS, ANDROID, DESKTOP

  private String language; // SwiftUI, Kotlin, React, Tauri

  private String techStack; // Detailed stack info

  private String htmlContent; // For web apps: complete HTML

  private Map<String, String> sourceFiles; // filename → content for multi-file projects

  private String version;

  private String status; // GENERATED, DEPLOYED, ERROR

  private String errorMessage;

  private byte[] screenshot;

  private String requestId;

  @ServerTimestamp private LocalDateTime createdAt;

  @ServerTimestamp private LocalDateTime updatedAt;

  // No-arg constructor
  public GeneratedApp() {}

  // All-args constructor
  public GeneratedApp(
      String appId,
      String userId,
      String name,
      String description,
      String platform,
      String language,
      String techStack,
      String htmlContent,
      Map<String, String> sourceFiles,
      String version,
      String status,
      String errorMessage,
      byte[] screenshot,
      String requestId,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.appId = appId;
    this.userId = userId;
    this.name = name;
    this.description = description;
    this.platform = platform;
    this.language = language;
    this.techStack = techStack;
    this.htmlContent = htmlContent;
    this.sourceFiles = sourceFiles;
    this.version = version;
    this.status = status;
    this.errorMessage = errorMessage;
    this.screenshot = screenshot;
    this.requestId = requestId;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  // Backward compatibility constructor
  public GeneratedApp(String appId, String userId, String platform, String language) {
    this.appId = appId;
    this.userId = userId;
    this.platform = platform;
    this.language = language;
    this.version = "1.0.0";
    this.status = "GENERATED";
    this.createdAt = LocalDateTime.now();
  }

  public String getId() {
    return appId;
  }

  // Getters and Setters
  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getPlatform() {
    return platform;
  }

  public void setPlatform(String platform) {
    this.platform = platform;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public String getTechStack() {
    return techStack;
  }

  public void setTechStack(String techStack) {
    this.techStack = techStack;
  }

  public String getHtmlContent() {
    return htmlContent;
  }

  public void setHtmlContent(String htmlContent) {
    this.htmlContent = htmlContent;
  }

  public Map<String, String> getSourceFiles() {
    return sourceFiles;
  }

  public void setSourceFiles(Map<String, String> sourceFiles) {
    this.sourceFiles = sourceFiles;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public byte[] getScreenshot() {
    return screenshot;
  }

  public void setScreenshot(byte[] screenshot) {
    this.screenshot = screenshot;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  // Builder pattern
  public static GeneratedAppBuilder builder() {
    return new GeneratedAppBuilder();
  }

  public static class GeneratedAppBuilder {
    private String appId;
    private String userId;
    private String name;
    private String description;
    private String platform;
    private String language;
    private String techStack;
    private String htmlContent;
    private Map<String, String> sourceFiles;
    private String version;
    private String status;
    private String errorMessage;
    private byte[] screenshot;
    private String requestId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public GeneratedAppBuilder appId(String appId) {
      this.appId = appId;
      return this;
    }

    public GeneratedAppBuilder userId(String userId) {
      this.userId = userId;
      return this;
    }

    public GeneratedAppBuilder name(String name) {
      this.name = name;
      return this;
    }

    public GeneratedAppBuilder description(String description) {
      this.description = description;
      return this;
    }

    public GeneratedAppBuilder platform(String platform) {
      this.platform = platform;
      return this;
    }

    public GeneratedAppBuilder language(String language) {
      this.language = language;
      return this;
    }

    public GeneratedAppBuilder techStack(String techStack) {
      this.techStack = techStack;
      return this;
    }

    public GeneratedAppBuilder htmlContent(String htmlContent) {
      this.htmlContent = htmlContent;
      return this;
    }

    public GeneratedAppBuilder sourceFiles(Map<String, String> sourceFiles) {
      this.sourceFiles = sourceFiles;
      return this;
    }

    public GeneratedAppBuilder version(String version) {
      this.version = version;
      return this;
    }

    public GeneratedAppBuilder status(String status) {
      this.status = status;
      return this;
    }

    public GeneratedAppBuilder errorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
      return this;
    }

    public GeneratedAppBuilder screenshot(byte[] screenshot) {
      this.screenshot = screenshot;
      return this;
    }

    public GeneratedAppBuilder requestId(String requestId) {
      this.requestId = requestId;
      return this;
    }

    public GeneratedAppBuilder createdAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public GeneratedAppBuilder updatedAt(LocalDateTime updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }

    public GeneratedApp build() {
      GeneratedApp app = new GeneratedApp();
      app.setAppId(appId);
      app.setUserId(userId);
      app.setName(name);
      app.setDescription(description);
      app.setPlatform(platform);
      app.setLanguage(language);
      app.setTechStack(techStack);
      app.setHtmlContent(htmlContent);
      app.setSourceFiles(sourceFiles);
      app.setVersion(version);
      app.setStatus(status);
      app.setErrorMessage(errorMessage);
      app.setScreenshot(screenshot);
      app.setRequestId(requestId);
      app.setCreatedAt(createdAt);
      app.setUpdatedAt(updatedAt);
      return app;
    }
  }
}
