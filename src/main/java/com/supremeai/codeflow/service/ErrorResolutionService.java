package com.supremeai.codeflow.service;
import com.supremeai.codeflow.analyzer.CodeAnalyzer;

import com.supremeai.codeflow.model.CodeRepository;
import com.supremeai.codeflow.repository.CodeFlowRepository;
import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.Data;
import lombok.Builder;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Error resolution workflow service
 * Analyzes errors, identifies affected components, and generates fixes
 */
@Service
public class ErrorResolutionService {
    
    private static final Logger logger = LoggerFactory.getLogger(ErrorResolutionService.class);
    
    @Autowired
    private CodeFlowRepository repository;
    
    @Autowired
    private AIProviderFactory providerFactory;
    
    @Autowired
    private CodeAnalyzer codeAnalyzer;
    
    /**
     * Analyze error and generate resolution
     */
    public CodeRepository.ErrorAnalysis analyzeError(
            String repositoryId, String errorType, String stackTrace, String context) {
        
        try {
            CodeRepository repo = repository.findById(repositoryId)
                .orElseThrow(() -> new RuntimeException("Repository not found"));
            
            // Use AI to analyze the error
            AIProvider aiProvider = providerFactory.getBestProviderForTask("error_analysis");
            
            String prompt = buildErrorAnalysisPrompt(errorType, stackTrace, context, repo);
            String response = aiProvider.generate(prompt).block();
            
            // Parse AI response
            CodeRepository.ErrorAnalysis analysis = parseErrorAnalysis(response, errorType, stackTrace, repo);
            
            // Update repository with error analysis
            List<CodeRepository.ErrorAnalysis> analyses = repo.getErrorAnalyses();
            if (analyses == null) {
                analyses = new ArrayList<>();
            }
            analyses.add(analysis);
            repo.setErrorAnalyses(analyses);
            
            repository.save(repo);
            
            logger.info("Error analysis completed for repository: {}", repositoryId);
            return analysis;
            
        } catch (Exception e) {
            logger.error("Error analysis failed", e);
            throw new RuntimeException("Failed to analyze error: " + e.getMessage(), e);
        }
    }
    
