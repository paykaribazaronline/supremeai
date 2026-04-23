package com.supremeai.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class AmbiguityScore {
    private double confidence;
    private List<String> unclearAreas;
}
