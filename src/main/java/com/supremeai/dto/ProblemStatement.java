package com.supremeai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProblemStatement {
    @NotBlank(message = "Description is required")
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @Size(max = 2000, message = "Context must not exceed 2000 characters")
    private String context;

    private String requiredOutputType;

    public ProblemStatement() {
    }

    public ProblemStatement(String description, String context, String requiredOutputType) {
        this.description = description;
        this.context = context;
        this.requiredOutputType = requiredOutputType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getRequiredOutputType() {
        return requiredOutputType;
    }

    public void setRequiredOutputType(String requiredOutputType) {
        this.requiredOutputType = requiredOutputType;
    }

    public static ProblemStatementBuilder builder() {
        return new ProblemStatementBuilder();
    }

    public static class ProblemStatementBuilder {
        private String description;
        private String context;
        private String requiredOutputType;

        ProblemStatementBuilder() {
        }

        public ProblemStatementBuilder description(String description) {
            this.description = description;
            return this;
        }

        public ProblemStatementBuilder context(String context) {
            this.context = context;
            return this;
        }

        public ProblemStatementBuilder requiredOutputType(String requiredOutputType) {
            this.requiredOutputType = requiredOutputType;
            return this;
        }

        public ProblemStatement build() {
            return new ProblemStatement(description, context, requiredOutputType);
        }
    }
}
