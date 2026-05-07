package com.supremeai.dto;

import com.supremeai.model.EntityDefinition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AppGenerationRequest {
    
    @NotBlank(message = "App name is required")
    @Size(min = 2, max = 50, message = "App name must be between 2 and 50 characters")
    private String name;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    private String platform = "fullstack";
    
    private String database = "PostgreSQL";
    
    private String type = "project";
    
    private boolean useAI = false;
    
    private List<EntityDefinition> entities;
    
    private Map<String, Object> additionalContext;
}
