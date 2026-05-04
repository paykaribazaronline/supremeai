package com.supremeai.model;

import java.util.List;

/**
 * Definition of an entity for code generation.
 * Used to describe custom entities beyond the default Product entity.
 */
public class EntityDefinition {
    private String name;
    private String description;
    private List<FieldDefinition> fields;
    private boolean generateRepository = true;
    private boolean generateService = true;
    private boolean generateController = true;
    private boolean generateTests = true;
    
    public EntityDefinition() {}
    
    public EntityDefinition(String name, String description, List<FieldDefinition> fields) {
        this.name = name;
        this.description = description;
        this.fields = fields;
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<FieldDefinition> getFields() {
        return fields;
    }
    
    public void setFields(List<FieldDefinition> fields) {
        this.fields = fields;
    }
    
    public boolean isGenerateRepository() {
        return generateRepository;
    }
    
    public void setGenerateRepository(boolean generateRepository) {
        this.generateRepository = generateRepository;
    }
    
    public boolean isGenerateService() {
        return generateService;
    }
    
    public void setGenerateService(boolean generateService) {
        this.generateService = generateService;
    }
    
    public boolean isGenerateController() {
        return generateController;
    }
    
    public void setGenerateController(boolean generateController) {
        this.generateController = generateController;
    }
    
    public boolean isGenerateTests() {
        return generateTests;
    }
    
    public void setGenerateTests(boolean generateTests) {
        this.generateTests = generateTests;
    }
}
