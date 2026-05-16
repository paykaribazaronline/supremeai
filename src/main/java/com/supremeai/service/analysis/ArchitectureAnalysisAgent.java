package com.supremeai.service.analysis;

import com.supremeai.model.analysis.AnalysisFinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class ArchitectureAnalysisAgent implements AnalysisAgentInterface {

    private static final Logger log = LoggerFactory.getLogger(ArchitectureAnalysisAgent.class);


    private final ArchitecturePatterns patterns = new ArchitecturePatterns();
    private final AnalysisStats stats = new AnalysisStats();

    @Override
    public String getCategory() {
        return "ARCHITECTURE";
    }

    @Override
    public List<String> getSupportedExtensions() {
        return Arrays.asList(".java", ".ts", ".tsx", ".js", ".jsx", ".py", ".go", ".cs", ".scala", ".kt");
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Flux<AnalysisFinding> scanFile(File file, String relativePath) {
        return Flux.<AnalysisFinding>create(emitter -> {
            try {
                String extension = getFileExtension(file.getName());
                if (!getSupportedExtensions().contains(extension)) {
                    emitter.complete();
                    return;
                }

                List<String> lines = readFileLines(file);
                String content = String.join("\n", lines);

                // Analyze architectural concerns
                analyzeLayering(content, relativePath, emitter);
                analyzeCoupling(content, relativePath, emitter);
                analyzeDesignPatterns(content, relativePath, emitter);
                analyzeSOLIDPrinciples(content, relativePath, emitter);

                emitter.complete();
            } catch (IOException e) {
                log.error("Error scanning file {}: {}", relativePath, e.getMessage());
                emitter.error(e);
            }
        }).doOnComplete(() -> log.debug("Completed architecture analysis for file: {}", relativePath))
          .doOnError(e -> log.error("Error in architecture analysis for {}: {}", relativePath, e.getMessage()));
    }

    private void analyzeLayering(String content, String relativePath, reactor.core.publisher.FluxSink<AnalysisFinding> emitter) {
        // Check for layering violations (e.g., UI code in business logic)
        if (relativePath.contains("/service/") || relativePath.contains("/business/")) {
            // Business logic layer should not contain UI imports
            if (content.contains("import React") || content.contains("import javax.swing") ||
                content.contains("import android.") || content.contains("from flask import")) {
                emitter.next(createFinding("HIGH", "LAYERING", "UI framework import in business logic layer",
                    "Separate UI concerns from business logic. Move UI code to presentation layer.", relativePath, 1));
                stats.incrementFinding("HIGH");
            }

            // Business logic should not contain database queries directly
            Pattern dbQueryPattern = Pattern.compile("(SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP).*", Pattern.CASE_INSENSITIVE);
            if (dbQueryPattern.matcher(content).find()) {
                emitter.next(createFinding("MEDIUM", "LAYERING", "Direct database queries in business logic",
                    "Use repository pattern or data access layer. Avoid raw SQL in business logic.", relativePath, 1));
                stats.incrementFinding("MEDIUM");
            }
        }

        // Check for repository pattern violations
        if (relativePath.contains("/repository/") || relativePath.contains("/dao/")) {
            // Repository should not contain business logic
            Pattern businessLogicPattern = Pattern.compile("(if|for|while|switch)\\s*\\(.*\\)\\s*\\{");
            long conditionalStatements = businessLogicPattern.matcher(content).results().count();
            if (conditionalStatements > 10) {
                emitter.next(createFinding("MEDIUM", "LAYERING", "Complex business logic in data access layer",
                    "Move business logic to service layer. Repository should only handle data persistence.", relativePath, 1));
                stats.incrementFinding("MEDIUM");
            }
        }
    }

    private void analyzeCoupling(String content, String relativePath, reactor.core.publisher.FluxSink<AnalysisFinding> emitter) {
        // High coupling indicators
        List<String> imports = extractImports(content, relativePath);

        // Too many imports (high coupling)
        if (imports.size() > 30) {
            emitter.next(createFinding("MEDIUM", "COUPLING", "High coupling - " + imports.size() + " imports",
                "Consider breaking down into smaller, focused classes with single responsibility.", relativePath, 1));
            stats.incrementFinding("MEDIUM");
        }

        // Circular dependency indicators
        Set<String> uniquePackages = new HashSet<>();
        for (String imp : imports) {
            String packageName = extractPackageName(imp);
            if (!packageName.isEmpty()) {
                uniquePackages.add(packageName);
            }
        }

        if (uniquePackages.size() > 15) {
            emitter.next(createFinding("LOW", "COUPLING", "Class depends on " + uniquePackages.size() + " different packages",
                "Review dependencies. Consider dependency injection or facade patterns to reduce coupling.", relativePath, 1));
            stats.incrementFinding("LOW");
        }

        // God class indicators (too many responsibilities)
        Pattern methodPattern = patterns.getMethodPattern();
        long methodCount = methodPattern.matcher(content).results().count();
        if (methodCount > 20) {
            emitter.next(createFinding("MEDIUM", "COUPLING", "God class - " + methodCount + " methods in single class",
                "Split into smaller classes with single responsibility. Consider strategy or visitor patterns.", relativePath, 1));
            stats.incrementFinding("MEDIUM");
        }
    }

    private void analyzeDesignPatterns(String content, String relativePath, reactor.core.publisher.FluxSink<AnalysisFinding> emitter) {
        // Missing design patterns where they would be beneficial

        // Singleton pattern usage (check for getInstance methods)
        if (content.contains("getInstance()") && !content.contains("private static")) {
            emitter.next(createFinding("LOW", "PATTERN", "Potential singleton pattern usage without proper implementation",
                "Ensure thread-safe singleton implementation or consider dependency injection.", relativePath, 1));
            stats.incrementFinding("LOW");
        }

        // Factory pattern opportunities
        Pattern newKeywordPattern = Pattern.compile("\\snew\\s+\\w+\\(");
        long instantiationCount = newKeywordPattern.matcher(content).results().count();
        if (instantiationCount > 5 && !content.contains("Factory")) {
            emitter.next(createFinding("LOW", "PATTERN", "Multiple direct instantiations - consider factory pattern",
                "Use factory pattern for object creation to improve testability and maintainability.", relativePath, 1));
            stats.incrementFinding("LOW");
        }

        // Builder pattern opportunities
        if (content.contains("new") && content.matches("(?s).*\\w+\\([^)]{100,}\\)")) {
            emitter.next(createFinding("LOW", "PATTERN", "Long constructor parameters - consider builder pattern",
                "Use builder pattern for complex object construction to improve readability.", relativePath, 1));
            stats.incrementFinding("LOW");
        }

        // Strategy pattern opportunities
        Pattern switchStatementPattern = Pattern.compile("switch\\s*\\([^)]+\\)\\s*\\{");
        if (switchStatementPattern.matcher(content).find()) {
            emitter.next(createFinding("LOW", "PATTERN", "Switch statement - consider strategy pattern",
                "Replace switch with polymorphism using strategy pattern for better extensibility.", relativePath, 1));
            stats.incrementFinding("LOW");
        }
    }

    private void analyzeSOLIDPrinciples(String content, String relativePath, reactor.core.publisher.FluxSink<AnalysisFinding> emitter) {
        // Single Responsibility Principle violations
        Pattern methodPattern = patterns.getMethodPattern();
        long methodCount = methodPattern.matcher(content).results().count();
        int lineCount = content.split("\n").length;

        if (methodCount > 0 && lineCount / methodCount < 5) {
            emitter.next(createFinding("MEDIUM", "SOLID", "Single Responsibility Principle violation",
                "Class has too many short methods. Consider splitting into classes with single responsibility.", relativePath, 1));
            stats.incrementFinding("MEDIUM");
        }

        // Open/Closed Principle - check for instanceof or type checking
        Pattern instanceofPattern = Pattern.compile("\\sinstanceof\\s");
        Pattern typeCheckingPattern = Pattern.compile("\\.getClass\\(\\)\\s*==\\s*\\w+\\.class");
        if (instanceofPattern.matcher(content).find() || typeCheckingPattern.matcher(content).find()) {
            emitter.next(createFinding("LOW", "SOLID", "Open/Closed Principle violation - type checking",
                "Use polymorphism instead of type checking. Consider strategy or visitor patterns.", relativePath, 1));
            stats.incrementFinding("LOW");
        }

        // Interface Segregation Principle - large interfaces
        Pattern interfaceMethodPattern = Pattern.compile("interface\\s+\\w+\\s*\\{[^}]*\\}");
        Matcher interfaceMatcher = interfaceMethodPattern.matcher(content);
        while (interfaceMatcher.find()) {
            String interfaceBody = interfaceMatcher.group();
            long methodDeclarations = Pattern.compile(";\\s*$", Pattern.MULTILINE).matcher(interfaceBody).results().count();
            if (methodDeclarations > 10) {
                emitter.next(createFinding("LOW", "SOLID", "Interface Segregation Principle violation - large interface",
                    "Split large interface into smaller, focused interfaces.", relativePath, 1));
                stats.incrementFinding("LOW");
                break;
            }
        }
    }

    private List<String> extractImports(String content, String relativePath) {
        List<String> imports = new ArrayList<>();
        String[] lines = content.split("\n");

        Pattern importPattern;
        if (relativePath.endsWith(".java")) {
            importPattern = Pattern.compile("^import\\s+([^;]+);");
        } else if (relativePath.endsWith(".py")) {
            importPattern = Pattern.compile("^import\\s+([^\\n]+)|^from\\s+([^\\n]+)");
        } else if (relativePath.endsWith(".ts") || relativePath.endsWith(".tsx") || relativePath.endsWith(".js") || relativePath.endsWith(".jsx")) {
            importPattern = Pattern.compile("^import\\s+.*from\\s+['\"]([^'\"]+)['\"]");
        } else {
            return imports; // Skip other languages for now
        }

        for (String line : lines) {
            Matcher matcher = importPattern.matcher(line.trim());
            if (matcher.find()) {
                String imp = matcher.group(1);
                if (imp != null) imports.add(imp);
            }
        }

        return imports;
    }

    private String extractPackageName(String importStatement) {
        if (importStatement.contains(".")) {
            String[] parts = importStatement.split("\\.");
            if (parts.length >= 2) {
                return parts[0] + "." + parts[1]; // Get first two parts of package
            }
        }
        return importStatement;
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return (lastDot == -1) ? "" : filename.substring(lastDot).toLowerCase();
    }

    private List<String> readFileLines(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return reader.lines().collect(Collectors.toList());
        }
    }

    private AnalysisFinding createFinding(String severity, String category, String message, String suggestion, String file, int line) {
        return AnalysisFinding.builder()
            .id(UUID.randomUUID().toString())
            .jobId("")
            .severity(severity)
            .category(category)
            .file(file)
            .line(line)
            .message(message)
            .suggestion(suggestion)
            .pattern("")
            .codeSnippet("")
            .build();
    }

    public AnalysisStats getStats() { return stats; }
    public void resetStats() { stats.reset(); }

    public static class AnalysisStats {
        private final Map<String, Integer> counts = new HashMap<>();
        public AnalysisStats() {
            counts.put("CRITICAL", 0); counts.put("HIGH", 0); counts.put("MEDIUM", 0);
            counts.put("LOW", 0); counts.put("INFO", 0);
        }
        public void incrementFinding(String severity) { counts.merge(severity, 1, Integer::sum); }
        public Map<String, Integer> getCounts() { return new HashMap<>(counts); }
        public void reset() { counts.replaceAll((k, v) -> 0); }
    }

    private static class ArchitecturePatterns {
        private final Pattern methodPattern;

        public ArchitecturePatterns() {
            // Language-agnostic method pattern (simplified)
            this.methodPattern = Pattern.compile("(?:function|def|public|private|protected|func|fn)\\s+\\w+\\s*\\(");
        }

        public Pattern getMethodPattern() {
            return methodPattern;
        }
    }
}