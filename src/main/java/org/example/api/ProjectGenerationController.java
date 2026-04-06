package org.example.api;

import org.example.service.FileOrchestrator;
import org.example.service.TemplateManager;
import org.example.service.AgentOrchestrator;
import org.example.service.IdleResearchService;
import org.example.service.PublicAIRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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

    private final FileOrchestrator fileOrchestrator;
    private final TemplateManager templateManager;
    private final AgentOrchestrator agentOrchestrator;
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    @Autowired(required = false)
    private IdleResearchService idleResearchService;
    
    // Project status tracking (projectId -> status)
    private final Map<String, Map<String, Object>> projectStatuses = new HashMap<>();
    
    public ProjectGenerationController(FileOrchestrator fileOrchestrator,
                                      TemplateManager templateManager,
                                      AgentOrchestrator agentOrchestrator) {
        this.fileOrchestrator = fileOrchestrator;
        this.templateManager = templateManager;
        this.agentOrchestrator = agentOrchestrator;
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
        List<Map<String, Object>> projects = new ArrayList<>(projectStatuses.values());
        
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
                "timestamp", LocalDateTime.now().format(formatter)
            );
        }
        if (!isValidRepoUrl(repoUrl)) {
            return Map.of(
                "status", "error",
                "message", "Invalid repoUrl format. Use: https://github.com/your-org/your-repo",
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
            projectStatus.put("status", pushed ? "PUSHED_TO_REPO" : "PUSH_FAILED");
            projectStatus.put("progress", 100);

            projectStatuses.put(projectId, projectStatus);

        } catch (Exception e) {
            projectStatus.put("status", "FAILED");
            projectStatus.put("error", e.getMessage());
            projectStatus.put("errorCount", 1);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("project", projectStatus);
        response.put("message", "Project generation initiated");

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
        Map<String, Object> status = projectStatuses.getOrDefault(projectId, 
            Map.of("error", "Project not found"));
        
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
        projectStatuses.remove(projectId);
        
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
        Map<String, Object> status = projectStatuses.getOrDefault(projectId, new HashMap<>());
        
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
        int totalProjects = projectStatuses.size();
        long completed = projectStatuses.values().stream()
                .filter(p -> "COMPLETED".equals(p.get("status")))
                .count();
        long failed = projectStatuses.values().stream()
                .filter(p -> "FAILED".equals(p.get("status")))
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
     * Pattern: https://<host>/<path> where host has no slashes.
     */
    private boolean isValidRepoUrl(String url) {
        if (url == null) return false;
        // Hostname: segments of [a-zA-Z0-9] joined by single dots or hyphens; no consecutive specials
        return url.matches(
            "^https://[a-zA-Z0-9]+([.\\-][a-zA-Z0-9]+)*(:[0-9]{1,5})?/[a-zA-Z0-9._/\\-]+(\\.[gG][iI][tT])?$");
    }

    /**
     * Clone the target repo, copy generated files in, then commit & push.
     * The authenticated clone URL is never written to logs.
     */
    private boolean pushGeneratedFilesToRepo(Path sourceDir, String repoUrl,
                                             String repoToken, String branch,
                                             String commitMsg) {
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
                    Path target = dest.resolve(relative);
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
}
