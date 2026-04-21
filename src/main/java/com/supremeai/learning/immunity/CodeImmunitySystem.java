package com.supremeai.learning.immunity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Project Immunity System
 * Learns what breaks the project and PREVENTS it from happening again.
 */
@Service
public class CodeImmunitySystem {

    private static final Logger log = LoggerFactory.getLogger(CodeImmunitySystem.class);
    // Stores regex patterns of bad code that previously broke the build
    private final Set<Pattern> toxicCodePatterns = new HashSet<>();

    public CodeImmunitySystem() {
        // Initial generic toxic patterns (e.g., hardcoded passwords, obvious infinite loops)
        toxicCodePatterns.add(Pattern.compile("(?i)(password|secret|key)\\s*=\\s*['\"][^'\"]+['\"]"));
        toxicCodePatterns.add(Pattern.compile("while\\s*\\(\\s*true\\s*\\)\\s*\\{\\s*\\}")); 
    }

    /**
     * If an AI generates code that causes a compilation error or crashes the system,
     * we extract the snippet and add it to our immunity list.
     */
    public void learnToxicPattern(String badCodeSnippet) {
        // Simple exact match for learning, but could be converted to regex
        // Escape special regex characters to safely match exact string
        String escapedPattern = Pattern.quote(badCodeSnippet.trim());
        toxicCodePatterns.add(Pattern.compile(escapedPattern));
        log.info("[Immunity System] Developed new antibody against toxic code snippet!");
    }

    /**
     * Before returning AI-generated code to the user, we pass it through the immune system.
     */
    public boolean isCodeInfected(String aiGeneratedCode) {
        for (Pattern toxicPattern : toxicCodePatterns) {
            if (toxicPattern.matcher(aiGeneratedCode).find()) {
                log.error("[Immunity System] THREAT DETECTED! AI generated known toxic code. Blocking...");
                return true;
            }
        }
        return false;
    }
}