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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class QualityAnalysisAgent implements AnalysisAgentInterface {

    private static final Logger log = LoggerFactory.getLogger(QualityAnalysisAgent.class);

    private final QualityPatterns patterns = new QualityPatterns();
    private final AnalysisStats stats = new AnalysisStats();

    @Override
    public String getCategory() {
        return "QUALITY";
    }

    @Override
    public List<String> getSupportedExtensions() {
        return Arrays.asList(".java", ".js", ".ts", ".tsx", ".jsx", ".py", ".go", ".rb", ".php", ".cs", ".c", ".cpp", ".h", ".hpp", ".scala", ".kt", ".swift", ".m", ".mm");
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

                // Analyze different aspects
                analyzeComplexity(content, relativePath, emitter);
                analyzeCodeSmells(content, lines, relativePath, emitter);
                analyzeNamingConventions(content, relativePath, emitter);
                analyzeBestPractices(content, lines, relativePath, emitter);

                emitter.complete();
            } catch (IOException e) {
                log.error("Error scanning file {}: {}", relativePath, e.getMessage());
                emitter.error(e);
            }
        }).doOnComplete(() -> log.debug("Completed quality analysis for file: {}", relativePath))
          .doOnError(e -> log.error("Error in quality analysis for {}: {}", relativePath, e.getMessage()));
    }

    private void analyzeComplexity(String content, String relativePath, reactor.core.publisher.FluxSink<AnalysisFinding> emitter) {
        // Cyclomatic complexity (nested conditionals/loops)
        int nestingDepth = calculateMaxNestingDepth(content);
        if (nestingDepth > 5) {
            emitter.next(createFinding("HIGH", "COMPLEXITY", "Excessive nesting depth (" + nestingDepth + " levels)",
                "Refactor to reduce nesting. Consider early returns or method extraction.", relativePath, 1));
            stats.incrementFinding("HIGH");
        }

        // Method length (lines of code)
        Pattern methodPattern = patterns.getMethodPattern();
        Matcher matcher = methodPattern.matcher(content);
        while (matcher.find()) {
            int startLine = getLineNumber(content, matcher.start());
            int endLine = getLineNumber(content, matcher.end());
            int methodLength = endLine - startLine + 1;
            if (methodLength > 50) {
                emitter.next(createFinding("MEDIUM", "COMPLEXITY", "Long method (" + methodLength + " lines)",
                    "Break down into smaller methods (aim for 30 lines max).", relativePath, startLine));
                stats.incrementFinding("MEDIUM");
            }
        }
    }

    private void analyzeCodeSmells(String content, List<String> lines, String relativePath, reactor.core.publisher.FluxSink<AnalysisFinding> emitter) {
        AtomicInteger lineNum = new AtomicInteger(1);

        for (String line : lines) {
            // Duplicate code detection (simplified - consecutive identical lines)
            if (line.trim().length() > 20 && lines.stream().filter(l -> l.trim().equals(line.trim())).count() > 2) {
                emitter.next(createFinding("MEDIUM", "DUPLICATION", "Potential code duplication",
                    "Extract common code into a shared method or class.", relativePath, lineNum.get()));
                stats.incrementFinding("MEDIUM");
            }

            // Long parameter lists
            Pattern longParamPattern = Pattern.compile("\\([^)]{200,}\\)");
            if (longParamPattern.matcher(line).find()) {
                emitter.next(createFinding("LOW", "COMPLEXITY", "Long parameter list",
                    "Consider using a parameter object or builder pattern.", relativePath, lineNum.get()));
                stats.incrementFinding("LOW");
            }

            // Unused imports (simplified check)
            if (line.contains("import") && (line.contains("java.util.*") || line.contains("lodash"))) {
                emitter.next(createFinding("LOW", "MAINTAINABILITY", "Wildcard import detected",
                    "Use specific imports for better clarity and smaller bundle sizes.", relativePath, lineNum.get()));
                stats.incrementFinding("LOW");
            }

            lineNum.incrementAndGet();
        }
    }

    private void analyzeNamingConventions(String content, String relativePath, reactor.core.publisher.FluxSink<AnalysisFinding> emitter) {
        // Check for non-camelCase variables/methods
        Pattern badNamingPattern = Pattern.compile("\\b[a-z]+_[a-z]+\\b"); // snake_case in camelCase expected languages
        Matcher matcher = badNamingPattern.matcher(content);
        while (matcher.find()) {
            String badName = matcher.group();
            if (!badName.startsWith("const") && !badName.startsWith("var") && !badName.startsWith("let")) {
                emitter.next(createFinding("LOW", "NAMING", "Inconsistent naming convention: " + badName,
                    "Use camelCase for variables/methods in JavaScript/TypeScript, PascalCase for classes.", relativePath, getLineNumber(content, matcher.start())));
                stats.incrementFinding("LOW");
            }
        }

        // Check for ALL_CAPS constants (should be PascalCase or UPPER_CASE)
        Pattern allCapsPattern = Pattern.compile("\\b[A-Z]{4,}\\b");
        Matcher allCapsMatcher = allCapsPattern.matcher(content);
        while (allCapsMatcher.find()) {
            String allCapsWord = allCapsMatcher.group();
            if (!allCapsWord.matches("^[A-Z_]+$")) { // Not a proper constant
                emitter.next(createFinding("LOW", "NAMING", "Inconsistent constant naming: " + allCapsWord,
                    "Use UPPER_CASE for constants, or PascalCase for class names.", relativePath, getLineNumber(content, allCapsMatcher.start())));
                stats.incrementFinding("LOW");
            }
        }
    }

    private void analyzeBestPractices(String content, List<String> lines, String relativePath, reactor.core.publisher.FluxSink<AnalysisFinding> emitter) {
        AtomicInteger lineNum = new AtomicInteger(1);

        for (String line : lines) {
            // Magic numbers
            Pattern magicNumberPattern = Pattern.compile("\\b(?!0\\b|\\b1\\b|\\b-1\\b)\\d{2,}\\b");
            if (magicNumberPattern.matcher(line).find() && !line.contains("const") && !line.contains("final")) {
                emitter.next(createFinding("LOW", "MAINTAINABILITY", "Magic number detected",
                    "Replace with named constants for better readability.", relativePath, lineNum.get()));
                stats.incrementFinding("LOW");
            }

            // Empty catch blocks
            if (line.trim().startsWith("} catch") && lines.size() > lineNum.get() &&
                lines.get(lineNum.get()).trim().equals("}")) {
                emitter.next(createFinding("MEDIUM", "ERROR_HANDLING", "Empty catch block",
                    "Handle exceptions properly or add logging.", relativePath, lineNum.get()));
                stats.incrementFinding("MEDIUM");
            }

            // Console.log statements in production code
            if (line.contains("console.log") || line.contains("System.out.println")) {
                emitter.next(createFinding("LOW", "LOGGING", "Debug logging in production code",
                    "Remove debug statements or use proper logging framework.", relativePath, lineNum.get()));
                stats.incrementFinding("LOW");
            }

            // TODO/FIXME comments
            if (line.toUpperCase().contains("TODO") || line.toUpperCase().contains("FIXME") || line.toUpperCase().contains("HACK")) {
                emitter.next(createFinding("LOW", "MAINTAINABILITY", "Technical debt marker found",
                    "Address technical debt or create issue for future improvement.", relativePath, lineNum.get()));
                stats.incrementFinding("LOW");
            }

            lineNum.incrementAndGet();
        }
    }

    private int calculateMaxNestingDepth(String content) {
        int maxDepth = 0;
        int currentDepth = 0;
        boolean inString = false;
        char stringChar = '"';

        for (char c : content.toCharArray()) {
            if ((c == '"' || c == '\'') && (currentDepth > 0 || !inString)) {
                if (!inString) {
                    inString = true;
                    stringChar = c;
                } else if (c == stringChar) {
                    inString = false;
                }
            } else if (!inString) {
                if (c == '{' || c == '(' || c == '[') {
                    currentDepth++;
                    maxDepth = Math.max(maxDepth, currentDepth);
                } else if (c == '}' || c == ')' || c == ']') {
                    currentDepth = Math.max(0, currentDepth - 1);
                }
            }
        }
        return maxDepth;
    }

    private int getLineNumber(String content, int charPosition) {
        return content.substring(0, charPosition).split("\n").length;
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

    private String truncateSnippet(String snippet) {
        if (snippet.length() > 200) return snippet.substring(0, 200) + "...";
        return snippet;
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

    private static class QualityPatterns {
        private final Pattern methodPattern;

        public QualityPatterns() {
            // Simple method detection (very basic - would need language-specific patterns for production)
            this.methodPattern = Pattern.compile("(?:public|private|protected|static|\\s+)\\s+(?:\\w+|\\w+<\\w+>)\\s+(\\w+)\\s*\\([^)]*\\)\\s*\\{");
        }

        public Pattern getMethodPattern() {
            return methodPattern;
        }
    }
}