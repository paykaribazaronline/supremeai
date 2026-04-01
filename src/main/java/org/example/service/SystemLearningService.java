package org.example.service;

import org.example.model.SystemLearning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.google.firebase.database.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * System Learning Service - SupremeAI's Brain
 * Learns from errors, tracks patterns, prevents future mistakes
 */
@Service
public class SystemLearningService {
    private static final Logger logger = LoggerFactory.getLogger(SystemLearningService.class);
    
    @Autowired(required = false)
    private FirebaseDatabase firebaseDb;
    
    private static final String LEARNINGS_PATH = "system/learnings";
    private static final String PATTERNS_PATH = "system/patterns";
    private Map<String, SystemLearning> learningsCache = new ConcurrentHashMap<>();
    
    /**
     * Check if Firebase is available
     */
    private boolean isFirebaseAvailable() {
        if (firebaseDb == null) {
            logger.warn("⚠️ Firebase not configured - using in-memory learning cache only");
            return false;
        }
        return true;
    }
    
    /**
     * Record an error for learning
     */
    public void recordError(String category, String errorMessage, Exception e, String solution) {
        try {
            SystemLearning learning = new SystemLearning();
            learning.setType("ERROR");
            learning.setCategory(category);
            learning.setContent(errorMessage);
            learning.setSeverity(analyzeSeverity(errorMessage));
            learning.setSolutions(Arrays.asList(solution));
            learning.setContext(buildErrorContext(e));
            
            // Check if similar error exists
            SystemLearning existing = findSimilarError(category, errorMessage);
            if (existing != null) {
                existing.incrementErrorCount();
                existing.addSolution(solution);
                updateLearning(existing);
                logger.info("📚 Previous {} error found {}: incremented to {}", 
                    category, existing.getId(), existing.getErrorCount());
            } else {
                saveLearning(learning);
                logger.info("📚 New {} error recorded: {}", category, learning.getId());
            }
        } catch (Exception ex) {
            logger.error("❌ Failed to record learning: {}", ex.getMessage());
        }
    }
    
    /**
     * Record a pattern discovered (best practice)
     */
    public void recordPattern(String category, String pattern, String reasoning) {
        try {
            SystemLearning learning = new SystemLearning();
            learning.setType("PATTERN");
            learning.setCategory(category);
            learning.setContent(pattern);
            learning.setSeverity("INFO");
            learning.addSolution(reasoning);
            learning.setConfidenceScore(0.8);
            
            saveLearning(learning);
            logger.info("✨ Pattern recorded {}: {}", category, learning.getId());
        } catch (Exception e) {
            logger.error("❌ Failed to record pattern: {}", e.getMessage());
        }
    }
    
    /**
     * Record admin requirement (CRITICAL)
     */
    public void recordRequirement(String requirement, String details) {
        try {
            SystemLearning learning = new SystemLearning();
            learning.setType("REQUIREMENT");
            learning.setCategory("ADMIN");
            learning.setContent(requirement);
            learning.setSeverity("CRITICAL");
            learning.addSolution(details);
            
            saveLearning(learning);
            logger.info("👑 Requirement recorded: {}", learning.getId());
        } catch (Exception e) {
            logger.error("❌ Failed to record requirement: {}", e.getMessage());
        }
    }
    
