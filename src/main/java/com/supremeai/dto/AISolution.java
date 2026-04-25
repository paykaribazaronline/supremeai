package com.supremeai.dto;

public class AISolution {
    private String providerId;
    private String solutionContent;
    private String generatedCode;
    private double selfScore;

    // Getters
    public String getProviderId() { return providerId; }
    public String getSolutionContent() { return solutionContent; }
    public String getGeneratedCode() { return generatedCode; }
    public double getSelfScore() { return selfScore; }

    // Setters
    public void setProviderId(String providerId) { this.providerId = providerId; }
    public void setSolutionContent(String solutionContent) { this.solutionContent = solutionContent; }
    public void setGeneratedCode(String generatedCode) { this.generatedCode = generatedCode; }
    public void setSelfScore(double selfScore) { this.selfScore = selfScore; }

    public double evaluate(AISolution other) {
        return other.getSolutionContent().equals(this.getSolutionContent()) ? 1.0 : 0.5;
    }
}
