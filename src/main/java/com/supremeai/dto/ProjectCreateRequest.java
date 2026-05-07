package com.supremeai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProjectCreateRequest {
    @NotBlank(message = "Project name is required")
    private String name;
    
    private String description;
    
    private String type = "standard";
    
    private String platform;
}