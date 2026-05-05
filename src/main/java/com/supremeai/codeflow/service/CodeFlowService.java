package com.supremeai.codeflow.service;

import com.supremeai.codeflow.analyzer.CodeAnalyzer;
import com.supremeai.codeflow.analyzer.DependencyAnalyzer;
import com.supremeai.codeflow.analyzer.HealthScorer;
import com.supremeai.codeflow.analyzer.PatternDetector;
import com.supremeai.codeflow.analyzer.SecurityScanner;
import com.supremeai.codeflow.model.CodeRepository;
import com.supremeai.codeflow.repository.CodeFlowRepository;
import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import com.supremeai.provider.AIProviderSwitcher;
import lombok.Data;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Main service for CodeFlow module
 * Handles code analysis, AI integration, and health scoring
 */
@Service
public class CodeFlowService {
    
    private static final Logger logger = LoggerFactory.getLogger(CodeFlowService.class);
    
    @Autowired
    private CodeFlowRepository repository;
    
    @Autowired
    private AIProviderFactory providerFactory;
    
    @Autowired
    private AIProviderSwitcher providerSwitcher;
    
    @Autowired
    private CodeAnalyzer codeAnalyzer;
    
    @Autowired
    private SecurityScanner securityScanner;
    
    @Autowired
    private PatternDetector patternDetector;
    
    @Autowired
    private HealthScorer healthScorer;
    
    @Autowired
    private DependencyAnalyzer dependencyAnalyzer;
    
    /**
     * Analyze a repository from URL
     */
    @Transactional
    public CodeRepository analyzeRepository(String repoUrl, String sourceType, String ownerId) 
            throws Exception {
        logger.info("Starting analysis for repository: {}, source: {}", repoUrl, sourceType);
        
        // Create repository entry
        CodeRepository repo = CodeRepository.builder()
            .name(extractRepoName(repoUrl))
            .fullName(repoUrl)
            .cloneUrl(repoUrl)
            .sourceType(sourceType)
            .sourceId(generateSourceId(repoUrl))
            .ownerId(ownerId)
            .ownerType("USER")
            .analysisStatus(CodeRepository.AnalysisStatus.ANALYZING)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .cached(false)
            .version(1)
            .build();
        
        repo = repository.save(repo);
        
        try {
            // Perform analysis asynchronously
            CompletableFuture.runAsync(() -> {
                try {
                    performFullAnalysis(repo.getId());
                } catch (Exception e) {
                    logger.error("Analysis failed for repo: " + repo.getId(), e);
                    try {
                        repository.updateAnalysisStatus(repo.getId(), 
                            CodeRepository.AnalysisStatus.FAILED);
                    } catch (Exception ex) {
                        logger.error("Failed to update status", ex);
                    }
                }
            });
            
            return repo;
        } catch (Exception e) {
            repository.updateAnalysisStatus(repo.getId(), CodeRepository.AnalysisStatus.FAILED);
            throw e;
        }
    }
    
