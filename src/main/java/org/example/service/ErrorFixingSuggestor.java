package org.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ErrorFixingSuggestor: Auto-repair service for common code generation errors
 * 
 * Automatically fixes issues detected by CodeValidationService:
 * - Missing imports and dependencies
 * - Missing configuration files
 * - Syntax errors (colons, braces, etc.)
 * - File structure issues
 * - Configuration issues
 */
@Service
public class ErrorFixingSuggestor {

    @Autowired
    private FileOrchestrator fileOrchestrator;

    @Autowired
    private TemplateManager templateManager;

    /**
     * Constructor for dependency injection (especially for testing)
     */
    public ErrorFixingSuggestor() {
    }

    public ErrorFixingSuggestor(FileOrchestrator fileOrchestrator, TemplateManager templateManager) {
        this.fileOrchestrator = fileOrchestrator;
        this.templateManager = templateManager;
    }

    /**
     * Suggest fixes for validation issues
     */
    public Map<String, Object> suggestFixes(String projectId, String templateType, 
                                           List<Map<String, Object>> issues) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> suggestions = new ArrayList<>();
        List<String> fixable = new ArrayList<>();
        List<String> manual = new ArrayList<>();

        for (Map<String, Object> issue : issues) {
            String code = (String) issue.get("code");
            String message = (String) issue.get("message");
            String file = (String) issue.get("file");

            Map<String, Object> suggestion = buildSuggestion(code, message, file, templateType);
            suggestions.add(suggestion);

            // Categorize as auto-fixable or manual
            if (isAutoFixable(code)) {
                fixable.add(code + ": " + message);
            } else {
                manual.add(code + ": " + message);
            }
        }

        result.put("projectId", projectId);
        result.put("totalIssues", issues.size());
        result.put("autoFixableCount", fixable.size());
        result.put("manualFixCount", manual.size());
        result.put("fixabilityRate", issues.isEmpty() ? 0 : (fixable.size() * 100 / issues.size()));
        result.put("suggestions", suggestions);
        result.put("autoFixable", fixable);
        result.put("requiresManualFix", manual);
        result.put("timestamp", System.currentTimeMillis());

