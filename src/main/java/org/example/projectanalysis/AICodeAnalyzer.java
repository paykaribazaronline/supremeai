package org.example.projectanalysis;

import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * 🤖 AI Code Analyzer - Learns from code patterns like an expert developer
 */
@Service
public class AICodeAnalyzer {
    
    // 🎯 Pattern Database - Kimi's Analysis Strength
    private final Map<String, List<CodePattern>> patternDatabase = new HashMap<>();
    
    public AICodeAnalyzer() {
        initializePatterns();
    }
    
    private void initializePatterns() {
        // 🔴 Anti-patterns (Bad practices)
        patternDatabase.put("ANTI_PATTERNS", Arrays.asList(
            new CodePattern("GOD_CLASS", 
                "Class has too many responsibilities",
                "class.*\\{.*\\n.*(public|private).*{50,}",
                "Split into smaller classes using Single Responsibility Principle"),
            new CodePattern("LONG_METHOD",
                "Method is too long",
                "(public|private|protected).*\\{.*\\n.*\\n.*\\n.*\\n.*\\n.*\\n.*\\n.*\\n.*\\n.*\\n.*\\n.*\\n.*\\n.*\\n.*\\n.*\\n.*\\n.*\\n.*\\n.*\\n.*\\n.*\\n",
                "Extract smaller methods, each doing one thing"),
            new CodePattern("MAGIC_NUMBERS",
                "Hardcoded numbers without constants",
                "[^a-zA-Z0-9](\\d{2,})[^a-zA-Z0-9]",
                "Replace with named constants"),
            new CodePattern("EMPTY_CATCH",
                "Empty catch block",
                "catch\\s*\\([^)]+\\)\\s*\\{\\s*\\}",
                "Add proper error handling or logging"),
            new CodePattern("SYSTEM_OUT",
                "Using System.out.print",
                "System\\.out\\.(print|println)",
                "Use proper logging framework (SLF4J, Log4j)")
        ));
        
        // 🟢 Best Practices
        patternDatabase.put("BEST_PRACTICES", Arrays.asList(
            new CodePattern("NULL_CHECKS",
                "Missing null checks",
                "\\.equals\\s*\\(",
                "Use Objects.equals() or add null checks"),
            new CodePattern("STRING_CONCAT",
                "String concatenation in loop",
                "for\\s*\\(.*\\+.*\\+",
                "Use StringBuilder for loop concatenation"),
            new CodePattern("RESOURCE_LEAK",
                "Potential resource leak",
                "new\\s+(FileInputStream|FileOutputStream|Socket).*[^}]\\s*\\}",
                "Use try-with-resources")
        ));
        
        // 🔵 Design Patterns Detection
        patternDatabase.put("DESIGN_PATTERNS", Arrays.asList(
            new CodePattern("SINGLETON",
                "Singleton Pattern detected",
                "getInstance\\s*\\(\\)|private\\s+static.*instance",
                "Ensure thread safety with volatile or enum"),
            new CodePattern("FACTORY",
                "Factory Pattern detected",
                "create[A-Z].*\\(|Factory",
                "Good! Consider abstract factory for complex cases"),
            new CodePattern("BUILDER",
                "Builder Pattern detected",
                "Builder.*\\{|\\.build\\s*\\(\\)",
                "Good! Use Lombok @Builder to reduce boilerplate")
        ));
        
        // 🟡 Security Patterns
        patternDatabase.put("SECURITY", Arrays.asList(
            new CodePattern("SQL_INJECTION",
                "Potential SQL Injection",
                "Statement.*execute.*\\+",
                "Use PreparedStatement with parameterized queries"),
            new CodePattern("HARDCODED_PASSWORD",
                "Hardcoded credentials",
                "(password|passwd|pwd|secret)\\s*=\\s*\"[^\"]+\"",
                "Use environment variables or secret manager"),
            new CodePattern("INSECURE_RANDOM",
                "Insecure random generation",
                "new\\s+Random\\s*\\(\\)",
                "Use SecureRandom for security purposes")
        ));
    }
    