    /**
     * Perform complete analysis of a repository
     */
    @Transactional
    public void performFullAnalysis(String repositoryId) throws Exception {
        long startTime = System.currentTimeMillis();
        
        CodeRepository repo = repository.findById(repositoryId)
            .orElseThrow(() -> new RuntimeException("Repository not found: " + repositoryId));
        
        try {
            // Step 1: Parse code files
            logger.info("Step 1: Parsing code files for {}", repositoryId);
            List<CodeRepository.CodeFile> files = codeAnalyzer.parseRepository(repo.getCloneUrl());
            repo.setFiles(files);
            repo.setTotalFiles(files.size());
            
            // Calculate language stats
            Map<String, Integer> langStats = files.stream()
                .collect(Collectors.groupingBy(
                    CodeRepository.CodeFile::getLanguage,
                    Collectors.summingInt(f -> 1)
                ));
            repo.setLanguageStats(langStats);
            
            // Step 2: Build dependency graph
            logger.info("Step 2: Building dependency graph for {}", repositoryId);
            CodeRepository.DependencyGraph graph = dependencyAnalyzer.buildDependencyGraph(files);
            repo.setDependencyGraph(graph);
            
            // Step 3: Security scanning
            logger.info("Step 3: Running security scan for {}", repositoryId);
            List<CodeRepository.SecurityIssue> securityIssues = securityScanner.scan(files);
            repo.setSecurityIssues(securityIssues);
            
            // Step 4: Pattern detection
            logger.info("Step 4: Detecting patterns for {}", repositoryId);
            List<CodeRepository.PatternDetection> patterns = patternDetector.detectPatterns(files);
            repo.setDetectedPatterns(patterns);
            
            // Step 5: Dead code detection
            logger.info("Step 5: Detecting dead code for {}", repositoryId);
            List<CodeRepository.DeadCode> deadCode = codeAnalyzer.detectDeadCode(files);
            repo.setDeadCode(deadCode);
            
            // Step 6: Circular dependency detection
            logger.info("Step 6: Detecting circular dependencies for {}", repositoryId);
            List<CodeRepository.CircularDependency> circularDeps = 
                dependencyAnalyzer.detectCircularDependencies(graph);
            repo.setCircularDependencies(circularDeps);
            
            // Step 7: AI-powered analysis
            logger.info("Step 7: Running AI analysis for {}", repositoryId);
            runAIAnalysis(repo);
            
            // Step 8: Calculate health score
            logger.info("Step 8: Calculating health score for {}", repositoryId);
            calculateHealthMetrics(repo);
            
            // Update completion status
            repo.setAnalysisStatus(CodeRepository.AnalysisStatus.COMPLETED);
            repo.setLastAnalyzedAt(Instant.now());
            repo.setAnalysisDurationMs(System.currentTimeMillis() - startTime);
            repo.setCached(true);
            repo.setCacheExpiresAt(Instant.now().plusSeconds(86400)); // 24 hours
            
            repository.save(repo);
            
            logger.info("Analysis completed for {} in {}ms", repositoryId,
                System.currentTimeMillis() - startTime);
            
        } catch (Exception e) {
            logger.error("Analysis failed for repository: " + repositoryId, e);
            repo.setAnalysisStatus(CodeRepository.AnalysisStatus.FAILED);
            repository.save(repo);
            throw e;
        }
    }
    
    /**
     * Run AI-powered analysis using the multi-AI routing system
     */
    private void runAIAnalysis(CodeRepository repo) throws Exception {
        List<CodeRepository.AISuggestion> suggestions = new ArrayList<>();
        List<CodeRepository.ErrorAnalysis> errorAnalyses = new ArrayList<>();
        
        // Use AI for complex pattern recognition
        AIProvider aiProvider = providerFactory.getBestProviderForTask("code_analysis");
        
        // Analyze each file for optimization opportunities
        for (CodeRepository.CodeFile file : repo.getFiles()) {
            if (file.getLinesOfCode() > 50) { // Only analyze substantial files
                try {
                    String prompt = buildAnalysisPrompt(file);
                    String response = aiProvider.generate(prompt).block();
                    
                    CodeRepository.AISuggestion suggestion = parseAISuggestion(response, file);
                    if (suggestion != null) {
                        suggestions.add(suggestion);
                    }
                } catch (Exception e) {
                    logger.warn("AI analysis failed for file: " + file.getPath(), e);
                }
            }
        }
        
        // Analyze security issues with AI
        for (CodeRepository.SecurityIssue issue : repo.getSecurityIssues()) {
            if (issue.getSeverity().equals("HIGH") || issue.getSeverity().equals("CRITICAL")) {
                try {
                    String prompt = buildSecurityAnalysisPrompt(issue);
                    String response = providerSwitcher.switchProvider(
                        aiProvider.getName(), "security_analysis", prompt, "system");
                    
                    CodeRepository.ErrorAnalysis errorAnalysis = parseErrorAnalysis(response, issue);
                    if (errorAnalysis != null) {
                        errorAnalyses.add(errorAnalysis);
                    }
                } catch (Exception e) {
                    logger.warn("AI security analysis failed", e);
                }
            }
        }
        
        repo.setAiSuggestions(suggestions);
        repo.setErrorAnalyses(errorAnalyses);
        repo.setLastAnalysisProvider(aiProvider.getName());
    }
    
