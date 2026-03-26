package org.example.service;

import org.example.model.Requirement;

import java.util.List;
import java.util.Arrays;

public class RequirementClassifier {
    private static final List<String> BIG_KEYWORDS = Arrays.asList("payment", "chatbot", "database", "stripe", "auth");
    private static final List<String> MEDIUM_KEYWORDS = Arrays.asList("screen", "validation", "form", "ui");

    public Requirement.Size classify(String description) {
        String desc = description.toLowerCase();
        
        if (BIG_KEYWORDS.stream().anyMatch(desc::contains)) {
            return Requirement.Size.BIG;
        } else if (MEDIUM_KEYWORDS.stream().anyMatch(desc::contains)) {
            return Requirement.Size.MEDIUM;
        } else {
            return Requirement.Size.SMALL;
        }
    }
}
