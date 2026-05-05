package com.supremeai.codeflow.analyzer;

import com.supremeai.codeflow.model.CodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Health scoring system for code repositories
 * Calculates 0-100 score with A-F grading
 */
@Component
public class HealthScorer {
    
    private static final Logger logger = LoggerFactory.getLogger(HealthScorer.class);
    
    /**
     * Calculate overall health score
     */
    public CodeFlowService.HealthScoreResult calculateScore(CodeRepository repo) {
        int score = 100;
        
        // Deduct for security issues
        score -= calculateSecurityDeduction(repo);
        
        // Deduct for code quality issues
        score -= calculateQualityDeduction(repo);
        
        // Deduct for architectural issues
        score -= calculateArchitectureDeduction(repo);
        
        // Deduct for dead code
        score -= calculateDeadCodeDeduction(repo);
        
        // Deduct for circular dependencies
        score -= calculateCircularDependencyDeduction(repo);
        
        // Ensure score is within bounds
        score = Math.max(0, Math.min(100, score));
        
        // Determine grade
        String grade = calculateGrade(score);
        
        logger.info("Health score calculated: {} ({})", score, grade);
        
        return CodeFlowService.HealthScoreResult.builder()
            .score(score)
            .grade(grade)
            .build();
    }
    
    /**
     * Calculate security deduction
     */
    private int calculateSecurityDeduction(CodeRepository repo) {
        if (repo.getSecurityIssues() == null || repo.getSecurityIssues().isEmpty()) {
            return 0;
        }
        
        int deduction = 0;
        for (CodeRepository.SecurityIssue issue : repo.getSecurityIssues()) {
            switch (issue.getSeverity()) {
                case "CRITICAL":
                    deduction += 15;
                    break;
                case "HIGH":
                    deduction += 8;
                    break;
                case "MEDIUM":
                    deduction += 4;
                    break;
                case "LOW":
                    deduction += 1;
                    break;
            }
        }
        
        return Math.min(deduction, 40); // Cap at 40 points
    }
    
    /**
     * Calculate code quality deduction
     */
    private int calculateQualityDeduction(CodeRepository repo) {
        int deduction = 0;
        
        // Check average complexity
        if (repo.getFiles() != null && !repo.getFiles().isEmpty()) {
            double avgComplexity = repo.getFiles().stream()
                .mapToInt(CodeRepository.CodeFile::getComplexity)
                .average()
                .orElse(0);
            
            if (avgComplexity > 20) {
                deduction += (int) ((avgComplexity - 20) * 0.5);
            }
            
            // Check for high cyclomatic complexity
            long highComplexityFunctions = repo.getFiles().stream()
                .filter(f -> f.getFunctions() != null)
                .flatMap(f -> f.getFunctions().stream())
                .filter(f -> f.getCyclomaticComplexity() > 10)
                .count();
            
            deduction += highComplexityFunctions * 2;
        }
        
        return Math.min(deduction, 20);
    }
    
    /**
     * Calculate architectural deduction
     */
    private int calculateArchitectureDeduction(CodeRepository repo) {
        int deduction = 0;
        
        // Check for circular dependencies
        if (repo.getCircularDependencies() != null && !repo.getCircularDependencies().isEmpty()) {
            deduction += repo.getCircularDependencies().size() * 5;
        }
        
        // Check dependency graph blast radius
        if (repo.getDependencyGraph() != null && repo.getDependencyGraph().getBlastRadius() > 10) {
            int blastRadius = repo.getDependencyGraph().getBlastRadius();
            deduction += Math.min(blastRadius - 10, 15);
        }
        
        // Check for God objects
        if (repo.getDetectedPatterns() != null) {
            long godObjects = repo.getDetectedPatterns().stream()
                .filter(p -> "GOD_OBJECT".equals(p.getPatternType()))
                .count();
            deduction += godObjects * 8;
        }
        
        return Math.min(deduction, 25);
    }
    
    /**
     * Calculate dead code deduction
     */
    private int calculateDeadCodeDeduction(CodeRepository repo) {
        if (repo.getDeadCode() == null || repo.getDeadCode().isEmpty()) {
            return 0;
        }
        
        int deduction = 0;
        for (CodeRepository.DeadCode dead : repo.getDeadCode()) {
            switch (dead.getType()) {
                case "UNUSED_FUNCTION":
                    deduction += 2;
                    break;
                case "UNUSED_IMPORT":
                    deduction += 1;
                    break;
                case "UNREACHABLE_CODE":
                    deduction += 3;
                    break;
            }
        }
        
        return Math.min(deduction, 15);
    }
    
    /**
     * Calculate circular dependency deduction
     */
    private int calculateCircularDependencyDeduction(CodeRepository repo) {
        if (repo.getCircularDependencies() == null || repo.getCircularDependencies().isEmpty()) {
            return 0;
        }
        
        int deduction = 0;
        for (CodeRepository.CircularDependency cd : repo.getCircularDependencies()) {
            deduction += cd.getSeverity();
        }
        
        return Math.min(deduction, 20);
    }
    
    /**
     * Calculate grade from score
     */
    private String calculateGrade(int score) {
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }
    
    /**
     * Get score interpretation
     */
    public String getScoreInterpretation(int score) {
        if (score >= 90) return "Excellent - High quality code with minimal issues";
        if (score >= 80) return "Good - Well-structured code with minor improvements needed";
        if (score >= 70) return "Fair - Acceptable quality but needs attention";
        if (score >= 60) return "Poor - Significant issues requiring refactoring";
        return "Critical - Urgent attention required";
    }
    
    /**
     * Get improvement suggestions based on score
     */
    public List<String> getImprovementSuggestions(CodeRepository repo) {
        List<String> suggestions = new ArrayList<>();
        
        if (repo.getSecurityIssues() != null && !repo.getSecurityIssues().isEmpty()) {
            long criticalIssues = repo.getSecurityIssues().stream()
                .filter(i -> "CRITICAL".equals(i.getSeverity()) || "HIGH".equals(i.getSeverity()))
                .count();
            if (criticalIssues > 0) {
                suggestions.add("Address " + criticalIssues + " critical/high security issues immediately");
            }
        }
        
        if (repo.getCircularDependencies() != null && !repo.getCircularDependencies().isEmpty()) {
            suggestions.add("Resolve " + repo.getCircularDependencies().size() + " circular dependencies");
        }
        
        if (repo.getDeadCode() != null && repo.getDeadCode().size() > 5) {
            suggestions.add("Remove " + repo.getDeadCode().size() + " instances of dead code");
        }
        
        if (repo.getDetectedPatterns() != null) {
            long godObjects = repo.getDetectedPatterns().stream()
                .filter(p -> "GOD_OBJECT".equals(p.getPatternType()))
                .count();
            if (godObjects > 0) {
                suggestions.add("Refactor " + godObjects + " God objects to follow Single Responsibility Principle");
            }
        }
        
        if (repo.getHealthScore() != null && repo.getHealthScore() < 70) {
            suggestions.add("Consider comprehensive refactoring to improve overall code quality");
        }
        
        return suggestions;
    }
}