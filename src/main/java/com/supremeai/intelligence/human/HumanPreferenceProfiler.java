package com.supremeai.intelligence.human;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Learns how a specific developer prefers to code and communicate.
 * E.g., Dev A likes short, direct answers. Dev B likes detailed explanations.
 * Dev A uses 2 spaces for indent. Dev B uses 4 spaces.
 */
@Service
public class HumanPreferenceProfiler {

    // Map: UserID -> Developer DNA
    private final Map<String, DeveloperDNA> globalDeveloperProfiles = new ConcurrentHashMap<>();

    public DeveloperDNA getProfile(String userId) {
        return globalDeveloperProfiles.computeIfAbsent(userId, k -> new DeveloperDNA());
    }

    /**
     * Every time the user interacts with the AI, the system analyzes the prompt
     * and the accepted code to update the user's "DNA".
     */
    public void learnFromInteraction(String userId, String prompt, String acceptedCode, boolean askedForExplanation) {
        DeveloperDNA dna = getProfile(userId);
        
        // 1. Learn Communication Style
        if (prompt.contains("explain") || prompt.contains("how") || askedForExplanation) {
            dna.increaseExplanationPreference();
        } else if (prompt.length() < 50) {
            // User gives very short prompts, probably wants direct code without talking
            dna.increaseDirectCodePreference();
        }

        // 2. Learn Coding Style (e.g., Indentation)
        if (acceptedCode != null) {
            if (acceptedCode.contains("\n    ")) {
                dna.setIndentStyle(4);
            } else if (acceptedCode.contains("\n  ")) {
                dna.setIndentStyle(2);
            }
            
            // Learn naming conventions (camelCase vs snake_case)
            if (acceptedCode.matches(".*\\b[a-z]+[A-Z][a-zA-Z]*\\b.*")) {
                dna.setUsesCamelCase(true);
            }
        }
        
        System.out.println("[Human Profiler] Learned new traits for user: " + userId + " -> " + dna.getSummary());
    }
}