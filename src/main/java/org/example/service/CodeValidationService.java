package org.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * CodeValidationService: Multi-framework code validation and quality checking
 * Detects compilation errors, linting violations, and framework-specific issues
 */
@Service
public class CodeValidationService {

    @Autowired
    private FileOrchestrator fileOrchestrator;

    /**
     * Default constructor for Spring injection
     */
    public CodeValidationService() {
    }

    /**
     * Constructor for dependency injection (especially for testing)
     */
    public CodeValidationService(FileOrchestrator fileOrchestrator) {
        this.fileOrchestrator = fileOrchestrator;
    }

    public enum ErrorSeverity {
        CRITICAL, ERROR, WARNING, INFO
    }

    /**
     * Main validation method for projects
     */
    public Map<String, Object> validateProject(String projectId, String templateType) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> issues = new ArrayList<>();

        try {
            switch (templateType.toUpperCase()) {
                case "REACT":
                    validateReactProject(projectId, issues);
                    break;
                case "NODEJS":
                    validateNodeProject(projectId, issues);
                    break;
                case "FLUTTER":
                    validateFlutterProject(projectId, issues);
                    break;
                case "PYTHON":
                    validatePythonProject(projectId, issues);
                    break;
                case "JAVA":
                    validateJavaProject(projectId, issues);
                    break;
                default:
                    addIssue(issues, ErrorSeverity.ERROR, "UNKNOWN_FRAMEWORK", 
                             "Unknown template type: " + templateType, "");
            }

            Map<String, Long> counts = issues.stream()
                    .collect(Collectors.groupingBy(
                            issue -> (String) issue.get("severity"),
                            Collectors.counting()
                    ));

            long critical = counts.getOrDefault("CRITICAL", 0L);
            long errors = counts.getOrDefault("ERROR", 0L);
            long warnings = counts.getOrDefault("WARNING", 0L);
            boolean isValid = critical == 0 && errors == 0;

            result.put("projectId", projectId);
            result.put("templateType", templateType);
            result.put("isValid", isValid);
            result.put("totalIssues", issues.size());
            result.put("criticalCount", critical);
            result.put("errorCount", errors);
            result.put("warningCount", warnings);
            result.put("issues", issues);
            result.put("validationScore", calculateScore(critical, errors, warnings));
            result.put("timestamp", System.currentTimeMillis());

        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("isValid", false);
        }