    /**
     * Calculate health score and grade
     */
    private void calculateHealthMetrics(CodeRepository repo) {
        com.supremeai.codeflow.analyzer.HealthScorer.HealthScoreResult result = healthScorer.calculateScore(repo);
        repo.setHealthScore(result.getScore());
        repo.setHealthGrade(result.getGrade());
        
        // Add health issues
        List<CodeRepository.HealthIssue> issues = new ArrayList<>();
        if (repo.getSecurityIssues() != null) {
            for (CodeRepository.SecurityIssue secIssue : repo.getSecurityIssues()) {
                if (secIssue.getSeverity().equals("CRITICAL") || secIssue.getSeverity().equals("HIGH")) {
                    issues.add(CodeRepository.HealthIssue.builder()
                        .type("SECURITY")
                        .severity(secIssue.getSeverity())
                        .description(secIssue.getDescription())
                        .file(secIssue.getFile())
                        .line(secIssue.getLine())
                        .suggestion(secIssue.getRemediation())
                        .build());
                }
            }
        }
        
        if (repo.getCircularDependencies() != null && !repo.getCircularDependencies().isEmpty()) {
            issues.add(CodeRepository.HealthIssue.builder()
                .type("ARCHITECTURE")
                .severity("HIGH")
                .description("Circular dependencies detected")
                .suggestion("Refactor to remove circular dependencies")
                .build());
        }
        
        repo.setHealthIssues(issues);
    }
    
    /**
     * Get repository analysis results
     */
    public CodeRepository getAnalysis(String repositoryId) throws Exception {
        CodeRepository repo = repository.findById(repositoryId)
            .orElseThrow(() -> new RuntimeException("Repository not found"));
        
        // Check if cache is valid
        if (repo.getCached() != null && repo.getCached() && 
            repo.getCacheExpiresAt() != null && 
            repo.getCacheExpiresAt().after(new Date())) {
            return repo;
        }
        
        // Trigger re-analysis if cache expired
        if (repo.getAnalysisStatus() != CodeRepository.AnalysisStatus.ANALYZING) {
            performFullAnalysis(repositoryId);
            repo = repository.findById(repositoryId).get();
        }
        
        return repo;
    }
    
    /**
     * Get analysis for multiple repositories
     */
    public List<CodeRepository> getAnalysesForOwner(String ownerId) throws Exception {
        return repository.findByOwnerId(ownerId);
    }
    
    /**
     * Analyze pull request impact
     */
    public CodeRepository.PullRequestAnalysis analyzePullRequest(
            String repositoryId, List<String> changedFiles) throws Exception {
        
        CodeRepository repo = repository.findById(repositoryId)
            .orElseThrow(() -> new RuntimeException("Repository not found"));
        
        CodeRepository.PullRequestAnalysis analysis = 
            CodeRepository.PullRequestAnalysis.builder()
                .prId(UUID.randomUUID().toString())
                .title("PR Analysis")
                .author("system")
                .createdAt(Instant.now())
                .changedFiles(changedFiles.size())
                .build();
        
        // Calculate risk score based on affected components
        int riskScore = 0;
        List<String> affectedComponents = new ArrayList<>();
        
        if (repo.getDependencyGraph() != null) {
            for (String file : changedFiles) {
                // Check if file is in critical path
                if (repo.getDependencyGraph().getCriticalPath() != null &&
                    repo.getDependencyGraph().getCriticalPath().contains(file)) {
                    riskScore += 30;
                    affectedComponents.add(file);
                }
                
                // Check centrality
                if (repo.getDependencyGraph().getCentralityScores() != null &&
                    repo.getDependencyGraph().getCentralityScores().get(file) != null &&
                    repo.getDependencyGraph().getCentralityScores().get(file) > 0.5) {
                    riskScore += 20;
                }
            }
        }
        
        analysis.setRiskScore(Math.min(riskScore, 100));
        analysis.setAffectedComponents(affectedComponents);
        
        // Suggest reviewers based on file ownership
        analysis.setSuggestedReviewers(suggestReviewers(changedFiles, repo));
        
        analysis.setAnalysisSummary("Risk score: " + riskScore + "/100. " +
            affectedComponents.size() + " critical components affected.");
        
        return analysis;
    }
    
