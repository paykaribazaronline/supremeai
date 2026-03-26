package org.example.model;

public class Requirement {
    public enum Size { SMALL, MEDIUM, BIG, HUMAN_REQUIRED }
    public enum Status { PENDING, APPROVED, REJECTED, WAITING_FOR_HUMAN }

    private String id;
    private String description;
    private Size size;
    private Status status;
    private String humanInstructions;

    public Requirement(String id, String description, Size size) {
        this.id = id;
        this.description = description;
        this.size = size;
        this.status = Status.PENDING;
    }

    public String getId() { return id; }
    public String getDescription() { return description; }
    public Size getSize() { return size; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    
    public String getHumanInstructions() { return humanInstructions; }
    public void setHumanInstructions(String humanInstructions) { this.humanInstructions = humanInstructions; }
}
