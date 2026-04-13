package org.example.projectanalysis;

import java.util.*;

/**
 * 🧠 Project Analysis Model - Stores complete analysis of any project
 * Saved to Firebase: /project_analyses/{id}
 */
public class ProjectAnalysis {
    
    private String id = UUID.randomUUID().toString();
    private String projectName;
    private String projectPath;
    private String projectType;
    private long analyzedAt = System.currentTimeMillis();
    
    // Basic Stats
    private int totalFiles;
    private int totalLinesOfCode;
    private int totalDirectories;
    
    // Language Distribution
    private Map<String, Integer> languageStats = new HashMap<>();
    
    // File Structure
    private List<FileNode> fileTree = new ArrayList<>();
    
    // Code Quality Metrics
    private CodeQualityMetrics qualityMetrics = new CodeQualityMetrics();
    
    // Architecture Analysis
    private ArchitectureAnalysis architecture = new ArchitectureAnalysis();
    
    // Issues & Suggestions
    private List<Issue> issues = new ArrayList<>();
    private List<Suggestion> suggestions = new ArrayList<>();
    
    // Summary
    private String summary;
    private String overallHealth; // EXCELLENT, GOOD, FAIR, POOR
    private int healthScore; // 0-100
    
    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    
    public String getProjectPath() { return projectPath; }
    public void setProjectPath(String projectPath) { this.projectPath = projectPath; }
    
    public String getProjectType() { return projectType; }
    public void setProjectType(String projectType) { this.projectType = projectType; }
    
    public long getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(long analyzedAt) { this.analyzedAt = analyzedAt; }
    
    public int getTotalFiles() { return totalFiles; }
    public void setTotalFiles(int totalFiles) { this.totalFiles = totalFiles; }
    
    public int getTotalLinesOfCode() { return totalLinesOfCode; }
    public void setTotalLinesOfCode(int totalLinesOfCode) { this.totalLinesOfCode = totalLinesOfCode; }
    
    public int getTotalDirectories() { return totalDirectories; }
    public void setTotalDirectories(int totalDirectories) { this.totalDirectories = totalDirectories; }
    
    public Map<String, Integer> getLanguageStats() { return languageStats; }
    public void setLanguageStats(Map<String, Integer> languageStats) { this.languageStats = languageStats; }
    
    public List<FileNode> getFileTree() { return fileTree; }
    public void setFileTree(List<FileNode> fileTree) { this.fileTree = fileTree; }
    
    public CodeQualityMetrics getQualityMetrics() { return qualityMetrics; }
    public void setQualityMetrics(CodeQualityMetrics qualityMetrics) { this.qualityMetrics = qualityMetrics; }
    
    public ArchitectureAnalysis getArchitecture() { return architecture; }
    public void setArchitecture(ArchitectureAnalysis architecture) { this.architecture = architecture; }
    
    public List<Issue> getIssues() { return issues; }
    public void setIssues(List<Issue> issues) { this.issues = issues; }
    
    public List<Suggestion> getSuggestions() { return suggestions; }
    public void setSuggestions(List<Suggestion> suggestions) { this.suggestions = suggestions; }
    
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    
    public String getOverallHealth() { return overallHealth; }
    public void setOverallHealth(String overallHealth) { this.overallHealth = overallHealth; }
    
    public int getHealthScore() { return healthScore; }
    public void setHealthScore(int healthScore) { this.healthScore = healthScore; }
    
    // Helper Methods
    public void addLanguage(String lang, int lines) {
        languageStats.merge(lang, lines, (existing, newLines) -> existing + newLines);
    }
    
    public void addIssue(Issue issue) {
        issues.add(issue);
    }
    
    public void addSuggestion(Suggestion suggestion) {
        suggestions.add(suggestion);
    }
    
