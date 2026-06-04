package com.supremeai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TranslationRequest {
  @NotBlank(message = "Text is required")
  @Size(min = 1, max = 10000, message = "Text must be between 1 and 10000 characters")
  private String text;

  @NotBlank(message = "Source language is required")
  @Size(min = 2, max = 10, message = "Source language code must be between 2 and 10 characters")
  private String fromLanguage;

  @NotBlank(message = "Target language is required")
  @Size(min = 2, max = 10, message = "Target language code must be between 2 and 10 characters")
  private String toLanguage;

  public TranslationRequest() {}

  public TranslationRequest(String text, String fromLanguage, String toLanguage) {
    this.text = text;
    this.fromLanguage = fromLanguage;
    this.toLanguage = toLanguage;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String getFromLanguage() {
    return fromLanguage;
  }

  public void setFromLanguage(String fromLanguage) {
    this.fromLanguage = fromLanguage;
  }

  public String getToLanguage() {
    return toLanguage;
  }

  public void setToLanguage(String toLanguage) {
    this.toLanguage = toLanguage;
  }
}
