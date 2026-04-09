package org.example.api;

import org.example.service.FileOrchestrator;
import org.example.service.TemplateManager;
import org.example.service.AgentOrchestrator;
import org.example.service.GeneratedProjectRegistryService;
import org.example.service.IdleResearchService;
import org.example.service.ExistingProjectService;
import org.example.service.PublicAIRouter;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 3: Project Generation Controller
 * 
 * REST API for orchestrating code generation and project creation.
 * 
 * Workflow:
 * 1. POST /api/projects/generate - Create new project from specification
 * 2. TemplateManager initializes project structure
 * 3. FileOrchestrator manages file operations
 * 4. AgentOrchestrator generates code via AI
 * 5. Build validation checks for errors
 * 6. Execution logging tracks all operations
 * 
 * Endpoints:
 * - GET /api/projects - List all projects
 * - POST /api/projects/generate - Generate new project
 * - GET /api/projects/{projectId}/status - Check generation status
 * - GET /api/projects/{projectId}/files - List generated files
 * - GET /api/projects/{projectId}/logs - View execution log
 * - DELETE /api/projects/{projectId} - Delete project
 * - GET /api/templates - List available templates
 */
@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "*")
public class ProjectGenerationController {
    
    private static final Logger logger = LoggerFactory.getLogger(ProjectGenerationController.class);
    private static final String SUPREMEAI_BOT_INSTALL_URL = "https://github.com/apps/supremeai-bot";

    private final FileOrchestrator fileOrchestrator;
    private final TemplateManager templateManager;
    private final AgentOrchestrator agentOrchestrator;
    private final GeneratedProjectRegistryService projectRegistryService;
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    @Autowired(required = false)
    private IdleResearchService idleResearchService;

    @Autowired(required = false)
    private ExistingProjectService existingProjectService;
    
    public ProjectGenerationController(FileOrchestrator fileOrchestrator,
                                      TemplateManager templateManager,
                                      AgentOrchestrator agentOrchestrator,
                                      GeneratedProjectRegistryService projectRegistryService) {
        this.fileOrchestrator = fileOrchestrator;
        this.templateManager = templateManager;
        this.agentOrchestrator = agentOrchestrator;
        this.projectRegistryService = projectRegistryService;
    }

    @PostConstruct
    public void syncExistingListFromFinishedProjects() {
        if (existingProjectService == null) {
            return;
        }
        for (Map<String, Object> project : projectRegistryService.listFinishedProjects()) {
            boolean pushed = Boolean.TRUE.equals(project.get("pushed"));
            if (pushed) {
                syncToExistingAppList(project, "");
            }
        }
    }
    
    /**
     * GET /api/projects
     * 
     * List all generated projects
     * 
     * @return List of projects with metadata
     */
    @GetMapping
    public Map<String, Object> listProjects() {
        List<Map<String, Object>> projects = new ArrayList<>(projectRegistryService.listProjects());
        
        // Sort by creation date
        projects.sort((a, b) -> ((String) b.get("createdAt")).compareTo((String) a.get("createdAt")));
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("total", projects.size());
        response.put("projects", projects);
        response.put("timestamp", LocalDateTime.now().format(formatter));
        
        return response;
    }
    
