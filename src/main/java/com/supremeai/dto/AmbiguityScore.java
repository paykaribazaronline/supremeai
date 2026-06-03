package com.supremeai.dto;

import java.util.List;

public class AmbiguityScore {
    private double confidence;
    private List<String> unclearAreas;

    // Getters
    public double getConfidence() { return confidence; }
    public List<String> getUnclearAreas() { return unclearAreas; }

    // Setters
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public void setUnclearAreas(List<String> unclearAreas) { this.unclearAreas = unclearAreas; }

    // Builder pattern
    public static AmbiguityScore builder() { return new AmbiguityScore(); }

    public AmbiguityScore confidence(double confidence) { this.confidence = confidence; return this; }
    public AmbiguityScore unclearAreas(List<String> unclearAreas) { this.unclearAreas = unclearAreas; return this; }
    public AmbiguityScore build() { return this; }
}
