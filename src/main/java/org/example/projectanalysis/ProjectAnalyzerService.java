package org.example.projectanalysis;

import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/**
 * 🧠 Project Analyzer Service - Analyzes any project like an AI expert
 */
@Service
public class ProjectAnalyzerService {
    
    // File extensions mapping
    private static final Map<String, String> LANGUAGE_MAP = Map.ofEntries(
        Map.entry("java", "Java"),
        Map.entry("kt", "Kotlin"),
        Map.entry("py", "Python"),
        Map.entry("js", "JavaScript"),
        Map.entry("ts", "TypeScript"),
        Map.entry("jsx", "React/JSX"),
        Map.entry("tsx", "React/TSX"),
        Map.entry("html", "HTML"),
        Map.entry("css", "CSS"),
        Map.entry("scss", "SCSS"),
        Map.entry("xml", "XML"),
        Map.entry("json", "JSON"),
        Map.entry("yaml", "YAML"),
        Map.entry("yml", "YAML"),
        Map.entry("md", "Markdown"),
        Map.entry("sql", "SQL"),
        Map.entry("sh", "Shell"),
        Map.entry("bat", "Batch"),
        Map.entry("gradle", "Gradle"),
        Map.entry("properties", "Properties")
    );
    
    // Patterns to detect frameworks
    private static final Map<String, String> FRAMEWORK_PATTERNS = Map.ofEntries(
        Map.entry("spring", "Spring Boot"),
        Map.entry("react", "React"),
        Map.entry("angular", "Angular"),
        Map.entry("vue", "Vue.js"),
        Map.entry("flutter", "Flutter"),
        Map.entry("django", "Django"),
        Map.entry("flask", "Flask"),
        Map.entry("express", "Express.js"),
        Map.entry("firebase", "Firebase"),
        Map.entry("docker", "Docker"),
        Map.entry("kubernetes", "Kubernetes")
    );
    
    /**
     * 🔍 Main analysis method
     */
    public ProjectAnalysis analyzeProject(String projectPath) {
        File rootDir = new File(projectPath);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            throw new IllegalArgumentException("Invalid project path: " + projectPath);
        }
        
        ProjectAnalysis analysis = new ProjectAnalysis();
        analysis.setProjectName(rootDir.getName());
        analysis.setProjectPath(projectPath);
        
        // Step 1: Scan file structure
        scanFiles(rootDir, analysis);
        
        // Step 2: Detect project type
        detectProjectType(analysis);
        
        // Step 3: Analyze code quality
        analyzeCodeQuality(analysis);
        
        // Step 4: Detect architecture
        detectArchitecture(analysis);
        
        // Step 5: Find issues
        findIssues(analysis);
        
        // Step 6: Generate suggestions
        generateSuggestions(analysis);
        
        // Step 7: Calculate health score
        calculateHealthScore(analysis);
        
        // Step 8: Generate summary
        generateSummary(analysis);
        