    public Map<String, Object> toFirebaseMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("projectName", projectName);
        map.put("projectPath", projectPath);
        map.put("projectType", projectType);
        map.put("analyzedAt", analyzedAt);
        map.put("totalFiles", totalFiles);
        map.put("totalLinesOfCode", totalLinesOfCode);
        map.put("totalDirectories", totalDirectories);
        map.put("languageStats", languageStats);
        map.put("overallHealth", overallHealth);
        map.put("healthScore", healthScore);
        map.put("summary", summary);
        return map;
    }
    
    // ==================== Inner Classes ====================
    
    public static class FileNode {
        private String name;
        private String path;
        private String type; // file or directory
        private int lines;
        private String language;
        private List<FileNode> children = new ArrayList<>();
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public int getLines() { return lines; }
        public void setLines(int lines) { this.lines = lines; }
        
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        
        public List<FileNode> getChildren() { return children; }
        public void setChildren(List<FileNode> children) { this.children = children; }
    }
    
    public static class CodeQualityMetrics {
        private int averageMethodLength;
        private int maxMethodLength;
        private int totalMethods;
        private int totalClasses;
        private double commentRatio; // percentage
        private int duplicateCodeBlocks;
        private int complexityScore;
        
        public int getAverageMethodLength() { return averageMethodLength; }
        public void setAverageMethodLength(int averageMethodLength) { this.averageMethodLength = averageMethodLength; }
        
        public int getMaxMethodLength() { return maxMethodLength; }
        public void setMaxMethodLength(int maxMethodLength) { this.maxMethodLength = maxMethodLength; }
        
        public int getTotalMethods() { return totalMethods; }
        public void setTotalMethods(int totalMethods) { this.totalMethods = totalMethods; }
        
        public int getTotalClasses() { return totalClasses; }
        public void setTotalClasses(int totalClasses) { this.totalClasses = totalClasses; }
        
        public double getCommentRatio() { return commentRatio; }
        public void setCommentRatio(double commentRatio) { this.commentRatio = commentRatio; }
        
        public int getDuplicateCodeBlocks() { return duplicateCodeBlocks; }
        public void setDuplicateCodeBlocks(int duplicateCodeBlocks) { this.duplicateCodeBlocks = duplicateCodeBlocks; }
        
        public int getComplexityScore() { return complexityScore; }
        public void setComplexityScore(int complexityScore) { this.complexityScore = complexityScore; }
    }
    
    public static class ArchitectureAnalysis {
        private List<String> layers = new ArrayList<>();
        private List<String> designPatterns = new ArrayList<>();
        private List<String> frameworks = new ArrayList<>();
        private List<String> dependencies = new ArrayList<>();
        private String architectureType; // MVC, Microservices, etc.
        
        public List<String> getLayers() { return layers; }
        public void setLayers(List<String> layers) { this.layers = layers; }
        
        public List<String> getDesignPatterns() { return designPatterns; }
        public void setDesignPatterns(List<String> designPatterns) { this.designPatterns = designPatterns; }
        
        public List<String> getFrameworks() { return frameworks; }
        public void setFrameworks(List<String> frameworks) { this.frameworks = frameworks; }
        
        public List<String> getDependencies() { return dependencies; }
        public void setDependencies(List<String> dependencies) { this.dependencies = dependencies; }
        
        public String getArchitectureType() { return architectureType; }
        public void setArchitectureType(String architectureType) { this.architectureType = architectureType; }
    }
    
    public static class Issue {
        private String severity; // CRITICAL, HIGH, MEDIUM, LOW
        private String category; // SECURITY, PERFORMANCE, CODE_QUALITY, etc.
        private String file;
        private int line;
        private String message;
        private String suggestion;
        
        public Issue() {}
        
        public Issue(String severity, String category, String file, String message) {
            this.severity = severity;
            this.category = category;
            this.file = file;
            this.message = message;
        }
        
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public String getFile() { return file; }
        public void setFile(String file) { this.file = file; }
        
        public int getLine() { return line; }
        public void setLine(int line) { this.line = line; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    }
    
    public static class Suggestion {
        private String priority; // HIGH, MEDIUM, LOW
        private String category;
        private String title;
        private String description;
        private String expectedBenefit;
        private int effortHours;
        
        public Suggestion() {}
        
        public Suggestion(String priority, String category, String title, String description) {
            this.priority = priority;
            this.category = category;
            this.title = title;
            this.description = description;
        }
        
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getExpectedBenefit() { return expectedBenefit; }
        public void setExpectedBenefit(String expectedBenefit) { this.expectedBenefit = expectedBenefit; }
        
        public int getEffortHours() { return effortHours; }
        public void setEffortHours(int effortHours) { this.effortHours = effortHours; }
    }
}
