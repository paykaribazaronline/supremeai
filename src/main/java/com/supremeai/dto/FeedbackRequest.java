package com.supremeai.dto;

import jakarta.validation.constraints.NotBlank;

public class FeedbackRequest {
  @NotBlank(message = "Message ID is required")
  private String messageId;

  private boolean helpful;

  private String userMessage;

  private String aiResponse;

  public FeedbackRequest() {}

  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }

  public boolean isHelpful() {
    return helpful;
  }

  public void setHelpful(boolean helpful) {
    this.helpful = helpful;
  }

  public String getUserMessage() {
    return userMessage;
  }

  public void setUserMessage(String userMessage) {
    this.userMessage = userMessage;
  }

  public String getAiResponse() {
    return aiResponse;
  }

  public void setAiResponse(String aiResponse) {
    this.aiResponse = aiResponse;
  }
}
