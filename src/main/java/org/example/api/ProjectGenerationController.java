package org.example.api;

import org.example.service.FileOrchestrator;
import org.example.service.TemplateManager;
import org.example.service.AgentOrchestrator;
import org.example.service.IdleResearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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
@CrossOrigin(origins = "http://localhost:8001")
public class ProjectGenerationController {
    
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
        
        String projectId = (String) request.getOrDefault("projectId", generateProjectId());
        String templateType = (String) request.getOrDefault("templateType", "REACT");
        String description = (String) request.getOrDefault("description", "");
        List<String> features = (List<String>) request.getOrDefault("features", new ArrayList<>());
        
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
            
            // Update progress
            projectStatus.put("progress", 30);
            projectStatus.put("status", "TEMPLATE_INITIALIZED");
            
            // Simulate code generation (in production, would use AIAPIService)
            generateProjectFiles(projectId, templateType, features);
            
            projectStatus.put("progress", 100);
            projectStatus.put("status", "COMPLETED");
            projectStatus.put("completedAt", LocalDateTime.now().format(formatter));
            projectStatus.put("generationTime", System.currentTimeMillis() - startTime + "ms");
            
            // Count files
            projectStatus.put("fileCount", fileOrchestrator.getFileCount(projectId));
            
            // Store status
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
        // Simulated code generation - in production would use AIAPIService
        // and generate actual code based on features
        
        switch (templateType.toUpperCase()) {
            case "REACT":
                generateReactFiles(projectId, features);
                break;
            case "NODEJS":
                generateNodeFiles(projectId, features);
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
        }
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
}
