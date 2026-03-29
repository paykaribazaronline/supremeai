package org.example.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CodeGenerationOrchestrator: AI-powered code generation engine
 * 
 * Orchestrates intelligent code generation using AI:
 * - Generates components, services, models with proper structure
 * - Framework-aware generation (React, Node, Flutter, Python, Java)
 * - Smart prompting to maximize code quality
 * - Validates and self-heals generated code
 * - Maintains consistency across generated files
 */
@Service
public class CodeGenerationOrchestrator {

    @Autowired
    private AIAPIService aiApiService;

    @Autowired
    private FileOrchestrator fileOrchestrator;

    @Autowired
    private CodeValidationService validationService;

    @Autowired
    private ErrorFixingSuggestor fixingSuggestor;

    /**
     * Generate a complete component for React/TypeScript
     */
    public Map<String, Object> generateReactComponent(String projectId, String componentName, 
                                                     String description, List<String> features) {
        Map<String, Object> result = new HashMap<>();
        result.put("projectId", projectId);
        result.put("componentName", componentName);
        result.put("framework", "REACT");
        result.put("status", "generating");

        try {
            // Generate TypeScript component
            String prompt = buildReactComponentPrompt(componentName, description, features);
            String componentCode = aiApiService.callAI("BUILDER", prompt, 
                Arrays.asList("GROQ", "DEEPSEEK", "CLAUDE", "GPT4"));
            
            // Generate hook if needed
            String hookCode = null;
            if (features.contains("state-management")) {
                String hookPrompt = buildReactHookPrompt(componentName, features);
                hookCode = aiApiService.callAI("BUILDER", hookPrompt, 
                    Arrays.asList("GROQ", "DEEPSEEK", "CLAUDE", "GPT4"));
            }

            // Generate styles
            String styleCode = buildReactStylesTemplate(componentName);

            // Write files
            String componentPath = "src/components/" + componentName + ".tsx";
            fileOrchestrator.writeFile(projectId, componentPath, componentCode);
            result.put("componentFile", componentPath);

            if (hookCode != null) {
                String hookPath = "src/hooks/use" + capitalize(componentName) + ".ts";
                fileOrchestrator.writeFile(projectId, hookPath, hookCode);
                result.put("hookFile", hookPath);
            }

            String stylePath = "src/components/" + componentName + ".module.css";
            fileOrchestrator.writeFile(projectId, stylePath, styleCode);
            result.put("styleFile", stylePath);

            result.put("status", "generated");
            result.put("filesGenerated", 2 + (hookCode != null ? 1 : 0));

        } catch (Exception e) {
            result.put("status", "failed");
            result.put("error", e.getMessage());
        }

        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    /**
     * Generate a complete service for Node.js/Express
     */
    public Map<String, Object> generateNodeService(String projectId, String serviceName, 
                                                   String description, List<String> methods) {
        Map<String, Object> result = new HashMap<>();
        result.put("projectId", projectId);
        result.put("serviceName", serviceName);
        result.put("framework", "NODEJS");
        result.put("status", "generating");

        try {
            // Generate service class
            String servicePrompt = buildNodeServicePrompt(serviceName, description, methods);
            String serviceCode = aiApiService.callAI("BUILDER", servicePrompt, 
                Arrays.asList("GROQ", "DEEPSEEK", "CLAUDE", "GPT4"));

            // Generate routes
            String routePrompt = buildNodeRoutePrompt(serviceName, methods);
            String routeCode = aiApiService.callAI("BUILDER", routePrompt, 
                Arrays.asList("GROQ", "DEEPSEEK", "CLAUDE", "GPT4"));

            // Generate tests
            String testPrompt = buildNodeTestPrompt(serviceName, methods);
            String testCode = aiApiService.callAI("REVIEWER", testPrompt, 
                Arrays.asList("GROQ", "DEEPSEEK", "CLAUDE", "GPT4"));

            // Write files
            String servicePath = "src/services/" + serviceName + ".ts";
            fileOrchestrator.writeFile(projectId, servicePath, serviceCode);
            result.put("serviceFile", servicePath);

            String routePath = "src/routes/" + serviceName.toLowerCase() + ".routes.ts";
            fileOrchestrator.writeFile(projectId, routePath, routeCode);
            result.put("routeFile", routePath);

            String testPath = "src/__tests__/" + serviceName + ".test.ts";
            fileOrchestrator.writeFile(projectId, testPath, testCode);
            result.put("testFile", testPath);

            result.put("status", "generated");
            result.put("filesGenerated", 3);

        } catch (Exception e) {
            result.put("status", "failed");
            result.put("error", e.getMessage());
        }

        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    /**
     * Generate model/entity classes
     */
    public Map<String, Object> generateModel(String projectId, String modelName, String framework,
                                            Map<String, String> fields, List<String> relations) {
        Map<String, Object> result = new HashMap<>();
        result.put("projectId", projectId);
        result.put("modelName", modelName);
        result.put("framework", framework);

        try {
            String modelPrompt = buildModelPrompt(modelName, framework, fields, relations);
            String language = getLanguageForFramework(framework);
            String modelCode = aiApiService.callAI("BUILDER", modelPrompt, 
                Arrays.asList("GROQ", "DEEPSEEK", "CLAUDE", "GPT4"));

            String modelPath = getModelPath(framework, modelName);
            fileOrchestrator.writeFile(projectId, modelPath, modelCode);

            result.put("status", "generated");
            result.put("modelFile", modelPath);

        } catch (Exception e) {
            result.put("status", "failed");
            result.put("error", e.getMessage());
        }

        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    /**
     * Generate and validate complete module
     */
    public Map<String, Object> generateAndValidateModule(String projectId, String moduleName, 
                                                        String framework, String description) {
        Map<String, Object> result = new HashMap<>();
        result.put("moduleName", moduleName);
        result.put("framework", framework);
        result.put("status", "in-progress");

        try {
            // Generate based on framework
            Map<String, Object> genResult = switch (framework.toUpperCase()) {
                case "REACT" -> generateReactComponent(projectId, moduleName, description, new ArrayList<>());
                case "NODEJS" -> generateNodeService(projectId, moduleName, description, new ArrayList<>());
                default -> new HashMap<>();
            };

            result.putAll(genResult);

            // Validate generated code
            Map<String, Object> validation = validationService.validateProject(projectId, framework);
            result.put("validation", validation);

            // If validation fails, attempt auto-fix
            if (!(Boolean) validation.get("isValid")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> issues = (List<Map<String, Object>>) validation.get("issues");
                Map<String, Object> fixResult = fixingSuggestor.applyFixes(projectId, framework, issues);
                result.put("fixingAttempt", fixResult);
            }

            result.put("status", "complete");

        } catch (Exception e) {
            result.put("status", "failed");
            result.put("error", e.getMessage());
        }

        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    /**
     * Generate utility/helper functions
     */
    public Map<String, Object> generateUtility(String projectId, String utilityName, 
                                               String framework, String purpose) {
        Map<String, Object> result = new HashMap<>();
        result.put("projectId", projectId);
        result.put("utilityName", utilityName);
        result.put("framework", framework);

        try {
            String prompt = buildUtilityPrompt(utilityName, framework, purpose);
            String language = getLanguageForFramework(framework);
            String utilityCode = aiApiService.callAI("BUILDER", prompt, 
                Arrays.asList("GROQ", "DEEPSEEK", "CLAUDE", "GPT4"));

            String utilPath = getUtilityPath(framework, utilityName);
            fileOrchestrator.writeFile(projectId, utilPath, utilityCode);

            result.put("status", "generated");
            result.put("utilityFile", utilPath);

        } catch (Exception e) {
            result.put("status", "failed");
            result.put("error", e.getMessage());
        }

        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    /**
     * Batch generate multiple components
     */
    public Map<String, Object> generateBatch(String projectId, String framework,
                                            List<Map<String, String>> components) {
        Map<String, Object> result = new HashMap<>();
        result.put("projectId", projectId);
        result.put("framework", framework);
        result.put("totalComponents", components.size());

        List<Map<String, Object>> generated = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (Map<String, String> component : components) {
            try {
                String name = component.get("name");
                String description = component.get("description");
                String type = component.getOrDefault("type", "component");

                Map<String, Object> genResult = switch (type.toLowerCase()) {
                    case "service" -> generateNodeService(projectId, name, description, new ArrayList<>());
                    case "model" -> generateModel(projectId, name, framework, new HashMap<>(), new ArrayList<>());
                    case "utility" -> generateUtility(projectId, name, framework, description);
                    default -> generateReactComponent(projectId, name, description, new ArrayList<>());
                };

                generated.add(genResult);
                if ("generated".equals(genResult.get("status"))) {
                    successCount++;
                } else {
                    failureCount++;
                }
            } catch (Exception e) {
                failureCount++;
            }
        }

        result.put("successCount", successCount);
        result.put("failureCount", failureCount);
        result.put("successRate", components.isEmpty() ? 0 : (successCount * 100 / components.size()));
        result.put("generated", generated);
        result.put("timestamp", System.currentTimeMillis());

        return result;
    }

    /**
     * Get generation statistics and capabilities
     */
    public Map<String, Object> getGenerationStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("supportedFrameworks", Arrays.asList("REACT", "NODEJS", "FLUTTER", "PYTHON", "JAVA"));
        stats.put("componentTypes", Arrays.asList("component", "service", "model", "utility", "controller"));
        stats.put("maxBatchSize", 50);
        stats.put("estimatedGenerationTime", 2000);  // ms per component
        stats.put("supportedLanguages", Arrays.asList("typescript", "javascript", "python", "java", "dart"));
        stats.put("timestamp", System.currentTimeMillis());
        return stats;
    }

    // ==================== Prompt Building Methods ====================

    private String buildReactComponentPrompt(String componentName, String description, List<String> features) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a React TypeScript component with the following specifications:\n\n");
        prompt.append("Component Name: ").append(componentName).append("\n");
        prompt.append("Description: ").append(description).append("\n");
        prompt.append("Features: ").append(String.join(", ", features)).append("\n\n");
        prompt.append("Requirements:\n");
        prompt.append("- Use React 18+ with TypeScript 5+\n");
        prompt.append("- Use functional components with hooks\n");
        prompt.append("- Include proper TypeScript interfaces/types\n");
        prompt.append("- Add JSDoc comments\n");
        prompt.append("- Export the component as default\n");
        prompt.append("- Handle error states gracefully\n");
        prompt.append("- Use proper TypeScript strict mode\n\n");
        prompt.append("Return ONLY the complete, production-ready component code.");
        return prompt.toString();
    }

    private String buildReactHookPrompt(String componentName, List<String> features) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a custom React hook for ").append(componentName).append(" that handles:\n");
        prompt.append(String.join(", ", features)).append("\n\n");
        prompt.append("Requirements:\n");
        prompt.append("- Use TypeScript with proper types\n");
        prompt.append("- Include error handling\n");
        prompt.append("- Use useEffect and useCallback appropriately\n");
        prompt.append("- Return proper hook interface\n");
        prompt.append("- Follow React best practices\n\n");
        prompt.append("Return ONLY the hook code.");
        return prompt.toString();
    }

    private String buildNodeServicePrompt(String serviceName, String description, List<String> methods) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a TypeScript Express service class:\n\n");
        prompt.append("Service: ").append(serviceName).append("\n");
        prompt.append("Purpose: ").append(description).append("\n");
        prompt.append("Methods to implement: ").append(String.join(", ", methods)).append("\n\n");
        prompt.append("Requirements:\n");
        prompt.append("- Use class-based design\n");
        prompt.append("- TypeScript with strict typing\n");
        prompt.append("- Async/await for database operations\n");
        prompt.append("- Error handling and logging\n");
        prompt.append("- JSDoc for all methods\n\n");
        prompt.append("Return ONLY the service class code.");
        return prompt.toString();
    }

    private String buildNodeRoutePrompt(String serviceName, List<String> methods) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate Express routes for ").append(serviceName).append(" service:\n");
        prompt.append("Methods: ").append(String.join(", ", methods)).append("\n\n");
        prompt.append("Requirements:\n");
        prompt.append("- Use Express Router\n");
        prompt.append("- Implement RESTful endpoints\n");
        prompt.append("- Error handling middleware\n");
        prompt.append("- Request validation\n");
        prompt.append("- Proper HTTP status codes\n\n");
        prompt.append("Return ONLY the route handlers code.");
        return prompt.toString();
    }

