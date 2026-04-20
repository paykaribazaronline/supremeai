package com.supremeai.learning.knowledge;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GlobalKnowledgeBase {

    // Map: Error Signature -> List of Solutions
    private final Map<String, List<SolutionMemory>> globalMemory = new ConcurrentHashMap<>();

    /**
     * Records a successful fix, including its performance and security metrics.
     */
    public void recordSuccess(String errorSignature, String successfulCode, String aiProvider, 
                              long executionTimeMs, double securityScore) {
        List<SolutionMemory> solutions = globalMemory.computeIfAbsent(errorSignature, k -> new ArrayList<>());

        // Check if we already have this exact solution
        for (SolutionMemory solution : solutions) {
            if (solution.getResolvedCode().equals(successfulCode)) {
                solution.incrementSuccess();
                System.out.println("[Knowledge Base] Boosted confidence for existing solution by " + aiProvider);
                return;
            }
        }

        // New solution learned!
        solutions.add(new SolutionMemory(errorSignature, successfulCode, aiProvider, executionTimeMs, securityScore));
        System.out.println("[Knowledge Base] Learned NEW solution for error: '" + errorSignature + "' (Provided by " + aiProvider + ")");
    }
    
    /**
     * If a solution that was given previously turns out to be wrong or causes a secondary bug.
     */
    public void recordFailure(String errorSignature, String failedCode) {
        List<SolutionMemory> solutions = globalMemory.get(errorSignature);
        if (solutions != null) {
            for (SolutionMemory solution : solutions) {
                if (solution.getResolvedCode().equals(failedCode)) {
                    solution.incrementFailure();
                    System.err.println("[Knowledge Base] Penalized a solution that failed in production.");
                    return;
                }
            }
        }
    }

    /**
     * INTELLIGENT RANKING SYSTEM
     * Evaluates multiple solutions for the same problem and picks the absolute BEST one.
     */
    public String findKnownSolution(String errorSignature) {
        List<SolutionMemory> solutions = globalMemory.get(errorSignature);
        
        if (solutions == null || solutions.isEmpty()) {
            return null; // Don't know how to fix this yet
        }

        // The Magic: Sort by our Supreme Score formula (Highest score first)
        solutions.sort((s1, s2) -> Double.compare(s2.calculateSupremeScore(), s1.calculateSupremeScore()));
        
        SolutionMemory bestSolution = solutions.get(0);
        
        // If the best solution has a terrible score (e.g., failed too many times), ignore it!
        if (bestSolution.calculateSupremeScore() < 0.4) {
             System.out.println("[Knowledge Base] Found known solutions, but they are unreliable (Low Score). Falling back to AI API.");
             return null;
        }

        System.out.printf("[Knowledge Base] Picked the BEST solution out of %d options! (Score: %.2f) provided originally by %s\n", 
                          solutions.size(), bestSolution.calculateSupremeScore(), bestSolution.getWorkingAIProvider());
                          
        return bestSolution.getResolvedCode();
    }
}