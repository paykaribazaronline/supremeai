package com.supremeai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class IdeAssistantRequest {
    @NotBlank(message = "Prompt is required")
    private String prompt;
    
    private String context;
    
    private String provider = "gemini";
}