    private String buildNodeTestPrompt(String serviceName, List<String> methods) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate Jest test suite for ").append(serviceName).append(":\n");
        prompt.append("Methods to test: ").append(String.join(", ", methods)).append("\n\n");
        prompt.append("Requirements:\n");
        prompt.append("- Use Jest testing framework\n");
        prompt.append("- Mock external dependencies\n");
        prompt.append("- Test both success and error paths\n");
        prompt.append("- Code coverage > 80%\n");
        prompt.append("- Clear test descriptions\n\n");
        prompt.append("Return ONLY the test file code.");
        return prompt.toString();
    }

    private String buildModelPrompt(String modelName, String framework, Map<String, String> fields, 
                                   List<String> relations) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a ").append(framework).append(" model for ").append(modelName).append(":\n\n");
        prompt.append("Fields:\n");
        fields.forEach((name, type) -> prompt.append("- ").append(name).append(": ").append(type).append("\n"));
        prompt.append("\nRelations: ").append(String.join(", ", relations)).append("\n\n");
        prompt.append("Return ONLY the model/entity class code.");
        return prompt.toString();
    }

    private String buildUtilityPrompt(String utilityName, String framework, String purpose) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a utility module '").append(utilityName).append("' for ").append(framework).append("\n");
        prompt.append("Purpose: ").append(purpose).append("\n\n");
        prompt.append("Requirements:\n");
        prompt.append("- General-purpose utilities and helpers\n");
        prompt.append("- Well-documented\n");
        prompt.append("- Reusable across the project\n");
        prompt.append("- Error handling\n\n");
        prompt.append("Return ONLY the utility code with exported functions.");
        return prompt.toString();
    }

    // ==================== Template Methods ====================

    private String buildReactStylesTemplate(String componentName) {
        return String.format(
            ".%s {\n" +
            "  /* Component styles */\n" +
            "}\n\n" +
            ".%sContainer {\n" +
            "  display: flex;\n" +
            "  flex-direction: column;\n" +
            "}\n", 
            componentName, componentName
        );
    }

    // ==================== Helper Methods ====================

    private String getLanguageForFramework(String framework) {
        return switch (framework.toUpperCase()) {
            case "REACT", "NODEJS" -> "typescript";
            case "FLUTTER" -> "dart";
            case "PYTHON" -> "python";
            case "JAVA" -> "java";
            default -> "typescript";
        };
    }

    private String getModelPath(String framework, String modelName) {
        return switch (framework.toUpperCase()) {
            case "REACT" -> "src/types/" + modelName + ".ts";
            case "NODEJS" -> "src/models/" + modelName + ".ts";
            case "FLUTTER" -> "lib/models/" + modelName + ".dart";
            case "PYTHON" -> "app/models/" + modelName + ".py";
            case "JAVA" -> "src/main/java/com/example/model/" + modelName + ".java";
            default -> "src/" + modelName.toLowerCase();
        };
    }

    private String getUtilityPath(String framework, String utilityName) {
        return switch (framework.toUpperCase()) {
            case "REACT" -> "src/utils/" + utilityName + ".ts";
            case "NODEJS" -> "src/utils/" + utilityName + ".ts";
            case "FLUTTER" -> "lib/utils/" + utilityName + ".dart";
            case "PYTHON" -> "app/utils/" + utilityName + ".py";
            case "JAVA" -> "src/main/java/com/example/util/" + utilityName + ".java";
            default -> "src/utils/" + utilityName;
        };
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