    /**
     * Get solutions for a category
     */
    public List<String> getSolutionsFor(String category) {
        if (!isFirebaseAvailable()) {
            return new ArrayList<>();
        }
        try {
            DatabaseReference ref = firebaseDb.getReference(LEARNINGS_PATH);
            AtomicReference<List<String>> solutions = new AtomicReference<>(new ArrayList<>());
            
            ref.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    List<String> sols = new ArrayList<>();
                    snapshot.getChildren().forEach(child -> {
                        SystemLearning learning = child.getValue(SystemLearning.class);
                        if (learning != null && category.equals(learning.getCategory())) {
                            if (learning.getSolutions() != null) {
                                sols.addAll(learning.getSolutions());
                            }
                        }
                    });
                    solutions.set(sols);
                }
                
                @Override
                public void onCancelled(DatabaseError error) {
                    logger.error("❌ Firebase error: {}", error.getMessage());
                }
            });
            
            return solutions.get();
        } catch (Exception e) {
            logger.error("❌ Failed to get solutions: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Get all critical requirements
     */
    public List<SystemLearning> getCriticalRequirements() {
        if (!isFirebaseAvailable()) {
            return new ArrayList<>();
        }
        try {
            DatabaseReference ref = firebaseDb.getReference(LEARNINGS_PATH);
            List<SystemLearning> requirements = new ArrayList<>();
            
            ref.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    snapshot.getChildren().forEach(child -> {
                        SystemLearning learning = child.getValue(SystemLearning.class);
                        if (learning != null && "CRITICAL".equals(learning.getSeverity())) {
                            requirements.add(learning);
                        }
                    });
                }
                
                @Override
                public void onCancelled(DatabaseError error) {
                    logger.error("❌ Firebase error: {}", error.getMessage());
                }
            });
            
            return requirements;
        } catch (Exception e) {
            logger.error("❌ Failed to get requirements: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Check if error should have been prevented
     */
    public Boolean shouldHaveBeenPrevented(String category, String errorMessage) {
        SystemLearning similar = findSimilarError(category, errorMessage);
        
        if (similar == null) {
            return false;
        }
        
        // If we've seen this error 3+ times, we should have prevented it
        return similar.getErrorCount() >= 3;
    }
    
    /**
     * Get learning statistics
     */
    public Map<String, Object> getLearningStats() {
        Map<String, Object> stats = new HashMap<>();
        
        if (!isFirebaseAvailable()) {
            stats.put("status", "firebase-unavailable");
            return stats;
        }
        
        try {
            DatabaseReference ref = firebaseDb.getReference(LEARNINGS_PATH);
            
            ref.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    int totalLearnings = 0;
                    int errorsResolved = 0;
                    int patternsFound = 0;
                    Map<String, Integer> byCategory = new HashMap<>();
                    
                    for (DataSnapshot child : snapshot.getChildren()) {
                        SystemLearning learning = child.getValue(SystemLearning.class);
                        if (learning != null) {
                            totalLearnings++;
                            
                            if ("ERROR".equals(learning.getType()) && learning.getResolved()) {
                                errorsResolved++;
                            }
                            if ("PATTERN".equals(learning.getType())) {
                                patternsFound++;
                            }
                            
                            String cat = learning.getCategory();
                            byCategory.put(cat, byCategory.getOrDefault(cat, 0) + 1);
                        }
                    }
                    
                    stats.put("totalLearnings", totalLearnings);
                    stats.put("errorsResolved", errorsResolved);
                    stats.put("patternsFound", patternsFound);
                    stats.put("byCategory", byCategory);
                    stats.put("timestamp", System.currentTimeMillis());
                }
                
                @Override
                public void onCancelled(DatabaseError error) {
                    logger.error("❌ Firebase error: {}", error.getMessage());
                }
            });
        } catch (Exception e) {
            logger.error("❌ Failed to get stats: {}", e.getMessage());
        }
        
        return stats;
    }
    
    // ========== PRIVATE HELPERS ==========
    
    private void saveLearning(SystemLearning learning) throws Exception {
        if (!isFirebaseAvailable()) {
            logger.debug("⚠️ Firebase unavailable, storing in memory cache only");
            learningsCache.put(learning.getId(), learning);
            return;
        }
        DatabaseReference ref = firebaseDb.getReference(LEARNINGS_PATH).push();
        ref.setValueAsync(learning);
        learningsCache.put(learning.getId(), learning);
    }
    
    private void updateLearning(SystemLearning learning) throws Exception {
        if (!isFirebaseAvailable()) {
            logger.debug("⚠️ Firebase unavailable, updating memory cache only");
            learningsCache.put(learning.getId(), learning);
            return;
        }
        DatabaseReference ref = firebaseDb.getReference(LEARNINGS_PATH + "/" + learning.getId());
        ref.setValueAsync(learning);
        learningsCache.put(learning.getId(), learning);
    }
    
    private SystemLearning findSimilarError(String category, String errorMessage) {
        return learningsCache.values().stream()
            .filter(l -> category.equals(l.getCategory()) 
                    && errorMessage.contains(l.getContent()))
            .findFirst()
            .orElse(null);
    }
    
    private String analyzeSeverity(String errorMessage) {
        if (errorMessage.toLowerCase().contains("security") || 
            errorMessage.toLowerCase().contains("injection")) {
            return "CRITICAL";
        }
        if (errorMessage.toLowerCase().contains("null") || 
            errorMessage.toLowerCase().contains("compilation")) {
            return "HIGH";
        }
        return "MEDIUM";
    }
    
    private Map<String, Object> buildErrorContext(Exception e) {
        Map<String, Object> context = new HashMap<>();
        context.put("exceptionClass", e.getClass().getName());
        context.put("message", e.getMessage());
        context.put("timestamp", System.currentTimeMillis());
        
        if (e.getStackTrace().length > 0) {
            StackTraceElement first = e.getStackTrace()[0];
            context.put("location", first.getClassName() + ":" + first.getLineNumber());
        }
        
        return context;
    }
}
