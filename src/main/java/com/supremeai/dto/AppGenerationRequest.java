package com.supremeai.dto;

import com.supremeai.model.EntityDefinition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public class AppGenerationRequest {

  @NotBlank(message = "App name is required")
  @Size(min = 2, max = 50, message = "App name must be between 2 and 50 characters")
  private String name;

  @Size(max = 500, message = "Description must not exceed 500 characters")
  private String description;

  private String platform = "fullstack";

  private String database = "PostgreSQL";

  private String type = "project";

  private boolean useAI = false;

  private List<EntityDefinition> entities;

  private Map<String, Object> additionalContext;

  public AppGenerationRequest() {}

  public AppGenerationRequest(
      String name,
      String description,
      String platform,
      String database,
      String type,
      boolean useAI,
      List<EntityDefinition> entities,
      Map<String, Object> additionalContext) {
    this.name = name;
    this.description = description;
    this.platform = platform;
    this.database = database;
    this.type = type;
    this.useAI = useAI;
    this.entities = entities;
    this.additionalContext = additionalContext;
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

  public String getDatabase() {
    return database;
  }

  public void setDatabase(String database) {
    this.database = database;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public boolean isUseAI() {
    return useAI;
  }

  public void setUseAI(boolean useAI) {
    this.useAI = useAI;
  }

  public List<EntityDefinition> getEntities() {
    return entities;
  }

  public void setEntities(List<EntityDefinition> entities) {
    this.entities = entities;
  }

  public Map<String, Object> getAdditionalContext() {
    return additionalContext;
  }

  public void setAdditionalContext(Map<String, Object> additionalContext) {
    this.additionalContext = additionalContext;
  }
}
