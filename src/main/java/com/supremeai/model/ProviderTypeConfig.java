package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import java.util.List;
import java.util.Map;

/**
 * ProviderTypeConfig — Firestore document for dynamic provider type definitions. Collection:
 * provider_types
 *
 * <p>This replaces all hardcoded switch/case provider mappings in AIProviderFactory. Admin can
 * add/edit/delete provider types via dashboard without code changes.
 */
@Document(collectionName = "provider_types")
public class ProviderTypeConfig {

  @DocumentId private String typeId;

  private String displayName;
  private String defaultBaseUrl;
  private String defaultModel;
  private String authType;
  private List<String> supportedModels;
  private List<String> capabilities;
  private List<String> defaultRoles;
  private List<String> keywords;
  private Map<String, Object> extraConfig;
  private boolean enabled;
  private int priority;
  private double costPer1kTokens;

  public ProviderTypeConfig() {}

  public String getTypeId() {
    return typeId;
  }

  public void setTypeId(String typeId) {
    this.typeId = typeId;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getDefaultBaseUrl() {
    return defaultBaseUrl;
  }

  public void setDefaultBaseUrl(String defaultBaseUrl) {
    this.defaultBaseUrl = defaultBaseUrl;
  }

  public String getDefaultModel() {
    return defaultModel;
  }

  public void setDefaultModel(String defaultModel) {
    this.defaultModel = defaultModel;
  }

  public String getAuthType() {
    return authType;
  }

  public void setAuthType(String authType) {
    this.authType = authType;
  }

  public List<String> getSupportedModels() {
    return supportedModels;
  }

  public void setSupportedModels(List<String> supportedModels) {
    this.supportedModels = supportedModels;
  }

  public List<String> getCapabilities() {
    return capabilities;
  }

  public void setCapabilities(List<String> capabilities) {
    this.capabilities = capabilities;
  }

  public List<String> getDefaultRoles() {
    return defaultRoles;
  }

  public void setDefaultRoles(List<String> defaultRoles) {
    this.defaultRoles = defaultRoles;
  }

  public List<String> getKeywords() {
    return keywords;
  }

  public void setKeywords(List<String> keywords) {
    this.keywords = keywords;
  }

  public Map<String, Object> getExtraConfig() {
    return extraConfig;
  }

  public void setExtraConfig(Map<String, Object> extraConfig) {
    this.extraConfig = extraConfig;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int getPriority() {
    return priority;
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }

  public double getCostPer1kTokens() {
    return costPer1kTokens;
  }

  public void setCostPer1kTokens(double costPer1kTokens) {
    this.costPer1kTokens = costPer1kTokens;
  }
}
