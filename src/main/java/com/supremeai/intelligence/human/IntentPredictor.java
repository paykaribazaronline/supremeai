package com.supremeai.intelligence.human;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Predicts what the developer is trying to build before they even finish typing.
 * Like an ultra-advanced GitHub Copilot.
 */
@Service
public class IntentPredictor {

    public String predictNextMove(String currentCode, String partialPrompt) {
        // In a real system, this uses a lightweight local ML model or heuristic engine
        
        if (currentCode.contains("@RestController") && partialPrompt.toLowerCase().contains("save")) {
            return "Are you trying to create a POST endpoint to save an entity to the database? I can generate the Controller, Service, and Repository layers for you in 1 second.";
        }
        
        if (currentCode.contains("interface") && currentCode.contains("extends JpaRepository")) {
            return "I noticed you created a Repository. Should I generate the matching Service class and basic CRUD operations?";
        }
        
        return null; // Not enough context to predict safely
    }
}