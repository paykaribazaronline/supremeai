package com.supremeai.dto;

import java.util.Map;

public class PubSubMessageEnvelope {
  private Message message;
  private String subscription;

  public PubSubMessageEnvelope() {}

  public PubSubMessageEnvelope(Message message, String subscription) {
    this.message = message;
    this.subscription = subscription;
  }

  public Message getMessage() {
    return message;
  }

  public void setMessage(Message message) {
    this.message = message;
  }

  public String getSubscription() {
    return subscription;
  }

  public void setSubscription(String subscription) {
    this.subscription = subscription;
  }

  public static class Message {
    private Map<String, String> attributes;
    private String data;
    private String messageId;
    private String publishTime;

    public Message() {}

    public Message(
        Map<String, String> attributes, String data, String messageId, String publishTime) {
      this.attributes = attributes;
      this.data = data;
      this.messageId = messageId;
      this.publishTime = publishTime;
    }

    public Map<String, String> getAttributes() {
      return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
      this.attributes = attributes;
    }

    public String getData() {
      return data;
    }

    public void setData(String data) {
      this.data = data;
    }

    public String getMessageId() {
      return messageId;
    }

    public void setMessageId(String messageId) {
      this.messageId = messageId;
    }

    public String getPublishTime() {
      return publishTime;
    }

    public void setPublishTime(String publishTime) {
      this.publishTime = publishTime;
    }
  }
}
