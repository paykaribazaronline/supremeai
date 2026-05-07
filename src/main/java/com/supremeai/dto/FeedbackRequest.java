package com.supremeai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FeedbackRequest {
    @NotBlank(message = "Message ID is required")
    private String messageId;
    
    private boolean helpful;
    
    private String userMessage;
    
    private String aiResponse;
}
