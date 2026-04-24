package com.supremeai.learning.knowledge;

import com.supremeai.admin.AdminDashboardService;
import com.supremeai.admin.ImprovementProposal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.google.cloud.spring.data.firestore.FirestoreTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GlobalKnowledgeBase {

    private static final Logger log = LoggerFactory.getLogger(GlobalKnowledgeBase.class);
    private static final String COLLECTION_NAME = "solution_memories";
    private final Map<String, List<SolutionMemory>> globalMemory = new ConcurrentHashMap<>();

    @Autowired
    private AdminDashboardService adminDashboard;

    @Autowired(required = false)
    private FirestoreTemplate firestoreTemplate;

    /**
     * Load memories from Firestore on startup.
     */
    // @PostConstruct - Disabled due to API changes in Spring Cloud GCP 5.x
    public void loadMemories() {
        if (firestoreTemplate == null) {
            log.warn("Firestore not available, using in-memory only");
            return;
        }

        try {
            // TODO: Update for new FirestoreTemplate API in Spring Cloud GCP 5.x
            // List<Map> documents = firestoreTemplate.findAll(COLLECTION_NAME, Map.class).collectList().block();
            log.info("Firestore loading disabled - please update for new API");
            return;
            
            /*
            if (documents == null) return;

            for (Map doc : documents) {
                String errorSignature = (String) doc.get("errorSignature");
                if (errorSignature == null) continue;

                List<Map> solutionsData = (List<Map>) doc.get("solutions");
                if (solutionsData == null) continue;

                List<SolutionMemory> solutions = new ArrayList<>();
                for (Map data : solutionsData) {
                    SolutionMemory memory = new SolutionMemory(
                            errorSignature,
                            (String) data.get("resolvedCode"),
                            (String) data.get("provider"),
                            ((Number) data.get("executionTimeMs")).longValue(),
                            ((Number) data.get("securityScore")).doubleValue()
                    );
                    memory.setSuccessCount(((Number) data.getOrDefault("successCount", 0)).intValue());
                    memory.setFailureCount(((Number) data.getOrDefault("failureCount", 0)).intValue());
                    solutions.add(memory);
                }

                globalMemory.put(errorSignature, solutions);
            }
            log.info("Loaded {} error signatures from Firestore", globalMemory.size());
            */
        } catch (Exception e) {
            log.error("Failed to load memories from Firestore: {}", e.getMessage());
        }
    }

    /**
     * Save memories to Firestore.
     */
    private void saveMemories(String errorSignature) {
        if (firestoreTemplate == null) return;

        // TODO: Update for new FirestoreTemplate API in Spring Cloud GCP 5.x
        log.warn("Firestore save disabled - please update for new API");
        return;
    }

    /**
     * Records a successful fix, BUT checks with Admin Dashboard first!
     */
    public void recordSuccessWithPermission(String errorSignature, String successfulCode, String aiProvider, 
                              long executionTimeMs, double securityScore) {
        
        // 1. Check if we already have this exact solution (Just boosting confidence doesn't need admin permission)
        List<SolutionMemory> solutions = globalMemory.computeIfAbsent(errorSignature, k -> new ArrayList<>());
        for (SolutionMemory solution : solutions) {
            if (solution.getResolvedCode().equals(successfulCode)) {
                solution.incrementSuccess();
                log.info("[Knowledge Base] Boosted confidence for existing solution by {}", aiProvider);
                return;
            }
        }

        // 2. It's a brand NEW solution. We must ask permission or check Auto-Pilot!
        ImprovementProposal proposal = new ImprovementProposal(
            "Learn new fix for " + errorSignature,
            "AI Provider " + aiProvider + " successfully fixed this error. Should I add it to Global Memory?",
            "KNOWLEDGE_BASE",
            successfulCode
        );

        boolean isApprovedImmediately = adminDashboard.submitImprovement(proposal);

        if (isApprovedImmediately) {
            // Auto-Pilot is ON (or admin approved synchronously). Save it!
            solutions.add(new SolutionMemory(errorSignature, successfulCode, aiProvider, executionTimeMs, securityScore));
            log.info("[Knowledge Base] Learned NEW solution automatically!");
            saveMemories(errorSignature); // Persist to Firestore
        } else {
            log.info("[Knowledge Base] Solution pending. Waiting for Admin to approve in the Dashboard.");
            // In a real app, when Admin clicks 'Approve', an event would trigger the addition.
        }
    }
    
    public void recordFailure(String errorSignature, String failedCode) {
        List<SolutionMemory> solutions = globalMemory.get(errorSignature);
        if (solutions != null) {
            for (SolutionMemory solution : solutions) {
                if (solution.getResolvedCode().equals(failedCode)) {
                    solution.incrementFailure();
                    log.error("[Knowledge Base] Penalized a solution that failed in production.");
                    saveMemories(errorSignature); // Persist to Firestore
                    return;
                }
            }
        }
    }

    public String findKnownSolution(String errorSignature) {
        List<SolutionMemory> solutions = globalMemory.get(errorSignature);
        if (solutions == null || solutions.isEmpty()) return null; 

        solutions.sort((s1, s2) -> Double.compare(s2.calculateSupremeScore(), s1.calculateSupremeScore()));
        SolutionMemory bestSolution = solutions.get(0);
        
        if (bestSolution.calculateSupremeScore() < 0.4) return null;

        return bestSolution.getResolvedCode();
    }
}