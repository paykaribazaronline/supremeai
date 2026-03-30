package org.example.model;

import java.util.*;

/**
 * Represents an AI model available in the system
 */
public class AIModel {
    private String id;
    private String name;
    private String provider;
    private int rank;
    private List<String> capabilities;
    private double performance;
    private double accuracy;
    private double costPerRequest;
    private boolean available;

    public AIModel() {
        this.id = UUID.randomUUID().toString();
        this.capabilities = new ArrayList<>();
        this.available = true;
    }

    public AIModel(String name, String provider) {
        this();
        this.name = name;
        this.provider = provider;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public List<String> getCapabilities() { return capabilities; }
    public void setCapabilities(List<String> capabilities) { this.capabilities = capabilities; }

    public double getPerformance() { return performance; }
    public void setPerformance(double performance) { this.performance = performance; }

    public double getAccuracy() { return accuracy; }
    public void setAccuracy(double accuracy) { this.accuracy = accuracy; }

    public double getCostPerRequest() { return costPerRequest; }
    public void setCostPerRequest(double costPerRequest) { this.costPerRequest = costPerRequest; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}
