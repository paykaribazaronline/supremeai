package org.example.generator.model;

public class CodeTemplate {
    private String name;
    private String description;
    private String content;

    // Constructors
    public CodeTemplate(String name, String description, String content) {
        this.name = name;
        this.description = description;
        this.content = content;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}