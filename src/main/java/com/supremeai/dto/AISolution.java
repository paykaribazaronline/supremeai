package com.supremeai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AISolution {
    private String providerId;
    private String solutionContent;
    private String generatedCode;
    private double selfScore;

    public double evaluate(AISolution other) {
        return other.getSolutionContent().equals(this.getSolutionContent()) ? 1.0 : 0.5;
    }
}