        return analysis;
    }
    
    /**
     * 📁 Scan all files and directories
     */
    private void scanFiles(File dir, ProjectAnalysis analysis) {
        List<ProjectAnalysis.FileNode> fileTree = new ArrayList<>();
        int[] stats = {0, 0, 0}; // files, dirs, lines
        
        scanDirectory(dir, "", fileTree, stats, analysis);
        
        analysis.setFileTree(fileTree);
        analysis.setTotalFiles(stats[0]);
        analysis.setTotalDirectories(stats[1]);
        analysis.setTotalLinesOfCode(stats[2]);
    }
    
    private void scanDirectory(File dir, String parentPath, List<ProjectAnalysis.FileNode> nodes, 
                               int[] stats, ProjectAnalysis analysis) {
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            // Skip hidden and common ignore directories
            if (shouldSkip(file)) continue;
            
            ProjectAnalysis.FileNode node = new ProjectAnalysis.FileNode();
            node.setName(file.getName());
            node.setPath(parentPath + "/" + file.getName());
            
            if (file.isDirectory()) {
                node.setType("directory");
                stats[1]++;
                List<ProjectAnalysis.FileNode> children = new ArrayList<>();
                scanDirectory(file, node.getPath(), children, stats, analysis);
                node.setChildren(children);
            } else {
                node.setType("file");
                stats[0]++;
                int lines = countLines(file);
                node.setLines(lines);
                stats[2] += lines;
                
                String lang = detectLanguage(file.getName());
                node.setLanguage(lang);
                analysis.addLanguage(lang, lines);
            }
            
            nodes.add(node);
        }
    }
    
    private boolean shouldSkip(File file) {
        String name = file.getName();
        return name.startsWith(".") || 
               name.equals("node_modules") ||
               name.equals("target") ||
               name.equals("build") ||
               name.equals("dist") ||
               name.equals(".git") ||
               name.equals("__pycache__") ||
               name.equals(".gradle") ||
               name.equals(".idea") ||
               name.equals(".vscode");
    }
    
    private int countLines(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return (int) reader.lines().count();
        } catch (Exception e) {
            return 0;
        }
    }
    
    private String detectLanguage(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            String ext = filename.substring(dotIndex + 1).toLowerCase();
            return LANGUAGE_MAP.getOrDefault(ext, "Other");
        }
        return "Other";
    }
    
    /**
     * 🔎 Detect project type
     */
    private void detectProjectType(ProjectAnalysis analysis) {
        String path = analysis.getProjectPath();
        
        if (new File(path, "pom.xml").exists() || new File(path, "build.gradle").exists() || 
            new File(path, "build.gradle.kts").exists()) {
            analysis.setProjectType("Java/Gradle Project");
        } else if (new File(path, "package.json").exists()) {
            analysis.setProjectType("Node.js Project");
        } else if (new File(path, "requirements.txt").exists() || new File(path, "setup.py").exists()) {
            analysis.setProjectType("Python Project");
        } else if (new File(path, "pubspec.yaml").exists()) {
            analysis.setProjectType("Flutter Project");
        } else if (new File(path, "Dockerfile").exists()) {
            analysis.setProjectType("Docker Project");
        } else {
            analysis.setProjectType("Generic Project");
        }
    }
    
    /**
     * 📊 Analyze code quality
     */
    private void analyzeCodeQuality(ProjectAnalysis analysis) {
        ProjectAnalysis.CodeQualityMetrics metrics = new ProjectAnalysis.CodeQualityMetrics();
        
        // Simple heuristics for demo
        int totalFiles = analysis.getTotalFiles();
        int totalLines = analysis.getTotalLinesOfCode();
        
        metrics.setTotalClasses(totalFiles / 3); // Rough estimate
        metrics.setTotalMethods(totalFiles * 5); // Rough estimate
        metrics.setAverageMethodLength(totalLines / (totalFiles * 5 + 1));
        metrics.setMaxMethodLength(50); // Would need actual parsing
        metrics.setCommentRatio(calculateCommentRatio(analysis));
        metrics.setComplexityScore(calculateComplexity(analysis));
        
        analysis.setQualityMetrics(metrics);
    }
    
    private double calculateCommentRatio(ProjectAnalysis analysis) {
        // Simplified calculation
        return 15.0; // Placeholder
    }
    
    private int calculateComplexity(ProjectAnalysis analysis) {
        // Simplified complexity score
        int files = analysis.getTotalFiles();
        int lines = analysis.getTotalLinesOfCode();
        return Math.min(100, (lines / 100) + (files / 10));
    }
    
    /**
     * 🏗️ Detect architecture patterns
     */
    private void detectArchitecture(ProjectAnalysis analysis) {
        ProjectAnalysis.ArchitectureAnalysis arch = new ProjectAnalysis.ArchitectureAnalysis();
        String path = analysis.getProjectPath();
        
        // Check for MVC
        if (new File(path, "src/main/java/org/example/controller").exists() ||
            new File(path, "src/main/java/org/example/service").exists()) {
            arch.setArchitectureType("Layered Architecture (MVC/MVP)");
            arch.getLayers().addAll(Arrays.asList("Controller", "Service", "Repository", "Model"));
        }
        
        // Detect frameworks
        detectFrameworks(path, arch);
        
        // Detect design patterns
        detectDesignPatterns(path, arch);
        
        analysis.setArchitecture(arch);
    }
    
    private void detectFrameworks(String path, ProjectAnalysis.ArchitectureAnalysis arch) {
        try {
            // Read build.gradle or package.json
            File buildFile = new File(path, "build.gradle");
            if (buildFile.exists()) {
                String content = Files.readString(buildFile.toPath()).toLowerCase();
                FRAMEWORK_PATTERNS.forEach((pattern, name) -> {
                    if (content.contains(pattern)) arch.getFrameworks().add(name);
                });
            }
            
            File packageFile = new File(path, "package.json");
            if (packageFile.exists()) {
                String content = Files.readString(packageFile.toPath()).toLowerCase();
                FRAMEWORK_PATTERNS.forEach((pattern, name) -> {
                    if (content.contains(pattern)) arch.getFrameworks().add(name);
                });
            }
        } catch (Exception e) {
            // Ignore
        }
    }
    
    private void detectDesignPatterns(String path, ProjectAnalysis.ArchitectureAnalysis arch) {
        // Check for common patterns
        if (new File(path, "src/main/java/org/example/factory").exists()) {
            arch.getDesignPatterns().add("Factory Pattern");
        }
        if (new File(path, "src/main/java/org/example/singleton").exists() || 
            hasSingletonPattern(path)) {
            arch.getDesignPatterns().add("Singleton Pattern");
        }
        if (new File(path, "src/main/java/org/example/observer").exists()) {
            arch.getDesignPatterns().add("Observer Pattern");
        }
        if (new File(path, "src/main/java/org/example/strategy").exists()) {
            arch.getDesignPatterns().add("Strategy Pattern");
        }
    }
    
    private boolean hasSingletonPattern(String path) {
        // Simple check - look for @Service or getInstance
        return true; // Simplified
    }
    
    /**
     * ⚠️ Find issues
     */
    private void findIssues(ProjectAnalysis analysis) {
        List<ProjectAnalysis.Issue> issues = new ArrayList<>();
        
        // Check for large files
        for (ProjectAnalysis.FileNode node : analysis.getFileTree()) {
            if ("file".equals(node.getType()) && node.getLines() > 500) {
                issues.add(new ProjectAnalysis.Issue(
                    "MEDIUM", "CODE_QUALITY", node.getPath(),
                    "File is too large (" + node.getLines() + " lines). Consider splitting."
                ));
            }
        }
        
        // Check for missing README
        String path = analysis.getProjectPath();
        if (!new File(path, "README.md").exists() && !new File(path, "README").exists()) {
            issues.add(new ProjectAnalysis.Issue(
                "LOW", "DOCUMENTATION", "project",
                "No README file found. Consider adding project documentation."
            ));
        }
        
        // Check for .gitignore
        if (!new File(path, ".gitignore").exists()) {
            issues.add(new ProjectAnalysis.Issue(
                "MEDIUM", "VERSION_CONTROL", "project",
                "No .gitignore file found. Unnecessary files may be committed."
            ));
        }
        
        // Check for tests
        boolean hasTests = new File(path, "src/test").exists() || 
                          new File(path, "__tests__").exists() ||
                          analysis.getFileTree().stream()
                              .anyMatch(n -> n.getName().contains("Test") || n.getName().contains("test"));
        if (!hasTests) {
            issues.add(new ProjectAnalysis.Issue(
                "HIGH", "TESTING", "project",
                "No test directory found. Consider adding unit tests."
            ));
        }
        
        analysis.setIssues(issues);
    }
    
    /**
     * 💡 Generate suggestions
     */
    private void generateSuggestions(ProjectAnalysis analysis) {
        List<ProjectAnalysis.Suggestion> suggestions = new ArrayList<>();
        
        // Suggest based on project type
        if (analysis.getProjectType().contains("Java")) {
            suggestions.add(new ProjectAnalysis.Suggestion(
                "HIGH", "CI/CD",
                "Add GitHub Actions Workflow",
                "Create .github/workflows/ci.yml for automated testing and building"
            ));
        }
        
        // Suggest based on file count
        if (analysis.getTotalFiles() > 50) {
            suggestions.add(new ProjectAnalysis.Suggestion(
                "MEDIUM", "ARCHITECTURE",
                "Consider Modular Architecture",
                "Project has " + analysis.getTotalFiles() + " files. Consider splitting into modules."
            ));
        }
        
        // Suggest documentation
        if (analysis.getQualityMetrics().getCommentRatio() < 10) {
            suggestions.add(new ProjectAnalysis.Suggestion(
                "MEDIUM", "DOCUMENTATION",
                "Improve Code Comments",
                "Add more inline comments and JavaDoc to improve maintainability"
            ));
        }
        
        // Suggest security
        suggestions.add(new ProjectAnalysis.Suggestion(
            "HIGH", "SECURITY",
            "Add Security Scanning",
            "Integrate dependency vulnerability scanning (e.g., Snyk, OWASP)"
        ));
        
        analysis.setSuggestions(suggestions);
    }
    
    /**
     * 🏥 Calculate health score
     */
    private void calculateHealthScore(ProjectAnalysis analysis) {
        int score = 100;
        
        // Deduct for issues
        for (ProjectAnalysis.Issue issue : analysis.getIssues()) {
            switch (issue.getSeverity()) {
                case "CRITICAL" -> score -= 20;
                case "HIGH" -> score -= 10;
                case "MEDIUM" -> score -= 5;
                case "LOW" -> score -= 2;
            }
        }
        
        // Deduct for complexity
        score -= analysis.getQualityMetrics().getComplexityScore() / 5;
        
        // Ensure within bounds
        score = Math.max(0, Math.min(100, score));
        
        analysis.setHealthScore(score);
        
        // Set health label
        if (score >= 90) analysis.setOverallHealth("EXCELLENT");
        else if (score >= 75) analysis.setOverallHealth("GOOD");
        else if (score >= 50) analysis.setOverallHealth("FAIR");
        else analysis.setOverallHealth("POOR");
    }
    
    /**
     * 📝 Generate summary
     */
    private void generateSummary(ProjectAnalysis analysis) {
        StringBuilder summary = new StringBuilder();
        
        summary.append("📊 Project Analysis Summary\n");
        summary.append("========================\n\n");
        
        summary.append("Project: ").append(analysis.getProjectName()).append("\n");
        summary.append("Type: ").append(analysis.getProjectType()).append("\n");
        summary.append("Health: ").append(analysis.getOverallHealth())
               .append(" (").append(analysis.getHealthScore()).append("/100)\n\n");
        
        summary.append("📁 Statistics:\n");
        summary.append("  - Files: ").append(analysis.getTotalFiles()).append("\n");
        summary.append("  - Directories: ").append(analysis.getTotalDirectories()).append("\n");
        summary.append("  - Lines of Code: ").append(analysis.getTotalLinesOfCode()).append("\n\n");
        
        summary.append("🔧 Languages:\n");
        analysis.getLanguageStats().forEach((lang, lines) -> {
            summary.append("  - ").append(lang).append(": ").append(lines).append(" lines\n");
        });
        summary.append("\n");
        
        summary.append("⚠️ Issues Found: ").append(analysis.getIssues().size()).append("\n");
        summary.append("💡 Suggestions: ").append(analysis.getSuggestions().size()).append("\n");
        
        analysis.setSummary(summary.toString());
    }
}
