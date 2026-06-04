package com.supremeai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UserRequest {
  @NotBlank(message = "Description is required")
  @Size(min = 1, max = 500, message = "Description must be between 1 and 500 characters")
  private String description;

  @NotNull(message = "Language preference is required")
  private LanguagePreference languagePreference;

  @NotBlank(message = "User ID is required")
  @Size(min = 1, max = 255, message = "User ID must be between 1 and 255 characters")
  private String userId;

  // Getters
  public String getDescription() {
    return description;
  }

  public LanguagePreference getLanguagePreference() {
    return languagePreference;
  }

  public String getUserId() {
    return userId;
  }

  // Setters
  public void setDescription(String description) {
    this.description = description;
  }

  public void setLanguagePreference(LanguagePreference languagePreference) {
    this.languagePreference = languagePreference;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  // Builder pattern
  public static UserRequest builder() {
    return new UserRequest();
  }

  public UserRequest description(String description) {
    this.description = description;
    return this;
  }

  public UserRequest languagePreference(LanguagePreference languagePreference) {
    this.languagePreference = languagePreference;
    return this;
  }

  public UserRequest userId(String userId) {
    this.userId = userId;
    return this;
  }

  public UserRequest build() {
    return this;
  }
}
