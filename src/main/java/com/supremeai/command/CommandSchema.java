package com.supremeai.command;

import java.util.*;

/**
 * Schema for command parameters
 * Defines what parameters a command accepts and validation rules
 */
public class CommandSchema {
    private final String commandName;
    private final Map<String, ParameterSpec> parameters;
    private final String[] requiredParams;
    
    public CommandSchema(String commandName) {
        this.commandName = commandName;
        this.parameters = new HashMap<>();
        this.requiredParams = new String[0];
    }
    
    /**
     * Add a parameter to the schema
     */
    public CommandSchema addParameter(String name, ParameterSpec spec) {
        parameters.put(name, spec);
        return this;
    }
    
    /**
     * Validate parameters against schema
     */
    public void validate(Map<String, Object> params) {
        // Check required parameters
        for (String required : requiredParams) {
            if (!params.containsKey(required)) {
                throw new CommandValidationException(
                    "Missing required parameter: " + required
                );
            }
        }
        
        // Validate each parameter
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            
            if (!parameters.containsKey(name)) {
                throw new CommandValidationException(
                    "Unknown parameter: " + name
                );
            }
            
            ParameterSpec spec = parameters.get(name);
            spec.validate(value);
        }
    }
    
    // Getters
    public Map<String, ParameterSpec> getParameters() { return parameters; }
    
    /**
     * Parameter specification
     */
    public static class ParameterSpec {
        private final String name;
        private final String description;
        private final Class<?> type;
        private final Object defaultValue;
        private final Object[] allowedValues;
        private final boolean required;
        
        public ParameterSpec(String name, Class<?> type) {
            this.name = name;
            this.type = type;
            this.description = "";
            this.defaultValue = null;
            this.allowedValues = null;
            this.required = false;
        }
        
        public void validate(Object value) {
            if (value == null && required) {
                throw new CommandValidationException(
                    String.format("Parameter %s is required", name)
                );
            }
            
            if (value != null && !type.isInstance(value)) {
                throw new CommandValidationException(
                    String.format("Parameter %s must be of type %s, got %s",
                        name, type.getSimpleName(), value.getClass().getSimpleName())
                );
            }
            
            if (value != null && allowedValues != null) {
                boolean valid = false;
                for (Object allowed : allowedValues) {
                    if (allowed.equals(value)) {
                        valid = true;
                        break;
                    }
                }
                if (!valid) {
                    throw new CommandValidationException(
                        String.format("Parameter %s has invalid value: %s", name, value)
                    );
                }
            }
        }
        
        // Getters
        public String getName() { return name; }
        public String getDescription() { return description; }
        public Class<?> getType() { return type; }
    }
}
