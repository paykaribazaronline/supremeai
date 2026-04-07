package org.example.controller;

import org.example.model.ExistingProject;
import org.example.service.ExistingProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ExistingProjectController — REST API for managing external GitHub repos
 * that SupremeAI continuously improves.
 *
 * Endpoints:
 *   POST   /api/existing-projects              — Register a new repo
 *   GET    /api/existing-projects              — List all registered projects
 *   GET    /api/existing-projects/{id}         — Full project detail
 *   POST   /api/existing-projects/{id}/discuss — Chat with AI about improvement plan
 *   POST   /api/existing-projects/{id}/improve — Trigger an improvement cycle now
 *   PATCH  /api/existing-projects/{id}/toggle  — Enable / disable continuous improvement
 *   DELETE /api/existing-projects/{id}         — Remove project from tracking
 */
@RestController
@RequestMapping("/api/existing-projects")
@CrossOrigin(origins = "*")
public class ExistingProjectController {
    private static final Logger logger = LoggerFactory.getLogger(ExistingProjectController.class);

    @Autowired
    private ExistingProjectService projectService;

    /**
     * POST /api/existing-projects
     * Register a new external GitHub repository for continuous improvement.
     *
     * Body (JSON):
     * {
     *   "name":            "Guitar App",
     *   "repoUrl":         "https://github.com/user/guitar-app",
     *   "branch":          "main",
     *   "repoToken":       "ghp_...",          // optional, for private repos
     *   "improvementGoal": "Fix audio bug and improve tab editor UX"
     * }
     */
    @PostMapping
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        try {
            String repoUrl = body.get("repoUrl");
            if (repoUrl == null || repoUrl.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "repoUrl is required"));
            }
            // Strict URL validation: must be a well-formed https://github.com (or similar) URL
            // or an SSH git@ URL.  We accept only safe characters to prevent injection.
            if (!repoUrl.matches("^https://[a-zA-Z0-9.\\-]+/[a-zA-Z0-9_.\\-]+/[a-zA-Z0-9_.\\-]+(\\.git)?$")
                    && !repoUrl.matches("^git@[a-zA-Z0-9.\\-]+:[a-zA-Z0-9_.\\-]+/[a-zA-Z0-9_.\\-]+(\\.git)?$")) {
                return ResponseEntity.badRequest().body(Map.of("error",
                    "Invalid repoUrl. Use https://github.com/user/repo or git@github.com:user/repo"));
            }

            ExistingProject project = projectService.registerProject(
                    body.get("name"),
                    repoUrl,
                    body.get("branch"),
                    body.get("repoToken"),
                    body.get("improvementGoal")
            );

            logger.info("📝 Registered existing project via API: {}", project.getName());
            return ResponseEntity.ok(project.toSummaryMap());

        } catch (Exception e) {
            logger.error("❌ Register project failed: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/existing-projects
     * List all registered projects (summary, no token).
     */
    @GetMapping
    public ResponseEntity<?> list() {
        try {
            return ResponseEntity.ok(Map.of(
                "projects", projectService.listProjects(),
                "total", projectService.listProjects().size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/existing-projects/{id}
     * Full project detail including conversation and improvement history.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getProject(@PathVariable String id) {
        try {
            ExistingProject project = projectService.getProject(id);
            if (project == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Project not found: " + id));
            }
            // Return full details except the raw token
            Map<String, Object> full = project.toSummaryMap();
            full.put("conversation", project.getConversation());
            full.put("improvementHistory", project.getImprovementHistory());
            return ResponseEntity.ok(full);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/existing-projects/{id}/discuss
     * Send a message to SupremeAI about this project's improvement plan.
     *
     * Body: { "message": "Please also fix the login bug and add dark mode" }
     */
    @PostMapping("/{id}/discuss")
    public ResponseEntity<?> discuss(@PathVariable String id, @RequestBody Map<String, String> body) {
        try {
            String message = body.get("message");
            if (message == null || message.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "message is required"));
            }
            if (message.length() > 2000) {
                return ResponseEntity.badRequest().body(Map.of("error", "message too long (max 2000 chars)"));
            }
            Map<String, Object> result = projectService.discuss(id, message);
            return ResponseEntity.ok(result);
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("❌ Discuss failed for project {}: {}", id, e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/existing-projects/{id}/improve
     * Trigger an immediate improvement cycle (clone/pull → AI analyse → commit → push).
     * Returns 202 Accepted immediately; the cycle runs in the background.
     */
    @PostMapping("/{id}/improve")
    public ResponseEntity<?> triggerImprovement(@PathVariable String id) {
        try {
            // Validate project exists before queuing
            if (projectService.getProject(id) == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Project not found: " + id));
            }
            projectService.triggerImprovementAsync(id);
            return ResponseEntity.accepted().body(Map.of(
                "status", "queued",
                "projectId", id,
                "message", "Improvement cycle started in background"
            ));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("❌ Improvement trigger failed for project {}: {}", id, e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PATCH /api/existing-projects/{id}/toggle
     * Enable or disable continuous improvement.
     *
     * Body: { "enabled": true }
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> toggle(@PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            Boolean enabled = (Boolean) body.get("enabled");
            if (enabled == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "enabled (boolean) is required"));
            }
            ExistingProject project = projectService.toggleContinuous(id, enabled);
            return ResponseEntity.ok(Map.of(
                "projectId", id,
                "continuousImprovement", project.isContinuousImprovement(),
                "message", "Continuous improvement " + (enabled ? "ENABLED" : "DISABLED")
            ));
        } catch (java.util.NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/existing-projects/{id}
     * Remove a project from tracking. The local clone is not deleted.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> remove(@PathVariable String id) {
        try {
            boolean removed = projectService.removeProject(id);
            if (!removed) {
                return ResponseEntity.status(404).body(Map.of("error", "Project not found: " + id));
            }
            logger.info("🗑️ Removed existing project from tracking: {}", id);
            return ResponseEntity.ok(Map.of("status", "removed", "projectId", id));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
