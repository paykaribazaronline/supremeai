package com.supremeai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {
    @NotBlank(message = "Message is required")
    private String message;
    
    private boolean skipValidation = false;
    
    private String agentId;
}
