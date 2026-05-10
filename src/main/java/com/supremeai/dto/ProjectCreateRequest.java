package com.supremeai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProjectCreateRequest {
    @NotBlank(message = "Project name is required")
    private String name;
    
    @Size(max = 2000, message = "Description must be 2000 characters or less")
    private String description;
    
    private String type = "standard";
    
    private String platform;
}