    /**
     * Identify affected components from error
     */
    public List<String> identifyAffectedComponents(
            String repositoryId, String errorType, String stackTrace) {
        
        try {
            CodeRepository repo = repository.findById(repositoryId)
                .orElseThrow(() -> new RuntimeException("Repository not found"));
            
            List<String> affectedComponents = new ArrayList<>();
            
            // Parse stack trace to identify files and methods
            List<String> stackFrames = parseStackTrace(stackTrace);
            
            // Match against repository files
            for (String frame : stackFrames) {
                for (CodeRepository.CodeFile file : repo.getFiles()) {
                    if (frame.contains(file.getName()) || 
                        frame.contains(file.getPath().replace("/", "."))) {
                        affectedComponents.add(file.getPath());
                        
                        // Add related components from dependency graph
                        if (repo.getDependencyGraph() != null) {
                            affectedComponents.addAll(
                                findRelatedComponents(file.getPath(), repo.getDependencyGraph()));
                        }
                    }
                }
            }
            
            // Remove duplicates
            return affectedComponents.stream().distinct().collect(Collectors.toList());
            
        } catch (Exception e) {
            logger.error("Failed to identify affected components", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Generate fix for error
     */
    public CodeRepository.AISuggestion generateFix(
            String repositoryId, String errorType, String description) {
        
        try {
            CodeRepository repo = repository.findById(repositoryId)
                .orElseThrow(() -> new RuntimeException("Repository not found"));
            
            AIProvider aiProvider = providerFactory.getBestProviderForTask("fix_generation");
            
            String prompt = buildFixGenerationPrompt(errorType, description, repo);
            String response = aiProvider.generate(prompt).block();
            
            // Parse fix suggestion
            CodeRepository.AISuggestion suggestion = parseFixSuggestion(response, errorType);
            
            // Add to repository
            List<CodeRepository.AISuggestion> suggestions = repo.getAiSuggestions();
            if (suggestions == null) {
                suggestions = new ArrayList<>();
            }
            suggestions.add(suggestion);
            repo.setAiSuggestions(suggestions);
            
            repository.save(repo);
            
            return suggestion;
            
        } catch (Exception e) {
            logger.error("Fix generation failed", e);
            throw new RuntimeException("Failed to generate fix: " + e.getMessage(), e);
        }
    }
    
    /**
     * Track error pattern across repositories
     */
    public void trackErrorPattern(String errorType, String repositoryId) {
        try {
            // This would typically update a shared knowledge base
            logger.info("Tracking error pattern: {} in repository: {}", errorType, repositoryId);
            
            // In production, this would update Firestore with error pattern statistics
            // and common fixes
            
        } catch (Exception e) {
            logger.error("Failed to track error pattern", e);
        }
    }
    
    /**
     * Get common fixes for error type
     */
    public List<String> getCommonFixes(String errorType) {
        // In production, this would query Firestore for common fixes
        // For now, return some default suggestions
        
        Map<String, List<String>> commonFixes = new HashMap<>();
        commonFixes.put("NullPointerException", Arrays.asList(
            "Add null checks before accessing objects",
            "Use Optional to handle null values",
            "Initialize objects before use",
            "Check method return values for null"
        ));
        commonFixes.put("SQLException", Arrays.asList(
            "Check SQL syntax",
            "Verify database connection",
            "Use parameterized queries",
            "Check table and column names"
        ));
        commonFixes.put("IOException", Arrays.asList(
            "Check file paths",
            "Verify file permissions",
            "Ensure resources are closed",
            "Check disk space"
        ));
        
        return commonFixes.getOrDefault(errorType, Arrays.asList(
            "Review stack trace for root cause",
            "Check recent code changes",
            "Verify dependencies and versions",
            "Consult documentation"
        ));
    }
    
    /**
     * Build error analysis prompt
     */
    private String buildErrorAnalysisPrompt(
            String errorType, String stackTrace, String context, CodeRepository repo) {
        
        return String.format(
            "Analyze this error and provide detailed resolution:\n\n" +
            "Error Type: %s\n" +
            "Stack Trace:\n%s\n" +
            "Context: %s\n" +
            "Repository: %s\n" +
            "Language: %s\n\n" +
            "Please provide:\n" +
            "1. Root cause analysis\n" +
            "2. Affected components\n" +
            "3. Step-by-step fix\n" +
            "4. Prevention strategies\n" +
            "5. Code examples if applicable",
            errorType, stackTrace, context, repo.getName(), 
            repo.getLanguageStats() != null ? repo.getLanguageStats().keySet() : "unknown"
        );
    }
    
    /**
     * Build fix generation prompt
     */
    private String buildFixGenerationPrompt(
            String errorType, String description, CodeRepository repo) {
        
        return String.format(
            "Generate a fix for this issue:\n\n" +
            "Error Type: %s\n" +
            "Description: %s\n" +
            "Repository: %s\n\n" +
            "Please provide:\n" +
            "1. Code changes needed\n" +
            "2. Before and after code\n" +
            "3. Explanation of the fix\n" +
            "4. Testing recommendations",
            errorType, description, repo.getName()
        );
    }
    
    /**
     * Parse error analysis from AI response
     */
    private CodeRepository.ErrorAnalysis parseErrorAnalysis(
            String response, String errorType, String stackTrace, CodeRepository repo) {
        
        if (response == null || response.isEmpty()) {
            return null;
        }
        
        // Extract affected files from response
        List<String> affectedNodes = new ArrayList<>();
        if (repo.getFiles() != null) {
            for (CodeRepository.CodeFile file : repo.getFiles()) {
                if (response.contains(file.getName()) || 
                    response.contains(file.getPath())) {
                    affectedNodes.add(file.getPath());
                }
            }
        }
        
        return CodeRepository.ErrorAnalysis.builder()
            .errorType(errorType)
            .stackTrace(stackTrace)
            .rootCause(extractSection(response, "Root Cause", "Affected Components"))
            .file(extractPrimaryFile(affectedNodes))
            .line(0)
            .affectedNodes(affectedNodes)
            .suggestedFix(extractSection(response, "Fix", "Prevention"))
            .provider("AI")
            .confidence(80)
            .build();
    }
    
    /**
     * Parse fix suggestion from AI response
     */
    private CodeRepository.AISuggestion parseFixSuggestion(String response, String errorType) {
        if (response == null || response.isEmpty()) {
            return null;
        }
        
        return CodeRepository.AISuggestion.builder()
            .type("FIX")
            .description("AI-generated fix for " + errorType)
            .file("multiple")
            .line(0)
            .suggestion(response.substring(0, Math.min(response.length(), 1000)))
            .provider("AI")
            .confidence(85)
            .generatedAt(Instant.now())
            .build();
    }
    
    /**
     * Parse stack trace into frames
     */
    private List<String> parseStackTrace(String stackTrace) {
        List<String> frames = new ArrayList<>();
        if (stackTrace == null || stackTrace.isEmpty()) {
            return frames;
        }
        
        String[] lines = stackTrace.split("\\n");
        for (String line : lines) {
            if (line.contains("at ") && line.contains("(")) {
                frames.add(line);
            }
        }
        
        return frames;
    }
    
    /**
     * Find related components in dependency graph
     */
    private List<String> findRelatedComponents(
            String file, CodeRepository.DependencyGraph graph) {
        
        List<String> related = new ArrayList<>();
        
        if (graph.getEdges() != null) {
            for (CodeRepository.DependencyGraph.Edge edge : graph.getEdges()) {
                if (edge.getSource().equals(file)) {
                    related.add(edge.getTarget());
                }
                if (edge.getTarget().equals(file)) {
                    related.add(edge.getSource());
                }
            }
        }
        
        return related;
    }
    
    /**
     * Extract section from response
     */
    private String extractSection(String response, String start, String end) {
        int startIndex = response.indexOf(start);
        if (startIndex == -1) {
            return response.substring(0, Math.min(response.length(), 300));
        }
        
        int endIndex = response.indexOf(end, startIndex);
        if (endIndex == -1) {
            return response.substring(startIndex, Math.min(response.length(), startIndex + 300));
        }
        
        return response.substring(startIndex, endIndex).trim();
    }
    
    /**
     * Extract primary file from affected nodes
     */
    private String extractPrimaryFile(List<String> affectedNodes) {
        if (affectedNodes == null || affectedNodes.isEmpty()) {
            return "unknown";
        }
        return affectedNodes.get(0);
    }
    
    /**
     * Error resolution request
     */
    @Data
    @Builder
    public static class ErrorResolutionRequest {
        private String repositoryId;
        private String errorType;
        private String stackTrace;
        private String context;
        private String description;
    }
    
    /**
     * Error resolution response
     */
    @Data
    @Builder
    public static class ErrorResolutionResponse {
        private CodeRepository.ErrorAnalysis analysis;
        private CodeRepository.AISuggestion fix;
        private List<String> affectedComponents;
        private List<String> commonFixes;
        private String resolutionSteps;
    }
}