package com.supremeai.dto;

import java.util.List;

public class ClarificationResponse {
    private boolean needsClarification;
    private List<String> questions;
    private String suggestedApproach;
    private String directResponse;

    // Getters
    public boolean isNeedsClarification() { return needsClarification; }
    public List<String> getQuestions() { return questions; }
    public String getSuggestedApproach() { return suggestedApproach; }
    public String getDirectResponse() { return directResponse; }

    // Setters
    public void setNeedsClarification(boolean needsClarification) { this.needsClarification = needsClarification; }
    public void setQuestions(List<String> questions) { this.questions = questions; }
    public void setSuggestedApproach(String suggestedApproach) { this.suggestedApproach = suggestedApproach; }
    public void setDirectResponse(String directResponse) { this.directResponse = directResponse; }

    // Builder pattern
    public static ClarificationResponse builder() { return new ClarificationResponse(); }

    public ClarificationResponse needsClarification(boolean needsClarification) { this.needsClarification = needsClarification; return this; }
    public ClarificationResponse questions(List<String> questions) { this.questions = questions; return this; }
    public ClarificationResponse suggestedApproach(String suggestedApproach) { this.suggestedApproach = suggestedApproach; return this; }
    public ClarificationResponse directResponse(String directResponse) { this.directResponse = directResponse; return this; }
    public ClarificationResponse build() { return this; }
}