    /**
     * 🔍 Analyze a single file with AI patterns
     */
    public List<PatternMatch> analyzeFile(File file) {
        List<PatternMatch> matches = new ArrayList<>();
        
        try {
            String content = Files.readString(file.toPath());
            
            for (Map.Entry<String, List<CodePattern>> category : patternDatabase.entrySet()) {
                for (CodePattern pattern : category.getValue()) {
                    Pattern regex = Pattern.compile(pattern.regex, Pattern.CASE_INSENSITIVE);
                    Matcher matcher = regex.matcher(content);
                    
                    int lineNum = 1;
                    int lastMatchEnd = 0;
                    
                    while (matcher.find()) {
                        // Calculate line number
                        for (int i = lastMatchEnd; i < matcher.start(); i++) {
                            if (content.charAt(i) == '\n') lineNum++;
                        }
                        
                        matches.add(new PatternMatch(
                            category.getKey(),
                            pattern.name,
                            pattern.description,
                            pattern.suggestion,
                            file.getPath(),
                            lineNum,
                            content.substring(matcher.start(), Math.min(matcher.end() + 30, content.length()))
                        ));
                        
                        lastMatchEnd = matcher.end();
                    }
                }
            }
        } catch (Exception e) {
            // Skip files that can't be read
        }
        
        return matches;
    }
    
    /**
     * 📊 Generate smart suggestions based on patterns
     */
    public List<SmartSuggestion> generateSmartSuggestions(List<PatternMatch> matches) {
        List<SmartSuggestion> suggestions = new ArrayList<>();
        
        // Group by severity
        long criticalCount = matches.stream().filter(m -> m.category.equals("SECURITY")).count();
        long antiPatternCount = matches.stream().filter(m -> m.category.equals("ANTI_PATTERNS")).count();
        
        if (criticalCount > 0) {
            suggestions.add(new SmartSuggestion(
                "CRITICAL",
                "Security Issues Found",
                "Found " + criticalCount + " potential security vulnerabilities. Review immediately.",
                "Run security scan and fix SQL injection, hardcoded passwords",
                4
            ));
        }
        
        if (antiPatternCount > 5) {
            suggestions.add(new SmartSuggestion(
                "HIGH",
                "Code Quality Improvement",
                "Found " + antiPatternCount + " anti-patterns. Refactoring recommended.",
                "Apply Extract Method, move magic numbers to constants",
                8
            ));
        }
        
        // Architecture suggestions
        boolean hasSingleton = matches.stream().anyMatch(m -> m.patternName.equals("SINGLETON"));
        boolean hasFactory = matches.stream().anyMatch(m -> m.patternName.equals("FACTORY"));
        
        if (hasSingleton && !hasFactory) {
            suggestions.add(new SmartSuggestion(
                "MEDIUM",
                "Consider Factory Pattern",
                "Using Singleton. Consider adding Factory for object creation flexibility.",
                "Implement Factory pattern alongside Singleton",
                3
            ));
        }
        
        return suggestions;
    }
    
    // ==================== Inner Classes ====================
    
    static class CodePattern {
        String name;
        String description;
        String regex;
        String suggestion;
        
        CodePattern(String name, String description, String regex, String suggestion) {
            this.name = name;
            this.description = description;
            this.regex = regex;
            this.suggestion = suggestion;
        }
    }
    
    public static class PatternMatch {
        public String category;
        public String patternName;
        public String description;
        public String suggestion;
        public String filePath;
        public int lineNumber;
        public String snippet;
        
        public PatternMatch(String category, String patternName, String description, 
                           String suggestion, String filePath, int lineNumber, String snippet) {
            this.category = category;
            this.patternName = patternName;
            this.description = description;
            this.suggestion = suggestion;
            this.filePath = filePath;
            this.lineNumber = lineNumber;
            this.snippet = snippet;
        }
    }
    
    public static class SmartSuggestion {
        public String priority;
        public String title;
        public String description;
        public String action;
        public int estimatedHours;
        
        public SmartSuggestion(String priority, String title, String description, 
                              String action, int estimatedHours) {
            this.priority = priority;
            this.title = title;
            this.description = description;
            this.action = action;
            this.estimatedHours = estimatedHours;
        }
    }
}
