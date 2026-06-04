package com.supremeai.dto;

public class AISolution {
  private String providerId;
  private String solutionContent;
  private String generatedCode;
  private double selfScore;

  public AISolution() {}

  public AISolution(
      String providerId, String solutionContent, String generatedCode, double selfScore) {
    this.providerId = providerId;
    this.solutionContent = solutionContent;
    this.generatedCode = generatedCode;
    this.selfScore = selfScore;
  }

  public static AISolutionBuilder builder() {
    return new AISolutionBuilder();
  }

  public String getProviderId() {
    return providerId;
  }

  public void setProviderId(String providerId) {
    this.providerId = providerId;
  }

  public String getSolutionContent() {
    return solutionContent;
  }

  public void setSolutionContent(String solutionContent) {
    this.solutionContent = solutionContent;
  }

  public String getGeneratedCode() {
    return generatedCode;
  }

  public void setGeneratedCode(String generatedCode) {
    this.generatedCode = generatedCode;
  }

  public double getSelfScore() {
    return selfScore;
  }

  public void setSelfScore(double selfScore) {
    this.selfScore = selfScore;
  }

  public double evaluate(AISolution other) {
    if (other == null || other.getSolutionContent() == null || this.solutionContent == null)
      return 0.0;
    return other.getSolutionContent().equals(this.solutionContent) ? 1.0 : 0.5;
  }

  public static class AISolutionBuilder {
    private String providerId;
    private String solutionContent;
    private String generatedCode;
    private double selfScore;

    public AISolutionBuilder providerId(String providerId) {
      this.providerId = providerId;
      return this;
    }

    public AISolutionBuilder solutionContent(String solutionContent) {
      this.solutionContent = solutionContent;
      return this;
    }

    public AISolutionBuilder generatedCode(String generatedCode) {
      this.generatedCode = generatedCode;
      return this;
    }

    public AISolutionBuilder selfScore(double selfScore) {
      this.selfScore = selfScore;
      return this;
    }

    public AISolution build() {
      return new AISolution(providerId, solutionContent, generatedCode, selfScore);
    }
  }
}
