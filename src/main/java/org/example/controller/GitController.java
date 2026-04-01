package org.example.controller;

import org.example.model.User;
import org.example.service.GitService;
import org.example.service.GitHubAPIService;
import org.example.service.AdminControlService;
import org.example.service.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * Git Controller
 * 
 * Endpoints for git operations:
 * - POST /api/git/commit - Commit changes
 * - POST /api/git/push - Push to remote
 * - GET /api/git/status - Get git status
 * - GET /api/git/branches - List branches
 * - GET /api/git/logs - Get commit logs
 * - POST /api/git/pr - Create pull request
 * - GET /api/github/workflow - Get workflow status
 * - GET /api/github/runs - Get workflow runs
 * - POST /api/github/issue - Create issue
 */
@RestController
@RequestMapping("/api/git")
public class GitController {
    private static final Logger logger = LoggerFactory.getLogger(GitController.class);
    
    @Autowired
    private GitService gitService;
    
    @Autowired
    private GitHubAPIService gitHubService;
    
    @Autowired
    private AdminControlService adminControlService;
    
    @Autowired
    private AuthenticationService authService;
    
    // ============ PRIVATE HELPER METHODS ============
    
    private User extractUserFromToken(String authHeader) throws Exception {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        
        String token = authHeader.substring(7);
        return authService.validateToken(token);
    }
    
    /**
     * POST /api/git/commit
     * Commit changes to git
     */
    @PostMapping("/commit")
    public ResponseEntity<?> commitChanges(
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = extractUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Authentication required"));
            }
            
            String message = request.get("message");
            if (message == null || message.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Commit message required"));
            }
            
            // Check admin control mode
            if (adminControlService.requiresApproval()) {
                // Create pending action
                adminControlService.createPendingAction(
                    org.example.model.PendingAction.ActionType.COMMIT,
                    "Commit: " + message,
                    message
                );
                
                return ResponseEntity.accepted().body(Map.of(
                    "status", "pending",
                    "message", "Commit created as pending action. Awaiting approval.",
                    "mode", adminControlService.getPermissionMode()
                ));
            }
            
            // Perform commit
            String authorName = user.getUsername() + " <" + user.getEmail() + ">";
            String commitHash = gitService.commitChanges(message, authorName);
            
            if (commitHash == null) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("status", "error", "message", "Commit failed"));
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Changes committed successfully");
            response.put("commitHash", commitHash);
            response.put("author", user.getUsername());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Commit error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    /**
     * POST /api/git/push
     * Push commits to remote
     */
    @PostMapping("/push")
    public ResponseEntity<?> pushChanges(
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = extractUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Authentication required"));
            }
            
            String branch = request.getOrDefault("branch", "main");
            
            // ✅ VALIDATE: Branch name only alphanumeric, dots, slashes, hyphens
            if (!branch.matches("^[a-zA-Z0-9._/-]+$")) {
                return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Invalid branch name: " + branch));
            }
            
            if (adminControlService.requiresApproval()) {
                adminControlService.createPendingAction(
                    org.example.model.PendingAction.ActionType.PUSH,
                    "Push to " + branch,
                    "Branch: " + branch
                );
                
                return ResponseEntity.accepted().body(Map.of(
                    "status", "pending",
                    "message", "Push created as pending action"
                ));
            }
            
            boolean success = gitService.pushToRemote(branch);
            
            if (!success) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("status", "error", "message", "Push failed"));
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Changes pushed successfully");
            response.put("branch", branch);
            response.put("by", user.getUsername());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Push error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    /**
     * GET /api/git/status
     * Get git repository status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = extractUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Authentication required"));
            }
            
            Map<String, Object> gitStatus = gitService.getStatus();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("gitStatus", gitStatus);
            response.put("adminControl", adminControlService.getStatus());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Status error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    /**
     * GET /api/git/logs
     * Get recent commits
     */
    @GetMapping("/logs")
    public ResponseEntity<?> getCommitLogs(
            @RequestParam(defaultValue = "10") int limit,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = extractUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Authentication required"));
            }
            
            List<Map<String, String>> commits = gitService.getRecentCommits(limit);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("commits", commits);
            response.put("count", commits.size());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Logs error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}

/**
 * GitHub Controller
 */
@RestController
@RequestMapping("/api/github")
class GitHubController {
    private static final Logger logger = LoggerFactory.getLogger(GitHubController.class);
    
    @Autowired
    private GitHubAPIService gitHubService;
    
    @Autowired
    private AuthenticationService authService;
    
    // ============ PRIVATE HELPER METHODS ============
    
    private User extractUserFromToken(String authHeader) throws Exception {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        
        String token = authHeader.substring(7);
        return authService.validateToken(token);
    }
    
    /**
     * GET /api/github/workflow/{name}
     * Get workflow status
     */
    @GetMapping("/workflow/{name}")
    public ResponseEntity<?> getWorkflowStatus(
            @PathVariable String name,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = extractUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Authentication required"));
            }
            
            Map<String, Object> status = gitHubService.getLatestWorkflowStatus(name);
            boolean successful = gitHubService.isLastWorkflowSuccessful(name);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("workflow", name);
            response.put("lastRun", status);
            response.put("successful", successful);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Workflow status error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    /**
     * GET /api/github/runs
     * Get recent workflow runs
     */
    @GetMapping("/runs")
    public ResponseEntity<?> getWorkflowRuns(
            @RequestParam(defaultValue = "10") int limit,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = extractUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Authentication required"));
            }
            
            List<Map<String, Object>> runs = gitHubService.getRecentWorkflowRuns(limit);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("runs", runs);
            response.put("count", runs.size());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Workflow runs error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
    
    /**
     * POST /api/github/issue
     * Create GitHub issue
     */
    @PostMapping("/issue")
    public ResponseEntity<?> createIssue(
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            User user = extractUserFromToken(authHeader);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Authentication required"));
            }
            
            String title = request.get("title");
            String body = request.get("body");
            String label = request.getOrDefault("label", "bug");
            
            if (title == null || title.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Title required"));
            }
            
            String issueNumber = gitHubService.createIssue(title, body, label);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Issue created successfully");
            response.put("issueNumber", issueNumber);
            response.put("createdBy", user.getUsername());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("❌ Issue creation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}
