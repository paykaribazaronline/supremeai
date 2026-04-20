package com.supremeai.intelligence.vision;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Predicts and flags potential "Ripple Effects" (Domino Effects) of a code change.
 */
@Service
public class RippleEffectPredictor {

    /**
     * When AI suggests changing a core file (e.g., Database Entity), 
     * this predictor looks around the project to warn the user about what else might break.
     */
    public RippleWarning analyzeCodeChange(String filePath, String oldCode, String newCode) {
        
        RippleWarning warning = new RippleWarning(filePath);

        // Example 1: Developer changes a database column in a Spring Boot Entity
        if (filePath.endsWith("Entity.java") || filePath.endsWith("Model.java")) {
            if (newCode.contains("@Column") && !oldCode.equals(newCode)) {
                warning.addAffectedArea("Database Schema", "You might need to write a Flyway/Liquibase migration for this column change.");
                warning.addAffectedArea("DTOs", "Did you update the corresponding ResponseDTO and RequestDTO?");
                warning.addAffectedArea("Frontend Models", "Warning: The React/Angular frontend might crash if it expects the old JSON structure.");
            }
        }

        // Example 2: Developer changes an Interface signature
        if (filePath.endsWith("Service.java") && oldCode.contains("interface") && !oldCode.equals(newCode)) {
            warning.addAffectedArea("Implementations", "All classes implementing this service will break.");
            warning.addAffectedArea("Unit Tests", "Mock objects in your test files will fail compilation.");
        }

        return warning.hasWarnings() ? warning : null;
    }
}

class RippleWarning {
    private String changedFile;
    private List<String> warnings = new ArrayList<>();

    public RippleWarning(String changedFile) {
        this.changedFile = changedFile;
    }

    public void addAffectedArea(String area, String message) {
        warnings.add("⚠️ [" + area + "] " + message);
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public String generateAlertMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("🚨 Supreme Vision Alert: Your change in ").append(changedFile).append(" has a Ripple Effect!\n");
        sb.append("Before you commit, please check these files that might break:\n");
        for (String w : warnings) {
            sb.append(w).append("\n");
        }
        return sb.toString();
    }
}