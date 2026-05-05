package com.supremeai.model;

/**
 * Definition of a field within an entity.
 */
public class FieldDefinition {
    private String name;
    private String type;
    private boolean required;
    private boolean unique;
    private int maxLength;
    private String defaultValue;
    private String description;
    
    // Common field types
    public static final String TYPE_STRING = "string";
    public static final String TYPE_INTEGER = "integer";
    public static final String TYPE_LONG = "long";
    public static final String TYPE_DOUBLE = "double";
    public static final String TYPE_FLOAT = "float";
    public static final String TYPE_BOOLEAN = "boolean";
    public static final String TYPE_DATE = "date";
    public static final String TYPE_DATETIME = "datetime";
    public static final String TYPE_EMAIL = "email";
    public static final String TYPE_TEXT = "text";
    
    public FieldDefinition() {}
    
    public FieldDefinition(String name, String type, boolean required) {
        this.name = name;
        this.type = type;
        this.required = required;
    }
    
    public FieldDefinition(String name, String type, boolean required, boolean unique) {
        this.name = name;
        this.type = type;
        this.required = required;
        this.unique = unique;
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public boolean isRequired() {
        return required;
    }
    
    public void setRequired(boolean required) {
        this.required = required;
    }
    
    public boolean isUnique() {
        return unique;
    }
    
    public void setUnique(boolean unique) {
        this.unique = unique;
    }
    
    public int getMaxLength() {
        return maxLength;
    }
    
    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }
    
    public String getDefaultValue() {
        return defaultValue;
    }
    
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
}
