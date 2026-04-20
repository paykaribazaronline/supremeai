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
     * When any AI (Groq, Gemini, etc.) successfully fixes an error, this method is called.
     * It stores the error and the exact code that fixed it.
     */
    public void recordSuccess(String errorSignature, String successfulCode, String aiProvider) {
        List<SolutionMemory> solutions = globalMemory.computeIfAbsent(errorSignature, k -> new ArrayList<>());

        // Check if we already have this exact solution. If yes, just boost its confidence.
        for (SolutionMemory solution : solutions) {
            if (solution.getResolvedCode().equals(successfulCode)) {
                solution.incrementSuccess();
                System.out.println("[Knowledge Base] Boosted confidence for existing solution by " + aiProvider);
                return;
            }
        }

        // New solution learned!
        solutions.add(new SolutionMemory(errorSignature, successfulCode, aiProvider));
        System.out.println("[Knowledge Base] Learned NEW solution for error: '" + errorSignature + "' (Provided by " + aiProvider + ")");
        
        // In a real system, you would async flush this to Firebase/Firestore here
    }

    /**
     * Before asking an expensive AI, the system checks if it already knows the answer.
     */
    public String findKnownSolution(String errorSignature) {
        List<SolutionMemory> solutions = globalMemory.get(errorSignature);
        
        if (solutions == null || solutions.isEmpty()) {
            return null; // Don't know how to fix this yet
        }

        // Return the solution that has the highest success count (Most reliable)
        solutions.sort((s1, s2) -> Integer.compare(s2.getSuccessCount(), s1.getSuccessCount()));
        
        System.out.println("[Knowledge Base] Found known solution! Saved an API call.");
        return solutions.get(0).getResolvedCode();
    }
}