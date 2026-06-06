package com.supremeai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatRequest {
  @NotBlank(message = "Message is required")
  private String message;

  private boolean skipValidation = false;

  private String agentId;

  private String sessionId;

  private List<Map<String, Object>> messages;

  public ChatRequest() {}

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public boolean isSkipValidation() {
    return skipValidation;
  }

  public void setSkipValidation(boolean skipValidation) {
    this.skipValidation = skipValidation;
  }

  public String getAgentId() {
    return agentId;
  }

  public void setAgentId(String agentId) {
    this.agentId = agentId;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public List<Map<String, Object>> getMessages() {
    return messages;
  }

  public void setMessages(List<Map<String, Object>> messages) {
    this.messages = messages;
  }
}