    /**
     * Suggest reviewers for PR
     */
    private List<String> suggestReviewers(List<String> changedFiles, CodeRepository repo) {
        // Simple implementation - in production, would use git blame and ownership data
        return Arrays.asList("senior-dev-1", "tech-lead");
    }
    
    /**
     * Build AI prompt for code analysis
     */
    private String buildAnalysisPrompt(CodeRepository.CodeFile file) {
        return String.format(
            "Analyze this code file for optimization opportunities:\n\n" +
            "File: %s\n" +
            "Language: %s\n" +
            "Lines of code: %d\n" +
            "Complexity: %d\n\n" +
            "Please provide:\n" +
            "1. Performance improvements\n" +
            "2. Code quality suggestions\n" +
            "3. Potential bugs\n" +
            "4. Refactoring opportunities",
            file.getPath(), file.getLanguage(), file.getLinesOfCode(), file.getComplexity()
        );
    }
    
    /**
     * Build AI prompt for security analysis
     */
    private String buildSecurityAnalysisPrompt(CodeRepository.SecurityIssue issue) {
        return String.format(
            "Analyze this security issue and provide detailed remediation:\n\n" +
            "Type: %s\n" +
            "Severity: %s\n" +
            "File: %s\n" +
            "Line: %d\n" +
            "Description: %s\n" +
            "Code: %s\n\n" +
            "Please provide:\n" +
            "1. Root cause analysis\n" +
            "2. Detailed fix\n" +
            "3. Prevention strategies",
            issue.getType(), issue.getSeverity(), issue.getFile(), 
            issue.getLine(), issue.getDescription(), issue.getCodeSnippet()
        );
    }
    
    /**
     * Parse AI suggestion from response
     */
    private CodeRepository.AISuggestion parseAISuggestion(String response, CodeRepository.CodeFile file) {
        if (response == null || response.isEmpty()) {
            return null;
        }
        
        return CodeRepository.AISuggestion.builder()
            .type("OPTIMIZE")
            .description("AI optimization suggestion")
            .file(file.getPath())
            .line(1)
            .suggestion(response.substring(0, Math.min(response.length(), 500)))
            .provider("AI")
            .confidence(80)
            .generatedAt(Instant.now())
            .build();
    }
    
    /**
     * Parse error analysis from AI response
     */
    private CodeRepository.ErrorAnalysis parseErrorAnalysis(String response, CodeRepository.SecurityIssue issue) {
        if (response == null || response.isEmpty()) {
            return null;
        }
        
        return CodeRepository.ErrorAnalysis.builder()
            .errorType(issue.getType())
            .file(issue.getFile())
            .line(issue.getLine())
            .rootCause(response.substring(0, Math.min(response.length(), 300)))
            .suggestedFix(response.substring(Math.min(response.length(), 300), 
                Math.min(response.length(), 600)))
            .affectedNodes(Arrays.asList(issue.getFile()))
            .provider("AI")
            .confidence(85)
            .build();
    }
    
    /**
     * Extract repository name from URL
     */
    private String extractRepoName(String url) {
        String[] parts = url.split("/");
        return parts[parts.length - 1].replace(".git", "");
    }
    
    /**
     * Generate source ID
     */
    private String generateSourceId(String url) {
        return UUID.nameUUIDFromBytes(url.getBytes()).toString();
    }
    
    /**
     * Health score calculation result
     */
    @Data
    @Builder
    public static class HealthScoreResult {
        private Integer score;
        private String grade;
    }
}