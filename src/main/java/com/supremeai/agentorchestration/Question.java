package com.supremeai.agentorchestration;

public class Question {
    private String key;
    private String text;
    private String priority; // CRITICAL, HIGH, MEDIUM, LOW

    public Question() {}

    public Question(String key, String text, String priority) {
        this.key = key;
        this.text = text;
        this.priority = priority;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
}