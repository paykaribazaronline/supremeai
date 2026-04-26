package com.supremeai.learning.immunity;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class CodeImmunitySystem {

    private static final Logger log = LoggerFactory.getLogger(CodeImmunitySystem.class);
    private static final String COLLECTION_NAME = "system_configs";
    private static final String DOCUMENT_ID = "code_immunity";

    // In-memory cache for fast access
    private final Set<Pattern> toxicCodePatterns = ConcurrentHashMap.newKeySet();

    @Autowired(required = false)
    private Firestore firestore;

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
        if (firestore == null) {
            log.warn("Firestore not available, using in-memory patterns only");
            return;
        }

        try {
            DocumentSnapshot doc = firestore.collection(COLLECTION_NAME)
                    .document(DOCUMENT_ID)
                    .get()
                    .get(10, TimeUnit.SECONDS);

            if (doc.exists()) {
                @SuppressWarnings("unchecked")
                List<String> patterns = (List<String>) doc.get("patterns");
                if (patterns != null) {
                    for (String patternStr : patterns) {
                        toxicCodePatterns.add(Pattern.compile(patternStr));
                    }
                    log.info("Loaded {} toxic patterns from Firestore", toxicCodePatterns.size());
                }
            } else {
                log.info("No existing immunity patterns found in Firestore, starting fresh");
            }
        } catch (Exception e) {
            log.error("Failed to load patterns from Firestore: {}", e.getMessage(), e);
        }
    }

    /**
     * Save patterns to Firestore.
     */
    private void savePatterns() {
        if (firestore == null) {
            log.warn("Firestore not available, cannot save patterns");
            return;
        }

        try {
            List<String> patterns = new ArrayList<>();
            for (Pattern p : toxicCodePatterns) {
                patterns.add(p.pattern());
            }

            Map<String, Object> data = new HashMap<>();
            data.put("patterns", patterns);
            data.put("updatedAt", new Date());
            data.put("patternCount", patterns.size());

            firestore.collection(COLLECTION_NAME)
                    .document(DOCUMENT_ID)
                    .set(data, SetOptions.merge())
                    .get(5, TimeUnit.SECONDS);

            log.debug("Saved {} patterns to Firestore", patterns.size());
        } catch (Exception e) {
            log.error("Failed to save patterns to Firestore: {}", e.getMessage(), e);
        }
    }

    /**
     * If an AI generates code that causes a compilation error or crashes the system,
     * we extract the snippet and add it to our immunity list.
     */
    public void learnToxicPattern(String badCodeSnippet) {
        // Simple exact match for learning, but could be converted to regex
        String escapedPattern = Pattern.quote(badCodeSnippet.trim());
        toxicCodePatterns.add(Pattern.compile(escapedPattern));
        log.info("[Immunity System] Developed new antibody against toxic code snippet!");

        // Persist to Firestore asynchronously (fire-and-forget)
        new Thread(() -> {
            try {
                savePatterns();
            } catch (Exception e) {
                log.error("Failed to async-save patterns", e);
            }
        }).start();
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