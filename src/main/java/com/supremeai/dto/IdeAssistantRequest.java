package com.supremeai.dto;

import jakarta.validation.constraints.NotBlank;

public class IdeAssistantRequest {
  @NotBlank(message = "Prompt is required")
  private String prompt;

  private String context;

  private String provider = "";

  public IdeAssistantRequest() {}

  public IdeAssistantRequest(String prompt, String context, String provider) {
    this.prompt = prompt;
    this.context = context;
    this.provider = provider;
  }

  public String getPrompt() {
    return prompt;
  }

  public void setPrompt(String prompt) {
    this.prompt = prompt;
  }

  public String getContext() {
    return context;
  }

  public void setContext(String context) {
    this.context = context;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }
}
