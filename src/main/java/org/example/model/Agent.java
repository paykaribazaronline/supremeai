package org.example.model;

public class Agent {
    private String id;
    private String name;
    private Role role;
    private String model;

    public enum Role {
        BUILDER, REVIEWER, ARCHITECT
    }

    public Agent(String id, String name, Role role, String model) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.model = model;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public Role getRole() { return role; }
    public String getModel() { return model; }
}
