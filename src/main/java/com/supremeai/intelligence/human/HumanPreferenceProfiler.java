package com.supremeai.intelligence.human;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Learns how a specific developer prefers to code and communicate.
 * Now includes Automatic Language Detection & Simplicity Tuning.
 */
@Service
public class HumanPreferenceProfiler {

    private static final Logger log = LoggerFactory.getLogger(HumanPreferenceProfiler.class);
    private final Map<String, DeveloperDNA> globalDeveloperProfiles = new ConcurrentHashMap<>();

    public DeveloperDNA getProfile(String userId) {
        return globalDeveloperProfiles.computeIfAbsent(userId, k -> new DeveloperDNA());
    }

    /**
     * Every time the user interacts with the AI, the system analyzes the prompt
     * to update the user's "DNA", including their native language.
     */
    public void learnFromInteraction(String userId, String prompt, String acceptedCode, boolean askedForExplanation) {
        DeveloperDNA dna = getProfile(userId);
        
        // 1. Detect Local Language
        detectLanguageAndTone(prompt, dna);

        // 2. Learn Communication Style
        if (prompt.contains("explain") || prompt.contains("how") || askedForExplanation) {
            dna.increaseExplanationPreference();
        } else if (prompt.length() < 50) {
            dna.increaseDirectCodePreference();
        }

        // 3. Learn Coding Style (e.g., Indentation)
        if (acceptedCode != null) {
            if (acceptedCode.contains("\n    ")) {
                dna.setIndentStyle(4);
            } else if (acceptedCode.contains("\n  ")) {
                dna.setIndentStyle(2);
            }
            if (acceptedCode.matches(".*\\b[a-z]+[A-Z][a-zA-Z]*\\b.*")) {
                dna.setUsesCamelCase(true);
            }
        }

        log.info("[Human Profiler] Learned new traits for user: {} -> {}", userId, dna.getSummary());
    }

    /**
     * Simple heuristic language detection.
     * In a real system, you would use an NLP library (like Apache Tika or Google CLD3).
     */
    private void detectLanguageAndTone(String prompt, DeveloperDNA dna) {
        String lowerPrompt = prompt.toLowerCase();

        // Detect Bengali
        if (lowerPrompt.matches(".*[\\u0980-\\u09FF]+.*") || 
            lowerPrompt.contains("koro") || lowerPrompt.contains("banaw") || lowerPrompt.contains("bhalo")) {
            dna.setPreferredLanguage("Bengali");
            dna.setPrefersSimpleLanguage(true); // Non-English users usually prefer simpler tech terms
        } 
        // Detect Hindi
        else if (lowerPrompt.matches(".*[\\u0900-\\u097F]+.*") || 
                 lowerPrompt.contains("karo") || lowerPrompt.contains("banao") || lowerPrompt.contains("hai")) {
            dna.setPreferredLanguage("Hindi");
            dna.setPrefersSimpleLanguage(true);
        }
        // Detect Spanish
        else if (lowerPrompt.contains("hola") || lowerPrompt.contains("hacer") || lowerPrompt.contains("por favor")) {
            dna.setPreferredLanguage("Spanish");
        }

        // Detect if user is struggling with tech terms (requests simple explanations)
        if (lowerPrompt.contains("simple") || lowerPrompt.contains("easy") || 
            lowerPrompt.contains("non-technical") || lowerPrompt.contains("don't understand")) {
            dna.setPrefersSimpleLanguage(true);
        }
    }
}