package com.supremeai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProblemStatement {
    private String description;
    private String context;
    private String requiredOutputType;
}