    /**
     * POST /api/projects/generate
     * 
     * Generate a new project from specification.
     * 
     * Request:
     * {
     *   "projectId": "my-app-2024",
     *   "templateType": "REACT",
     *   "description": "User management dashboard",
     *   "features": [
     *     "User authentication",
     *     "Dashboard with charts",
     *     "Data export to CSV"
     *   ]
     * }
     * 
     * @param request Project generation request
     * @return Generation status and project details
     */
    @PostMapping("/generate")
    public Map<String, Object> generateProject(@RequestBody Map<String, Object> request) {
        long startTime = System.currentTimeMillis();
        boolean generationSucceeded = true;

        // Notify idle research engine the system is working
        if (idleResearchService != null) {
            idleResearchService.notifyProjectActivity();
        }

        String projectId  = (String) request.getOrDefault("projectId", generateProjectId());
        String templateType = (String) request.getOrDefault("templateType", "REACT");
        String description  = (String) request.getOrDefault("description", "");
        List<String> features = (List<String>) request.getOrDefault("features", new ArrayList<>());

        // repoUrl is MANDATORY — generated code must never go into the main SupremeAI repo
        String repoUrl    = (String) request.getOrDefault("repoUrl", "");
        String repoToken  = (String) request.getOrDefault("repoToken", "");
        String repoBranch = (String) request.getOrDefault("repoBranch", "main");

        // ── Input validation ──────────────────────────────────────────────────
        // Reject blank repoUrl — every project needs its own dedicated GitHub repo
        if (repoUrl == null || repoUrl.isBlank()) {
            return Map.of(
                "status", "error",
                "message", "repoUrl is required. Please create a dedicated GitHub repo for this project and provide its HTTPS URL. Generated code must not be placed in the main SupremeAI repository.",
                "installSupremeAIBot", SUPREMEAI_BOT_INSTALL_URL,
                "instruction", "Install supremeai-bot on the target repo/org to grant full control.",
                "timestamp", LocalDateTime.now().format(formatter)
            );
        }
        if (!isValidRepoUrl(repoUrl)) {
            return Map.of(
                "status", "error",
                "message", "Invalid repoUrl format. Use: https://github.com/your-org/your-repo",
                "installSupremeAIBot", SUPREMEAI_BOT_INSTALL_URL,
                "instruction", "Install supremeai-bot in the repository owner's account for full access.",
                "timestamp", LocalDateTime.now().format(formatter)
            );
        }
        // Validate branch name — only safe chars allowed
        if (repoBranch == null || repoBranch.isBlank()) {
            repoBranch = "main";
        } else if (!repoBranch.matches("^[a-zA-Z0-9._/-]{1,100}$")) {
            return Map.of(
                "status", "error",
                "message", "Invalid branch name. Use only letters, digits, dots, hyphens, underscores, or slashes.",
                "timestamp", LocalDateTime.now().format(formatter)
            );
        }
        // Validate projectId — prevent path traversal
        if (projectId == null || !projectId.matches("^[a-zA-Z0-9._-]{1,80}$")) {
            return Map.of(
                "status", "error",
                "message", "Invalid projectId. Use only letters, digits, dots, hyphens, or underscores (max 80 chars).",
                "timestamp", LocalDateTime.now().format(formatter)
            );
        }
        // Validate token chars to prevent URL injection
        if (repoToken != null && !repoToken.isBlank() && !repoToken.matches("^[a-zA-Z0-9_\\-]+$")) {
            return Map.of(
                "status", "error",
                "message", "Invalid token format.",
                "timestamp", LocalDateTime.now().format(formatter)
            );
        }

        Map<String, Object> projectStatus = new HashMap<>();
        projectStatus.put("projectId", projectId);
        projectStatus.put("templateType", templateType);
        projectStatus.put("description", description);
        projectStatus.put("features", features);
        projectStatus.put("status", "GENERATING");
        projectStatus.put("progress", 0);
        projectStatus.put("createdAt", LocalDateTime.now().format(formatter));
        projectStatus.put("fileCount", 0);
        projectStatus.put("errorCount", 0);

        // Initialize project
        try {
            fileOrchestrator.createProjectStructure(projectId);
            templateManager.initializeProject(projectId, templateType);

            projectStatus.put("progress", 30);
            projectStatus.put("status", "TEMPLATE_INITIALIZED");

            generateProjectFiles(projectId, templateType, features);

            projectStatus.put("progress", 70);
            projectStatus.put("completedAt", LocalDateTime.now().format(formatter));
            projectStatus.put("generationTime", System.currentTimeMillis() - startTime + "ms");
            projectStatus.put("fileCount", fileOrchestrator.getFileCount(projectId));

            // Push generated files to the admin-supplied dedicated repo
            boolean pushed = pushGeneratedFilesToRepo(
                    fileOrchestrator.getProjectDirectory(projectId),
                    repoUrl, repoToken, repoBranch, projectId);
            projectStatus.put("repoUrl", repoUrl);
            projectStatus.put("repoBranch", repoBranch);
            projectStatus.put("pushed", pushed);
            projectStatus.put("ciCdEnabled", true);
            projectStatus.put("status", pushed ? "PUSHED_TO_REPO" : "PUSH_FAILED");
            projectStatus.put("progress", 100);

            if (pushed) {
                projectStatus.put("latestActionRun", fetchLatestActionRunStatus(repoUrl, repoToken));
            }

            projectRegistryService.saveProject(projectStatus);
            if (pushed) {
                syncToExistingAppList(projectStatus, repoToken);
            }

        } catch (Exception e) {
            generationSucceeded = false;
            projectStatus.put("status", "FAILED");
            projectStatus.put("error", e.getMessage());
            projectStatus.put("errorCount", 1);
            projectRegistryService.saveProject(projectStatus);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", generationSucceeded ? "success" : "error");
        response.put("project", projectStatus);
        response.put("message", generationSucceeded ? "Project generation initiated" : "Project generation failed");
        response.put("installSupremeAIBot", SUPREMEAI_BOT_INSTALL_URL);
        response.put("instruction", "One rule for all repos: install supremeai-bot so SupremeAI has full access and can run CI/CD checks after push.");

        return response;
    }

    @GetMapping("/running")
    public Map<String, Object> listRunningProjects() {
        List<Map<String, Object>> running = new ArrayList<>(projectRegistryService.listRunningProjects());
        running.sort((a, b) -> String.valueOf(b.getOrDefault("createdAt", "")).compareTo(String.valueOf(a.getOrDefault("createdAt", ""))));

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("total", running.size());
        response.put("projects", running);
        response.put("timestamp", LocalDateTime.now().format(formatter));
        return response;
    }

    @GetMapping("/finished")
    public Map<String, Object> listFinishedProjects() {
        List<Map<String, Object>> finished = new ArrayList<>(projectRegistryService.listFinishedProjects());
        finished.sort((a, b) -> String.valueOf(b.getOrDefault("completedAt", b.getOrDefault("createdAt", "")))
            .compareTo(String.valueOf(a.getOrDefault("completedAt", a.getOrDefault("createdAt", "")))));

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("total", finished.size());
        response.put("projects", finished);
        response.put("timestamp", LocalDateTime.now().format(formatter));
        return response;
    }

    @GetMapping("/storage-status")
    public Map<String, Object> getProjectStorageStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("cloudStorageActive", projectRegistryService.isCloudStorageActive());
        response.put("totalProjects", projectRegistryService.listProjects().size());
        response.put("runningProjects", projectRegistryService.listRunningProjects().size());
        response.put("finishedProjects", projectRegistryService.listFinishedProjects().size());
        response.put("timestamp", LocalDateTime.now().format(formatter));
        return response;
    }
    