        return result;
    }

    /**
     * Apply auto-fixes to project files
     */
    public Map<String, Object> applyFixes(String projectId, String templateType, 
                                         List<Map<String, Object>> issues) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> appliedFixes = new ArrayList<>();
        List<String> failedFixes = new ArrayList<>();

        for (Map<String, Object> issue : issues) {
            String code = (String) issue.get("code");
            String file = (String) issue.get("file");
            String message = (String) issue.get("message");

            if (!isAutoFixable(code)) {
                continue;
            }

            try {
                Map<String, Object> fixResult = applyFixForIssue(projectId, code, file, message, templateType);
                if ((Boolean) fixResult.getOrDefault("success", false)) {
                    appliedFixes.add(fixResult);
                } else {
                    failedFixes.add(code + " on " + file);
                }
            } catch (Exception e) {
                failedFixes.add(code + " on " + file + ": " + e.getMessage());
            }
        }

        result.put("projectId", projectId);
        result.put("appliedCount", appliedFixes.size());
        result.put("failedCount", failedFixes.size());
        result.put("successRate", appliedFixes.isEmpty() && failedFixes.isEmpty() ? 0 :
                (appliedFixes.size() * 100 / (appliedFixes.size() + failedFixes.size())));
        result.put("appliedFixes", appliedFixes);
        result.put("failedFixes", failedFixes);
        result.put("timestamp", System.currentTimeMillis());

        return result;
    }

    /**
     * Apply specific fix based on error code
     */
    private Map<String, Object> applyFixForIssue(String projectId, String code, String file, 
                                                 String message, String templateType) throws Exception {
        Map<String, Object> fix = new HashMap<>();
        fix.put("code", code);
        fix.put("file", file);
        fix.put("success", false);

        switch (code) {
            case "MISSING_DEPENDENCY":
                fix = fixMissingDependency(projectId, templateType, file, message);
                break;
            case "MISSING_SPRING_BOOT":
                fix = addSpringBootDependency(projectId);
                break;
            case "MISSING_MAIN":
                fix = addMainEntry(projectId, templateType, file);
                break;
            case "MISSING_IMPORTS":
                fix = addMissingImports(projectId, file, templateType);
                break;
            case "MISSING_VERSION":
                fix = addVersionField(projectId, file, templateType);
                break;
            case "INDENTATION_ERROR":
                fix = fixPythonIndentation(projectId, file);
                break;
            case "SYNTAX_ERROR":
                fix = fixSyntaxError(projectId, file, templateType);
                break;
            case "MISSING_FLUTTER_SDK":
                fix = addFlutterSdk(projectId);
                break;
            case "UNMATCHED_BRACES":
                fix = fixUnmatchedBraces(projectId, file);
                break;
            case "INVALID_JSON":
                fix = fixInvalidJson(projectId, file);
                break;
            default:
                fix.put("suggestion", "Manual review required for: " + code);
        }

        return fix;
    }

    // ==================== Fix Implementations ====================

    private Map<String, Object> fixMissingDependency(String projectId, String templateType, 
                                                     String file, String message) throws Exception {
        Map<String, Object> fix = new HashMap<>();
        fix.put("code", "MISSING_DEPENDENCY");
        fix.put("file", file);

        String dependency = extractDependencyName(message);
        if (dependency.isEmpty()) {
            return fix;
        }

        if ("package.json".equals(file)) {
            String content = fileOrchestrator.readFile(projectId, "package.json");
            String updated = content;

            if (templateType.toUpperCase().equals("REACT")) {
                if (message.contains("react") && !content.contains("\"react\":")) {
                    updated = updated.replace("\"react-dom\":", "\"react\": \"^18.0.0\",\n    \"react-dom\":");
                }
            } else if (templateType.toUpperCase().equals("NODEJS")) {
                if (message.contains("express") && !content.contains("\"express\":")) {
                    updated = updated.replace("\"dependencies\"", "\"express\": \"^4.18.0\",\n    \"cors\": \"^2.8.5\",\n    \"dotenv\": \"^16.0.0\"\n  },\n  \"dependencies\"");
                }
            }

            if (!updated.equals(content)) {
                fileOrchestrator.writeFile(projectId, "package.json", updated);
                fix.put("success", true);
                fix.put("action", "Added dependency: " + dependency);
                fix.put("before", content.substring(0, Math.min(100, content.length())));
                fix.put("after", updated.substring(0, Math.min(100, updated.length())));
            }
        }

        return fix;
    }

    private Map<String, Object> addSpringBootDependency(String projectId) throws Exception {
        Map<String, Object> fix = new HashMap<>();
        fix.put("code", "MISSING_SPRING_BOOT");

        if (!fileOrchestrator.fileExists(projectId, "pom.xml")) {
            return fix;
        }

        String content = fileOrchestrator.readFile(projectId, "pom.xml");
        if (content.contains("spring-boot")) {
            fix.put("success", true);
            fix.put("action", "Spring Boot already configured");
            return fix;
        }

        String updated = content.replace("</dependencies>", 
            "    <dependency>\n" +
            "      <groupId>org.springframework.boot</groupId>\n" +
            "      <artifactId>spring-boot-starter-web</artifactId>\n" +
            "      <version>3.1.0</version>\n" +
            "    </dependency>\n" +
            "  </dependencies>");

        fileOrchestrator.writeFile(projectId, "pom.xml", updated);
        fix.put("success", true);
        fix.put("action", "Added Spring Boot dependencies to pom.xml");
        fix.put("file", "pom.xml");

        return fix;
    }

    private Map<String, Object> addMainEntry(String projectId, String templateType, 
                                             String file) throws Exception {
        Map<String, Object> fix = new HashMap<>();
        fix.put("code", "MISSING_MAIN");
        fix.put("file", file);

        if ("package.json".equals(file)) {
            String content = fileOrchestrator.readFile(projectId, "package.json");
            if (!content.contains("\"main\"")) {
                String updated = content.replace("\"name\":", 
                    "\"main\": \"index.js\",\n  \"name\":");
                fileOrchestrator.writeFile(projectId, "package.json", updated);
                fix.put("success", true);
                fix.put("action", "Added main entry point");
            }
        } else if ("lib/main.dart".equals(file)) {
            String dartMain = "import 'package:flutter/material.dart';\n\n" +
                            "void main() {\n" +
                            "  runApp(const MainApp());\n" +
                            "}\n\n" +
                            "class MainApp extends StatelessWidget {\n" +
                            "  const MainApp({Key? key}) : super(key: key);\n\n" +
                            "  @Override\n" +
                            "  Widget build(BuildContext context) {\n" +
                            "    return MaterialApp(\n" +
                            "      home: Scaffold(\n" +
                            "        appBar: AppBar(title: const Text('SupremeAI')),\n" +
                            "        body: const Center(child: Text('Hello World')),\n" +
                            "      ),\n" +
                            "    );\n" +
                            "  }\n" +
                            "}";
            
            fileOrchestrator.writeFile(projectId, file, dartMain);
            fix.put("success", true);
            fix.put("action", "Created main.dart with Flutter app");
        }

        return fix;
    }

    private Map<String, Object> addMissingImports(String projectId, String file, 
                                                  String templateType) throws Exception {
        Map<String, Object> fix = new HashMap<>();
        fix.put("code", "MISSING_IMPORTS");
        fix.put("file", file);

        if (templateType.toUpperCase().equals("JAVA")) {
            String content = fileOrchestrator.readFile(projectId, file);
            String updated = content;

            if (content.contains("@") && !content.contains("import org.springframework")) {
                updated = "import org.springframework.stereotype.Service;\n" +
                         "import org.springframework.beans.factory.annotation.Autowired;\n\n" + 
                         content;
                
                fileOrchestrator.writeFile(projectId, file, updated);
                fix.put("success", true);
                fix.put("action", "Added Spring annotation imports");
            }
        }

        return fix;
    }

    private Map<String, Object> addVersionField(String projectId, String file, 
                                               String templateType) throws Exception {
        Map<String, Object> fix = new HashMap<>();
        fix.put("code", "MISSING_VERSION");
        fix.put("file", file);

        if ("pubspec.yaml".equals(file)) {
            String content = fileOrchestrator.readFile(projectId, "pubspec.yaml");
            if (!content.contains("version:")) {
                String updated = content.replace("name:", "version: 1.0.0\n\nname:");
                fileOrchestrator.writeFile(projectId, "pubspec.yaml", updated);
                fix.put("success", true);
                fix.put("action", "Added version field: 1.0.0");
            }
        } else if ("pom.xml".equals(file)) {
            String content = fileOrchestrator.readFile(projectId, "pom.xml");
            if (!content.contains("<version>")) {
                String updated = content.replace("<artifactId>", 
                    "<version>1.0.0-SNAPSHOT</version>\n  <artifactId>");
                fileOrchestrator.writeFile(projectId, "pom.xml", updated);
                fix.put("success", true);
                fix.put("action", "Added version field: 1.0.0-SNAPSHOT");
            }
        }

        return fix;
    }

    private Map<String, Object> fixPythonIndentation(String projectId, String file) throws Exception {
        Map<String, Object> fix = new HashMap<>();
        fix.put("code", "INDENTATION_ERROR");
        fix.put("file", file);

        String content = fileOrchestrator.readFile(projectId, file);
        String updated = normalizeIndentation(content);

        if (!updated.equals(content)) {
            fileOrchestrator.writeFile(projectId, file, updated);
            fix.put("success", true);
            fix.put("action", "Normalized Python indentation to 4 spaces");
        }

        return fix;
    }

    private Map<String, Object> fixSyntaxError(String projectId, String file, 
                                              String templateType) throws Exception {
        Map<String, Object> fix = new HashMap<>();
        fix.put("code", "SYNTAX_ERROR");
        fix.put("file", file);

        String content = fileOrchestrator.readFile(projectId, file);
        String updated = content;

        // Fix missing colons in Python
        if (file.endsWith(".py")) {
            updated = updated.replaceAll("(if |for |while |def |class )(.+)([\\s]*?)\\n", "$1$2:\\n");
        }

        if (!updated.equals(content)) {
            fileOrchestrator.writeFile(projectId, file, updated);
            fix.put("success", true);
            fix.put("action", "Fixed syntax errors (missing colons, etc.)");
        }

        return fix;
    }

    private Map<String, Object> addFlutterSdk(String projectId) throws Exception {
        Map<String, Object> fix = new HashMap<>();
        fix.put("code", "MISSING_FLUTTER_SDK");

        if (!fileOrchestrator.fileExists(projectId, "pubspec.yaml")) {
            return fix;
        }

        String content = fileOrchestrator.readFile(projectId, "pubspec.yaml");
        if (!content.contains("sdk: flutter")) {
            String updated = content.replace("dependencies:", 
                "dependencies:\n  flutter:\n    sdk: flutter");
            fileOrchestrator.writeFile(projectId, "pubspec.yaml", updated);
            fix.put("success", true);
            fix.put("action", "Added Flutter SDK declaration");
        }

        return fix;
    }

    private Map<String, Object> fixUnmatchedBraces(String projectId, String file) throws Exception {
        Map<String, Object> fix = new HashMap<>();
        fix.put("code", "UNMATCHED_BRACES");
        fix.put("file", file);

        String content = fileOrchestrator.readFile(projectId, file);
        
        int openCount = countOccurrences(content, "{");
        int closeCount = countOccurrences(content, "}");

        if (openCount != closeCount) {
            // Add closing braces if missing
            if (openCount > closeCount) {
                StringBuilder updated = new StringBuilder(content);
                for (int i = 0; i < openCount - closeCount; i++) {
                    updated.append("\n}");
                }
                fileOrchestrator.writeFile(projectId, file, updated.toString());
                fix.put("success", true);
                fix.put("action", "Added " + (openCount - closeCount) + " closing braces");
            }
        }

        return fix;
    }

    private Map<String, Object> fixInvalidJson(String projectId, String file) throws Exception {
        Map<String, Object> fix = new HashMap<>();
        fix.put("code", "INVALID_JSON");
        fix.put("file", file);

        String content = fileOrchestrator.readFile(projectId, file);
        
        try {
            // Try to validate and reformat JSON
            String validated = validateAndFormatJson(content);
            if (!validated.equals(content)) {
                fileOrchestrator.writeFile(projectId, file, validated);
                fix.put("success", true);
                fix.put("action", "Fixed JSON formatting");
            }
        } catch (Exception e) {
            fix.put("success", false);
            fix.put("error", "Cannot auto-fix invalid JSON: " + e.getMessage());
        }

        return fix;
    }

    // ==================== Helper Methods ====================

    public boolean isAutoFixable(String code) {
        return switch (code) {
            case "MISSING_DEPENDENCY", "MISSING_SPRING_BOOT", "MISSING_MAIN",
                 "MISSING_IMPORTS", "MISSING_VERSION", "INDENTATION_ERROR",
                 "SYNTAX_ERROR", "MISSING_FLUTTER_SDK", "UNMATCHED_BRACES",
                 "INVALID_JSON" -> true;
            default -> false;
        };
    }

    private Map<String, Object> buildSuggestion(String code, String message, String file, 
                                               String templateType) {
        Map<String, Object> suggestion = new HashMap<>();
        suggestion.put("code", code);
        suggestion.put("file", file);
        suggestion.put("autoFixable", isAutoFixable(code));

        suggestion.put("suggestion", switch (code) {
            case "MISSING_DEPENDENCY" -> "Add missing dependency to package.json or requirements.txt";
            case "MISSING_SPRING_BOOT" -> "Add Spring Boot starter dependency to pom.xml";
            case "MISSING_MAIN" -> "Add main entry point (package.json or main.dart)";
            case "MISSING_IMPORTS" -> "Add missing import statements from Spring Framework";
            case "MISSING_VERSION" -> "Add version field to project configuration";
            case "INDENTATION_ERROR" -> "Normalize Python indentation to 4 spaces";
            case "SYNTAX_ERROR" -> "Add missing colons or fix syntax errors";
            case "MISSING_FLUTTER_SDK" -> "Add Flutter SDK declaration to pubspec.yaml";
            case "UNMATCHED_BRACES" -> "Add missing closing braces to source file";
            case "INVALID_JSON" -> "Validate and reformat JSON file";
            default -> "Manual review required: " + message;
        });

        return suggestion;
    }

    private String extractDependencyName(String message) {
        if (message.contains("react")) return "react";
        if (message.contains("express")) return "express";
        if (message.contains("flutter")) return "flutter";
        return "";
    }

    private String normalizeIndentation(String content) {
        StringBuilder result = new StringBuilder();
        for (String line : content.split("\n")) {
            if (line.trim().isEmpty()) {
                result.append("\n");
            } else {
                int indent = line.length() - line.stripLeading().length();
                int spaces = (indent / 4) * 4; // Round to nearest 4-space increment
                result.append(" ".repeat(spaces)).append(line.stripLeading()).append("\n");
            }
        }
        return result.toString();
    }

    private String validateAndFormatJson(String content) {
        // Basic validation and formatting
        content = content.trim();
        if (!content.startsWith("{") && !content.startsWith("[")) {
            throw new IllegalArgumentException("Invalid JSON: must start with { or [");
        }
        if (!content.endsWith("}") && !content.endsWith("]")) {
            throw new IllegalArgumentException("Invalid JSON: must end with } or ]");
        }
        return content;
    }

    private int countOccurrences(String text, String pattern) {
        return (text.length() - text.replace(pattern, "").length()) / pattern.length();
    }

    public Map<String, Object> getFixStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("autoFixableIssues", Arrays.asList(
            "MISSING_DEPENDENCY", "MISSING_SPRING_BOOT", "MISSING_MAIN",
            "MISSING_IMPORTS", "MISSING_VERSION", "INDENTATION_ERROR",
            "SYNTAX_ERROR", "MISSING_FLUTTER_SDK", "UNMATCHED_BRACES", "INVALID_JSON"
        ));
        stats.put("autoFixableCount", 10);
        stats.put("timestamp", System.currentTimeMillis());
        return stats;
    }
}
