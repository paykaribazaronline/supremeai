package com.supremeai.learning.immunity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.google.cloud.spring.data.firestore.FirestoreTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.springframework.boot.context.event.ApplicationReadyEvent;

/**
 * Project Immunity System
 * Learns what breaks the project and PREVENTS it from happening again.
 */
@Service
public class CodeImmunitySystem {

    private static final Logger log = LoggerFactory.getLogger(CodeImmunitySystem.class);
    private static final String COLLECTION_NAME = "system_configs";
    private static final String DOCUMENT_ID = "code_immunity";

    // In-memory cache for fast access
    private final Set<Pattern> toxicCodePatterns = ConcurrentHashMap.newKeySet();

    @Autowired(required = false)
    private FirestoreTemplate firestoreTemplate;

    public CodeImmunitySystem() {
        // Initial generic toxic patterns (e.g., hardcoded passwords, obvious infinite loops)
        learnToxicPattern("password\\s*=\\s*['\\\"][^'\\\"]+['\\\"]");
        learnToxicPattern("while\\s*\\(\\s*true\\s*\\)\\s*\\{\\s*\\}");
    }

    /**
     * Load patterns from Firestore on startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void loadPatterns() {
        if (firestoreTemplate == null) {
            log.warn("Firestore not available, using in-memory patterns only");
            return;
        }

        try {
            // TODO: Update for new FirestoreTemplate API in Spring Cloud GCP 5.x
            log.warn("Firestore loading disabled - please update for new API");
            return;
            
            /*
            Map<String, Object> doc = firestoreTemplate.findById(DOCUMENT_ID, COLLECTION_NAME, Map.class).block();
            if (doc != null && doc.containsKey("patterns")) {
                @SuppressWarnings("unchecked")
                List<String> patterns = (List<String>) doc.get("patterns");
                for (String patternStr : patterns) {
                    toxicCodePatterns.add(Pattern.compile(patternStr));
                }
                log.info("Loaded {} toxic patterns from Firestore", toxicCodePatterns.size());
            }
            */
        } catch (Exception e) {
            log.error("Failed to load patterns from Firestore: {}", e.getMessage());
        }
    }

    /**
     * Save patterns to Firestore.
     */
    private void savePatterns() {
        if (firestoreTemplate == null) return;

        // TODO: Update for new FirestoreTemplate API in Spring Cloud GCP 5.x
        log.warn("Firestore save disabled - please update for new API");
        return;
        
        /*
        List<String> patterns = new ArrayList<>();
        for (Pattern p : toxicCodePatterns) {
            patterns.add(p.pattern());
        }

        Map<String, Object> doc = new HashMap<>();
        doc.put("patterns", patterns);
        doc.put("updatedAt", new Date());

        firestoreTemplate.save(doc, COLLECTION_NAME, DOCUMENT_ID).subscribe(
                result -> log.debug("Saved {} patterns to Firestore", patterns.size()),
                error -> log.error("Failed to save patterns: {}", error.getMessage())
        );
        */
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

        // Persist to Firestore asynchronously
        savePatterns();
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