package com.supremeai.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class VotingResult {
    private AISolution winner;
    private double confidence;
    private List<String> dissentingOpinions;
    private Map<String, Double> fullBreakdown;
}