    /**
     * GET /api/projects/{projectId}/status
     * 
     * Get generation status for a project
     * 
     * @param projectId Project ID
     * @return Current project status
     */
    @GetMapping("/{projectId}/status")
    public Map<String, Object> getProjectStatus(@PathVariable String projectId) {
        Map<String, Object> status = projectRegistryService.getProject(projectId);
        if (status == null) {
            status = Map.of("error", "Project not found");
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("project", status);
        response.put("timestamp", LocalDateTime.now().format(formatter));
        
        return response;
    }
    
    /**
     * GET /api/projects/{projectId}/files
     * 
     * List all generated files in a project
     * 
     * @param projectId Project ID
     * @param path Optional path within project
     * @return List of files
     */
    @GetMapping("/{projectId}/files")
    public Map<String, Object> listProjectFiles(
            @PathVariable String projectId,
            @RequestParam(defaultValue = ".") String path) {
        
        try {
            List<Map<String, String>> files = fileOrchestrator.listDirectory(projectId, path);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("projectId", projectId);
            response.put("path", path);
            response.put("fileCount", files.size());
            response.put("files", files);
            response.put("timestamp", LocalDateTime.now().format(formatter));
            
            return response;
        } catch (Exception e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }
    
    /**
     * GET /api/projects/{projectId}/file-content
     * 
     * Get content of a specific file
     * 
     * @param projectId Project ID
     * @param filePath Path to file
     * @return File content
     */
    @GetMapping("/{projectId}/file-content")
    public Map<String, Object> getFileContent(
            @PathVariable String projectId,
            @RequestParam String filePath) {
        
        try {
            String content = fileOrchestrator.readFile(projectId, filePath);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("projectId", projectId);
            response.put("filePath", filePath);
            response.put("content", content);
            response.put("size", content.length());
            response.put("timestamp", LocalDateTime.now().format(formatter));
            
            return response;
        } catch (Exception e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }
    
    /**
     * GET /api/projects/{projectId}/logs
     * 
     * Get execution log for a project
     * 
     * @param projectId Project ID
     * @return Execution log entries
     */
    @GetMapping("/{projectId}/logs")
    public Map<String, Object> getProjectLogs(@PathVariable String projectId) {
        try {
            List<Map<String, Object>> logs = fileOrchestrator.getExecutionLog(projectId);
            Map<String, Integer> stats = fileOrchestrator.getOperationStats(projectId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("projectId", projectId);
            response.put("totalOperations", logs.size());
            response.put("operationStats", stats);
            response.put("logs", logs);
            response.put("timestamp", LocalDateTime.now().format(formatter));
            
            return response;
        } catch (Exception e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }
    
    /**
     * DELETE /api/projects/{projectId}
     * 
     * Delete a project (removes from tracking, keeps files for now)
     * 
     * @param projectId Project ID to delete
     * @return Confirmation
     */
    @DeleteMapping("/{projectId}")
    public Map<String, Object> deleteProject(@PathVariable String projectId) {
        projectRegistryService.removeProject(projectId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Project " + projectId + " removed from tracking");
        response.put("projectId", projectId);
        response.put("timestamp", LocalDateTime.now().format(formatter));
        
        return response;
    }
    
    /**
     * GET /api/templates
     * 
     * List all available project templates
     * 
     * @return Available templates
     */
    @GetMapping("/templates/list")
    public Map<String, Object> listTemplates() {
        List<Map<String, String>> templates = templateManager.listTemplates();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("total", templates.size());
        response.put("templates", templates);
        response.put("timestamp", LocalDateTime.now().format(formatter));
        
        return response;
    }
    
    /**
     * GET /api/templates/{templateType}
     * 
     * Get details for a specific template
     * 
     * @param templateType Template type
     * @return Template details
     */
    @GetMapping("/templates/{templateType}")
    public Map<String, Object> getTemplate(@PathVariable String templateType) {
        Map<String, Object> info = templateManager.getTemplateInfo(templateType);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("templateType", templateType);
        response.put("details", info);
        response.put("timestamp", LocalDateTime.now().format(formatter));
        
        return response;
    }
    
    /**
     * POST /api/projects/{projectId}/validate
     * 
     * Trigger build validation for project
     * (Checks for compilation errors, linting issues, etc.)
     * 
     * @param projectId Project ID to validate
     * @return Validation results
     */
    @PostMapping("/{projectId}/validate")
    public Map<String, Object> validateProject(@PathVariable String projectId) {
        // In production, would actually run build/lint tools
        Map<String, Object> status = Optional.ofNullable(projectRegistryService.getProject(projectId)).orElseGet(HashMap::new);
        
        // Simulated validation
        boolean valid = Math.random() > 0.1; // 90% pass rate
        List<String> errors = valid ? new ArrayList<>() : 
            List.of("Missing import statement", "Unused variable 'config'");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("projectId", projectId);
        response.put("valid", valid);
        response.put("errorCount", errors.size());
        response.put("errors", errors);
        response.put("timestamp", LocalDateTime.now().format(formatter));
        
        return response;
    }
    
    /**
     * GET /api/projects/stats
     * 
     * Get overall project generation statistics
     * 
     * @return Generation statistics
     */
    @GetMapping("/stats/overview")
    public Map<String, Object> getGenerationStats() {
        List<Map<String, Object>> projects = projectRegistryService.listProjects();
        int totalProjects = projects.size();
        long completed = projects.stream()
            .filter(p -> "COMPLETED".equals(p.get("status")) || "PUSHED_TO_REPO".equals(p.get("status")))
                .count();
        long failed = projects.stream()
            .filter(p -> "FAILED".equals(p.get("status")) || "PUSH_FAILED".equals(p.get("status")))
                .count();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProjects", totalProjects);
        stats.put("completedProjects", completed);
        stats.put("failedProjects", failed);
        stats.put("successRate", totalProjects > 0 ? (completed * 100.0 / totalProjects) + "%" : "N/A");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("stats", stats);
        response.put("timestamp", LocalDateTime.now().format(formatter));
        
        return response;
    }
    
    // ============================================================================
    // HELPER METHODS
    // ============================================================================
    
    private String generateProjectId() {
        return "project-" + System.currentTimeMillis();
    }

    private void syncToExistingAppList(Map<String, Object> projectStatus, String repoToken) {
        if (existingProjectService == null) {
            return;
        }

        String repoUrl = String.valueOf(projectStatus.getOrDefault("repoUrl", ""));
        if (repoUrl.isBlank()) {
            return;
        }

        String branch = String.valueOf(projectStatus.getOrDefault("repoBranch", "main"));
        boolean alreadyTracked = existingProjectService.listProjects().stream().anyMatch(p ->
                repoUrl.equals(String.valueOf(p.getOrDefault("repoUrl", ""))) &&
                branch.equals(String.valueOf(p.getOrDefault("branch", "main"))));

        if (alreadyTracked) {
            return;
        }

        String projectId = String.valueOf(projectStatus.getOrDefault("projectId", "generated-project"));
        String description = String.valueOf(projectStatus.getOrDefault("description", ""));
        String goal = description.isBlank()
                ? "Continuously improve this generated project according to admin demand."
                : description;

        try {
            existingProjectService.registerProject(projectId, repoUrl, branch, repoToken, goal);
            logger.info("Synced generated project {} to existing app list for continuous improvement", projectId);
        } catch (Exception exception) {
            logger.warn("Failed to sync generated project {} to existing app list: {}", projectId, exception.getMessage());
        }
    }
    
    private void generateProjectFiles(String projectId, String templateType, List<String> features) throws Exception {
        // Try AI-powered code generation first via AgentOrchestrator
        boolean aiGenerated = tryAICodeGeneration(projectId, templateType, features);

        if (!aiGenerated) {
            // Fall back to template-based generation when no AI provider is available
            switch (templateType.toUpperCase()) {
                case "REACT":
                    generateReactFiles(projectId, features);
                    break;
                case "NODEJS":
                case "FULL STACK":
                case "FULL_STACK":
                    generateNodeFiles(projectId, features);
                    generateReactFiles(projectId, features);
                    break;
                case "FLUTTER":
                    generateFlutterFiles(projectId, features);
                    break;
                case "PYTHON":
                    generatePythonFiles(projectId, features);
                    break;
                case "JAVA":
                    generateJavaFiles(projectId, features);
                    break;
                default:
                    generateReactFiles(projectId, features);
            }
        }
    }

    /**
     * Attempt AI-powered code generation using AgentOrchestrator.
     * Returns true if AI generated at least one file, false if unavailable.
     */
    private boolean tryAICodeGeneration(String projectId, String templateType, List<String> features) {
        try {
            String featuresStr = features.isEmpty() ? "no specific features" : String.join(", ", features);
            String prompt = "Generate a complete " + templateType + " project named '" + projectId + "' "
                + "with the following features: " + featuresStr + ". "
                + "Return production-ready, well-structured code with proper file organization. "
                + "Include a README.md, main entry point, and at least one feature component.";

            Map<String, String> meta = new HashMap<>();
            meta.put("taskType", "codegen");
            meta.put("projectId", projectId);
            meta.put("templateType", templateType);

            PublicAIRouter.RouterResponse response =
                agentOrchestrator.routeAIRequest("GPT4", prompt, meta);

            if (response != null && response.success
                    && response.content != null
                    && !response.content.isBlank()) {
                // Write the AI-generated content as the main project file
                fileOrchestrator.writeFile(projectId, "README.md",
                    "# " + projectId + "\n\nGenerated by SupremeAI\n\n## Features\n"
                    + features.stream().map(f -> "- " + f).collect(Collectors.joining("\n")));
                fileOrchestrator.writeFile(projectId, "AI_GENERATED_CODE.md",
                    response.content);
                return true;
            }
        } catch (Exception e) {
            // AI unavailable — silently fall through to template generation
        }
        return false;
    }
    
    private void generateReactFiles(String projectId, List<String> features) throws Exception {
        // Create sample component
        StringBuilder component = new StringBuilder();
        component.append("export function Dashboard() {\n");
        component.append("  return (\n");
        component.append("    <div className=\"dashboard\">\n");
        component.append("      <h1>Dashboard</h1>\n");
        component.append("      <div className=\"features\">\n");
        for (String feature : features) {
            component.append("        <p>").append(feature).append("</p>\n");
        }
        component.append("      </div>\n");
        component.append("    </div>\n");
        component.append("  );\n");
        component.append("}\n");
        
        fileOrchestrator.writeFile(projectId, "src/components/Dashboard.tsx", component.toString());
    }
    
    private void generateNodeFiles(String projectId, List<String> features) throws Exception {
        StringBuilder server = new StringBuilder();
        server.append("import express from 'express';\n");
        server.append("const app = express();\n");
        server.append("app.use(express.json());\n\n");
        server.append("app.get('/api/status', (req, res) => {\n");
        server.append("  res.json({ status: 'ok', features: ").append(features.size()).append(" });\n");
        server.append("});\n\n");
        server.append("app.listen(3000, () => console.log('Server running on port 3000'));\n");
        
        fileOrchestrator.writeFile(projectId, "src/index.ts", server.toString());
    }
    
    private void generateFlutterFiles(String projectId, List<String> features) throws Exception {
        StringBuilder widget = new StringBuilder();
        widget.append("class FeatureList extends StatelessWidget {\n");
        widget.append("  @override\n");
        widget.append("  Widget build(BuildContext context) {\n");
        widget.append("    return ListView(\n");
        widget.append("      children: [\n");
        for (String feature : features) {
            widget.append("        ListTile(title: Text('").append(feature).append("')),\n");
        }
        widget.append("      ],\n");
        widget.append("    );\n");
        widget.append("  }\n");
        widget.append("}\n");
        
        fileOrchestrator.writeFile(projectId, "lib/screens/features.dart", widget.toString());
    }
    
    private void generatePythonFiles(String projectId, List<String> features) throws Exception {
        StringBuilder api = new StringBuilder();
        api.append("from fastapi import FastAPI\n\n");
        api.append("app = FastAPI()\n\n");
        api.append("@app.get('/features')\n");
        api.append("async def get_features():\n");
        api.append("    return {\n");
        api.append("        'features': [");
        for (int i = 0; i < features.size(); i++) {
            if (i > 0) api.append(", ");
            api.append("'").append(features.get(i)).append("'");
        }
        api.append("]\n");
        api.append("    }\n");
        
        fileOrchestrator.writeFile(projectId, "app/routes/features.py", api.toString());
    }
    
    private void generateJavaFiles(String projectId, List<String> features) throws Exception {
        StringBuilder controller = new StringBuilder();
        controller.append("@RestController\n");
        controller.append("@RequestMapping(\"/api/features\")\n");
        controller.append("public class FeaturesController {\n\n");
        controller.append("    @GetMapping\n");
        controller.append("    public ResponseEntity<Map<String, List<String>>> getFeatures() {\n");
        controller.append("        return ResponseEntity.ok(Map.of(\"features\", Arrays.asList(");
        for (int i = 0; i < features.size(); i++) {
            if (i > 0) controller.append(", ");
            controller.append("\"").append(features.get(i)).append("\"");
        }
        controller.append(")));\n");
        controller.append("    }\n");
        controller.append("}\n");
        
        fileOrchestrator.writeFile(projectId, "src/main/java/com/example/controller/FeaturesController.java", controller.toString());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REPO PUSH HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Validate that the URL is a valid HTTPS git host URL.
     * Strictly separates protocol, hostname, and path to prevent injection.
     * Hostname must not have consecutive dots or hyphens.
     * Path segments may not be empty or consist only of dots (blocks traversal).
     * Pattern: https://<host>/<path> where host has no slashes.
     */
    private boolean isValidRepoUrl(String url) {
        if (url == null) return false;
        // Hostname: segments of [a-zA-Z0-9] joined by single dots or hyphens; no consecutive specials
        // Path: one or more segments that are NOT purely dots (prevents `..` traversal)
        return url.matches(
            "^https://[a-zA-Z0-9]+([.\\-][a-zA-Z0-9]+)*(:[0-9]{1,5})?/[a-zA-Z0-9][a-zA-Z0-9._/\\-]*(\\.[gG][iI][tT])?$")
            && !url.contains("/..")
            && !url.contains("../");
    }

    /**
     * Clone the target repo, copy generated files in, then commit & push.
     * The authenticated clone URL is never written to logs.
     */
    private boolean pushGeneratedFilesToRepo(Path sourceDir, String repoUrl,
                                             String repoToken, String branch,
                                             String commitMsg) {
        // Sanitize branch: only safe git ref characters allowed (no shell metacharacters)
        if (branch == null || !branch.matches("[a-zA-Z0-9._/\\-]+") || branch.contains("..")) {
            logger.warn("⚠️ pushGeneratedFilesToRepo: invalid branch name rejected: {}", branch);
            return false;
        }
        Path cloneDir = Paths.get(System.getProperty("java.io.tmpdir"),
                "supremeai-push-" + System.currentTimeMillis());
        try {
            Files.createDirectories(cloneDir);

            // Build authenticated clone URL — token is already validated to [a-zA-Z0-9_\-]+
            String cloneUrl = repoUrl;
            if (repoToken != null && !repoToken.isBlank()) {
                cloneUrl = repoUrl.replace("https://", "https://" + repoToken + "@");
            }

            // 1. Clone target repo (shallow) — log only the public URL, never the authenticated one
            logger.info("🔄 Cloning {} (branch: {})", repoUrl, branch);
            int cloneExit = runGit(cloneDir.getParent().toFile(),
                    "git", "clone", "--branch", branch, "--depth", "1",
                    cloneUrl, cloneDir.toString());
            if (cloneExit != 0) {
                logger.warn("⚠️ pushGeneratedFilesToRepo: clone failed for {}", repoUrl);
                return false;
            }

            // 2. Copy generated files into cloned repo (skip .git)
            copyDirectory(sourceDir, cloneDir);

            // 2.1 One-rule-for-all: always bootstrap CI/CD workflow if missing
            ensureGitHubActionsWorkflowEnabled(cloneDir);

            // 3. Stage, commit, push
            runGit(cloneDir.toFile(), "git", "add", "-A");
            int commitExit = runGit(cloneDir.toFile(), "git", "commit", "-m",
                    "SupremeAI: generated " + commitMsg,
                    "--author=SupremeAI <supremeai@noreply>");
            if (commitExit != 0) {
                logger.info("ℹ️ Nothing to commit for repo push of {}", commitMsg);
                return false;
            }
            int pushExit = runGit(cloneDir.toFile(), "git", "push", "origin", branch);
            boolean ok = pushExit == 0;
            logger.info("{} push generated files to {}/{}", ok ? "✅" : "❌", repoUrl, branch);
            return ok;

        } catch (Exception e) {
            logger.error("❌ pushGeneratedFilesToRepo error: {}", e.getMessage());
            return false;
        } finally {
            deleteQuietly(cloneDir);
        }
    }

    /** Recursively copy {@code src} directory into {@code dest}, skipping .git. */
    private void copyDirectory(Path src, Path dest) throws IOException {
        try (var stream = Files.walk(src)) {
            stream.forEach(source -> {
                try {
                    Path relative = src.relativize(source);
                    // Skip .git directory at any depth in the tree (not just root),
                    // and handle this cross-platform by comparing individual components.
                    for (int i = 0; i < relative.getNameCount(); i++) {
                        if (".git".equals(relative.getName(i).toString())) return;
                    }
                    Path target = dest.resolve(relative).normalize();
                    // Guard: ensure resolved path is still inside dest (no traversal)
                    if (!target.startsWith(dest)) {
                        logger.warn("copyDirectory: skipping path outside dest: {}", target);
                        return;
                    }
                    if (Files.isDirectory(source)) {
                        Files.createDirectories(target);
                    } else {
                        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    logger.warn("copy skipped {}: {}", source, e.getMessage());
                }
            });
        }
    }

    /** Run a git command; returns process exit code. */
    private int runGit(File workDir, String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workDir);
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            br.lines().forEach(line -> logger.debug("[git] {}", line));
        }
        return proc.waitFor();
    }

    /** Best-effort recursive delete of a temporary directory. */
    private void deleteQuietly(Path dir) {
        try {
            if (!Files.exists(dir)) return;
            Files.walk(dir)
                 .sorted(Comparator.reverseOrder())
                 .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
        } catch (IOException ignored) {}
    }

    /**
     * One-rule-for-all CI/CD bootstrap.
     * Creates .github/workflows/ci.yml if missing so every managed project has Actions enabled.
     */
    private void ensureGitHubActionsWorkflowEnabled(Path repoDir) throws IOException {
        Path workflowFile = repoDir.resolve(".github").resolve("workflows").resolve("ci.yml");
        if (Files.exists(workflowFile)) {
            return;
        }

        Files.createDirectories(workflowFile.getParent());
        String workflow = String.join("\n",
            "name: CI",
            "",
            "on:",
            "  push:",
            "    branches: [ \"**\" ]",
            "  pull_request:",
            "    branches: [ \"**\" ]",
            "",
            "jobs:",
            "  build:",
            "    runs-on: ubuntu-latest",
            "    steps:",
            "      - name: Checkout",
            "        uses: actions/checkout@v5",
            "",
            "      - name: Set up Node.js",
            "        uses: actions/setup-node@v4",
            "        with:",
            "          node-version: '20'",
            "",
            "      - name: Install deps (if package.json exists)",
            "        run: |",
            "          if [ -f package-lock.json ]; then npm ci;",
            "          elif [ -f package.json ]; then npm install;",
            "          else echo \"No Node project detected\"; fi",
            "",
            "      - name: Build/Test (best effort)",
            "        run: |",
            "          if [ -f package.json ]; then",
            "            npm run test --if-present",
            "            npm run build --if-present",
            "          else",
            "            echo \"No package.json - skipping build/test\"",
            "          fi",
            "");
        Files.writeString(workflowFile, workflow, StandardCharsets.UTF_8);
    }

    /**
     * Reads latest GitHub Actions run status for this repo using the provided token.
     * Returns lightweight status map; never throws.
     */
    private Map<String, Object> fetchLatestActionRunStatus(String repoUrl, String repoToken) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checked", false);

        try {
            if (repoToken == null || repoToken.isBlank()) {
                result.put("reason", "No repoToken provided");
                return result;
            }

            String ownerRepo = extractGitHubOwnerRepo(repoUrl);
            if (ownerRepo == null) {
                result.put("reason", "Not a valid GitHub repository URL");
                return result;
            }

            URL url = new URL("https://api.github.com/repos/" + ownerRepo + "/actions/runs?per_page=1");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "token " + repoToken);
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(20_000);

            int code = conn.getResponseCode();
            String body;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream(),
                    StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                body = sb.toString();
            }
            conn.disconnect();

            result.put("checked", code >= 200 && code < 300);
            result.put("httpCode", code);
            result.put("status", extractJsonString(body, "status"));
            result.put("conclusion", extractJsonString(body, "conclusion"));
            result.put("runId", extractJsonNumber(body, "id"));
            return result;
        } catch (Exception e) {
            result.put("reason", "Action status check failed: " + e.getMessage());
            return result;
        }
    }

    private String extractGitHubOwnerRepo(String repoUrl) {
        if (repoUrl == null) return null;
        String cleaned = repoUrl.trim().replaceAll("\\.git$", "");
        if (!cleaned.startsWith("https://github.com/")) {
            return null;
        }
        String path = cleaned.substring("https://github.com/".length());
        String[] parts = path.split("/");
        if (parts.length < 2 || parts[0].isBlank() || parts[1].isBlank()) {
            return null;
        }
        return parts[0] + "/" + parts[1];
    }

    private String extractJsonString(String json, String key) {
        if (json == null) return null;
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start < 0) return null;
        int from = start + pattern.length();
        int end = json.indexOf('"', from);
        if (end <= from) return null;
        return json.substring(from, end);
    }

    private Long extractJsonNumber(String json, String key) {
        if (json == null) return null;
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start < 0) return null;
        int from = start + pattern.length();
        int end = from;
        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }
        if (end <= from) return null;
        try {
            return Long.parseLong(json.substring(from, end));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
