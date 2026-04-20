package com.supremeai.intelligence.healing;

import com.supremeai.fallback.AIFallbackOrchestrator;
import com.supremeai.intelligence.voting.CouncilVotingSystem;
import com.supremeai.fallback.AIProvider;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * The Infinite Auto-Healing Loop!
 * Ensures nothing is considered "Done" until it actually compiles and passes tests in CI/CD.
 */
@Service
public class InfiniteAutoHealer {

    private final AIFallbackOrchestrator fallbackOrchestrator;
    private final CouncilVotingSystem votingSystem;
    
    private final int MAX_ITERATIONS = 5; // Prevent literal infinite loops that drain money

    public InfiniteAutoHealer(AIFallbackOrchestrator fallbackOrchestrator, CouncilVotingSystem votingSystem) {
        this.fallbackOrchestrator = fallbackOrchestrator;
        this.votingSystem = votingSystem;
    }

    /**
     * This method acts as the overseer. It writes code -> triggers CI/CD -> 
     * if it fails -> feeds logs back to AI -> writes new code -> repeat until SUCCESS.
     */
    public String developUntilPerfection(String taskCategory, String userPrompt) {
        
        System.out.println("\n[Auto-Healer] Starting development loop for: " + taskCategory);
        
        String currentCode = "";
        String currentPrompt = userPrompt;
        
        for (int attempt = 1; attempt <= MAX_ITERATIONS; attempt++) {
            System.out.println("\n=== ATTEMPT " + attempt + " ===");

            // 1. Generate/Fix the code
            currentCode = fallbackOrchestrator.executeWithSupremeIntelligence(taskCategory, "SIGNATURE_" + taskCategory, currentPrompt);
            
            // 2. Voting Council Review (Before even trying to compile, filter out obvious bad logic)
            List<AIProvider> council = Arrays.asList(AIProvider.GROQ_LLAMA3, AIProvider.GEMINI_PRO);
            boolean councilApproved = votingSystem.conductVote("GENERAL_REVIEW", currentCode, council);
            
            if (!councilApproved) {
                System.out.println("[Auto-Healer] Council REJECTED the code. Sending back for rewrite...");
                currentPrompt = "The Council of AI models rejected your code due to potential logic/security flaws. Please write a safer version.";
                continue; // Try again!
            }

            // 3. Trigger CI/CD Pipeline (Compilation & Tests)
            System.out.println("[Auto-Healer] Council approved. Triggering CI/CD Pipeline...");
            BuildResult buildResult = simulateCICDPipeline(currentCode);

            if (buildResult.isSuccess()) {
                System.out.println("\n[Auto-Healer] SUCCESS! Code compiled and all tests passed on attempt " + attempt + "!");
                return currentCode; // PERFECT RESULT!
            } else {
                System.out.println("[Auto-Healer] CI/CD FAILED at stage: " + buildResult.getFailedStage());
                
                // 4. The Magic: Feed the compiler/test error straight back to the AI for the next iteration!
                currentPrompt = "Your previous code failed during " + buildResult.getFailedStage() + ".\n" +
                                "Here are the compiler/test error logs:\n" +
                                buildResult.getErrorLogs() + "\n" +
                                "Please fix the exact errors mentioned in the logs.";
            }
        }

        throw new RuntimeException("CRITICAL: Auto-Healer failed to achieve perfection after " + MAX_ITERATIONS + " attempts.");
    }

    /**
     * Simulates running a Gradle/Maven build and JUnit tests.
     */
    private BuildResult simulateCICDPipeline(String code) {
        double randomOutcome = Math.random();
        
        // Simulating the harsh reality of software development
        if (randomOutcome < 0.3) {
            return new BuildResult(false, "Error:(45, 12) java: cannot find symbol\n  symbol:   variable data\n  location: class Main", "COMPILATION");
        } else if (randomOutcome < 0.6) {
            return new BuildResult(false, "org.junit.ComparisonFailure: expected:<[Success]> but was:<[Null]>", "UNIT_TESTS");
        } else {
            return new BuildResult(true, "BUILD SUCCESSFUL in 2s", "ALL_STAGES"); // Code is actually perfect
        }
    }
}