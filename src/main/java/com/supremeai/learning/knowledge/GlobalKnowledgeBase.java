qstn package com.supremeai.learning.knowledge;

import com.supremeai.admin.AdminDashboardService;
import com.supremeai.admin.ImprovementProposal;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GlobalKnowledgeBase {

    private final Map<String, List<SolutionMemory>> globalMemory = new ConcurrentHashMap<>();
    private final AdminDashboardService adminDashboard;

    public GlobalKnowledgeBase(AdminDashboardService adminDashboard) {
        this.adminDashboard = adminDashboard;
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
                System.out.println("[Knowledge Base] Boosted confidence for existing solution by " + aiProvider);
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
            System.out.println("[Knowledge Base] Learned NEW solution automatically!");
        } else {
            System.out.println("[Knowledge Base] Solution pending. Waiting for Admin to approve in the Dashboard.");
            // In a real app, when Admin clicks 'Approve', an event would trigger the addition.
        }
    }
    
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

    public String findKnownSolution(String errorSignature) {
        List<SolutionMemory> solutions = globalMemory.get(errorSignature);
        if (solutions == null || solutions.isEmpty()) return null; 

        solutions.sort((s1, s2) -> Double.compare(s2.calculateSupremeScore(), s1.calculateSupremeScore()));
        SolutionMemory bestSolution = solutions.get(0);
        
        if (bestSolution.calculateSupremeScore() < 0.4) return null;

        return bestSolution.getResolvedCode();
    }
}