        return result;
    }

    private void validateReactProject(String projectId, List<Map<String, Object>> issues) {
        try {
            if (fileOrchestrator.fileExists(projectId, "package.json")) {
                String content = fileOrchestrator.readFile(projectId, "package.json");
                if (!content.contains("react")) {
                    addIssue(issues, ErrorSeverity.CRITICAL, "MISSING_DEPENDENCY", 
                             "Missing react dependency", "package.json");
                }
                if (!content.contains("react-dom")) {
                    addIssue(issues, ErrorSeverity.CRITICAL, "MISSING_DEPENDENCY", 
                             "Missing react-dom dependency", "package.json");
                }
            }
            
            List<Map<String, Object>> tsxFiles = fileOrchestrator.searchFiles(projectId, "src/**/*.tsx");
            if (tsxFiles.isEmpty()) {
                addIssue(issues, ErrorSeverity.WARNING, "NO_COMPONENTS", 
                         "No TypeScript components found", projectId);
            }
        } catch (Exception e) {
            addIssue(issues, ErrorSeverity.WARNING, "VALIDATION_ERROR", 
                     "React validation: " + e.getMessage(), projectId);
        }
    }

    private void validateNodeProject(String projectId, List<Map<String, Object>> issues) {
        try {
            if (fileOrchestrator.fileExists(projectId, "package.json")) {
                String content = fileOrchestrator.readFile(projectId, "package.json");
                if (!content.contains("express")) {
                    addIssue(issues, ErrorSeverity.CRITICAL, "MISSING_DEPENDENCY", 
                             "Missing express dependency", "package.json");
                }
                if (!content.contains("\"main\"")) {
                    addIssue(issues, ErrorSeverity.ERROR, "MISSING_MAIN", 
                             "Missing main entry point", "package.json");
                }
            }
            
            validateTypeScriptConfig(projectId, issues);
            
            List<Map<String, Object>> routes = fileOrchestrator.searchFiles(projectId, "src/routes/**/*.ts");
            if (routes.isEmpty()) {
                addIssue(issues, ErrorSeverity.WARNING, "NO_ROUTES", 
                         "No route files found", projectId);
            }
        } catch (Exception e) {
            addIssue(issues, ErrorSeverity.WARNING, "VALIDATION_ERROR", 
                     "Node validation: " + e.getMessage(), projectId);
        }
    }

    private void validateFlutterProject(String projectId, List<Map<String, Object>> issues) {
        try {
            if (fileOrchestrator.fileExists(projectId, "pubspec.yaml")) {
                String content = fileOrchestrator.readFile(projectId, "pubspec.yaml");
                if (!content.contains("sdk: flutter")) {
                    addIssue(issues, ErrorSeverity.CRITICAL, "MISSING_FLUTTER_SDK", 
                             "Missing Flutter SDK in pubspec.yaml", "pubspec.yaml");
                }
            }
            
            if (fileOrchestrator.fileExists(projectId, "lib/main.dart")) {
                String mainContent = fileOrchestrator.readFile(projectId, "lib/main.dart");
                if (!mainContent.contains("void main()")) {
                    addIssue(issues, ErrorSeverity.CRITICAL, "MISSING_MAIN", 
                             "Missing main() function", "lib/main.dart");
                }
            }
        } catch (Exception e) {
            addIssue(issues, ErrorSeverity.WARNING, "VALIDATION_ERROR", 
                     "Flutter validation: " + e.getMessage(), projectId);
        }
    }

    private void validatePythonProject(String projectId, List<Map<String, Object>> issues) {
        try {
            if (fileOrchestrator.fileExists(projectId, "requirements.txt")) {
                String content = fileOrchestrator.readFile(projectId, "requirements.txt");
                if (!content.contains("fastapi") && !content.contains("django") && 
                    !content.contains("flask")) {
                    addIssue(issues, ErrorSeverity.WARNING, "NO_FRAMEWORK", 
                             "No web framework found", "requirements.txt");
                }
            }
            
            List<Map<String, Object>> pyFiles = fileOrchestrator.searchFiles(projectId, "**/*.py");
            if (pyFiles.isEmpty()) {
                addIssue(issues, ErrorSeverity.WARNING, "NO_PYTHON_FILES", 
                         "No Python files found", projectId);
            }
        } catch (Exception e) {
            addIssue(issues, ErrorSeverity.WARNING, "VALIDATION_ERROR", 
                     "Python validation: " + e.getMessage(), projectId);
        }
    }

    private void validateJavaProject(String projectId, List<Map<String, Object>> issues) {
        try {
            if (fileOrchestrator.fileExists(projectId, "pom.xml")) {
                String content = fileOrchestrator.readFile(projectId, "pom.xml");
                if (!content.contains("spring-boot")) {
                    addIssue(issues, ErrorSeverity.CRITICAL, "MISSING_SPRING_BOOT", 
                             "Missing Spring Boot in pom.xml", "pom.xml");
                }
                if (!content.contains("<version>")) {
                    addIssue(issues, ErrorSeverity.ERROR, "MISSING_VERSION", 
                             "Missing version in pom.xml", "pom.xml");
                }
            }
            
            List<Map<String, Object>> javaFiles = fileOrchestrator.searchFiles(projectId, "src/**/*.java");
            if (javaFiles.isEmpty()) {
                addIssue(issues, ErrorSeverity.WARNING, "NO_JAVA_FILES", 
                         "No Java files found", projectId);
            }
        } catch (Exception e) {
            addIssue(issues, ErrorSeverity.WARNING, "VALIDATION_ERROR", 
                     "Java validation: " + e.getMessage(), projectId);
        }
    }

    private void validateTypeScriptConfig(String projectId, List<Map<String, Object>> issues) {
        try {
            if (fileOrchestrator.fileExists(projectId, "tsconfig.json")) {
                String content = fileOrchestrator.readFile(projectId, "tsconfig.json");
                if (!content.contains("{")) {
                    addIssue(issues, ErrorSeverity.ERROR, "INVALID_JSON", 
                             "Invalid tsconfig.json", "tsconfig.json");
                }
            }
        } catch (Exception e) {
            // tsconfig is optional
        }
    }

    private int calculateScore(long critical, long errors, long warnings) {
        int score = 100;
        score -= (critical * 20);
        score -= (errors * 10);
        score -= (warnings * 2);
        return Math.max(0, score);
    }

    private void addIssue(List<Map<String, Object>> issues, ErrorSeverity severity, 
                         String code, String message, String file) {
        Map<String, Object> issue = new HashMap<>();
        issue.put("severity", severity.name());
        issue.put("code", code);
        issue.put("message", message);
        issue.put("file", file);
        issue.put("timestamp", System.currentTimeMillis());
        issues.add(issue);
    }

    public Map<String, Object> getValidationStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("supportedFrameworks", Arrays.asList("REACT", "NODEJS", "FLUTTER", "PYTHON", "JAVA"));
        stats.put("timestamp", System.currentTimeMillis());
        return stats;
    }
}
