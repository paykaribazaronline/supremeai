package org.example.service;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Git Integration Service
 * 
 * Handles:
 * - Clone repository
 * - Make changes
 * - Commit changes
 * - Push to origin
 * 
 * ADMIN PROVIDES:
 * - Git repo URL (https or SSH)
 * - Branch to work on
 * - Commit message format
 * - Push configuration
 */
public class GitIntegrationService {
    
    private final String workspaceRoot;
    private final FirebaseService firebase;
    
    public GitIntegrationService(String workspaceRoot, FirebaseService firebase) {
        this.workspaceRoot = workspaceRoot;
        this.firebase = firebase;
    }
    
    /**
     * Clone repository for a project
     */
    public boolean cloneRepository(String projectId, String gitUrl, String branch) {
        try {
            String projectPath = workspaceRoot + File.separator + projectId;
            Files.createDirectories(Paths.get(projectPath));
            
            System.out.println("🔄 [GIT] Cloning repository: " + gitUrl);
            ProcessBuilder pb = new ProcessBuilder("git", "clone", "--branch", branch, gitUrl, projectPath);
            pb.directory(new File(workspaceRoot));
            
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                System.out.println("✅ [GIT] Repository cloned successfully");
                logGitAction(projectId, "CLONE", "✅ Cloned: " + gitUrl);
                return true;
            } else {
                System.err.println("❌ [GIT] Failed to clone repository");
                logGitAction(projectId, "CLONE", "❌ Failed to clone: " + gitUrl);
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ [GIT] Clone error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Make file changes (developer/AI modifies files)
     */
    public void modifyFile(String projectId, String filePath, String content) throws IOException {
        String fullPath = workspaceRoot + File.separator + projectId + File.separator + filePath;
        Files.createDirectories(Paths.get(fullPath).getParent());
        Files.write(Paths.get(fullPath), content.getBytes());
        System.out.println("✏️ [GIT] Modified: " + filePath);
    }
    
    /**
     * Get file contents
     */
    public String getFileContent(String projectId, String filePath) throws IOException {
        String fullPath = workspaceRoot + File.separator + projectId + File.separator + filePath;
        return new String(Files.readAllBytes(Paths.get(fullPath)));
    }
    
    /**
     * Commit changes to git
     */
    public boolean commitChanges(String projectId, String commitMessage) {
        try {
            String projectPath = workspaceRoot + File.separator + projectId;
            
            System.out.println("📝 [GIT] Committing: " + commitMessage);
            
            // Stage all changes
            ProcessBuilder pbAdd = new ProcessBuilder("git", "add", ".");
            pbAdd.directory(new File(projectPath));
            pbAdd.start().waitFor();
            
            // Commit
            ProcessBuilder pbCommit = new ProcessBuilder("git", "commit", "-m", commitMessage);
            pbCommit.directory(new File(projectPath));
            Process process = pbCommit.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                System.out.println("✅ [GIT] Changes committed");
                logGitAction(projectId, "COMMIT", "✅ " + commitMessage);
                return true;
            } else {
                System.out.println("⚠️ [GIT] Nothing to commit or commit failed");
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ [GIT] Commit error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Push changes to origin
     */
    public boolean pushToOrigin(String projectId, String branch, String credentials) {
        try {
            String projectPath = workspaceRoot + File.separator + projectId;
            
            System.out.println("⬆️ [GIT] Pushing to origin (" + branch + ")");
            
            ProcessBuilder pb = new ProcessBuilder("git", "push", "origin", branch);
            pb.directory(new File(projectPath));
            
            // Set credentials in environment if provided
            if (credentials != null && !credentials.isEmpty()) {
                Map<String, String> env = pb.environment();
                // Parse credentials: "username:token" format
                env.put("GIT_CREDENTIALS", credentials);
            }
            
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                System.out.println("✅ [GIT] Pushed to origin");
                logGitAction(projectId, "PUSH", "✅ Pushed to " + branch);
                return true;
            } else {
                readProcessError(process);
                System.err.println("❌ [GIT] Failed to push");
                logGitAction(projectId, "PUSH", "❌ Push failed");
                return false;
            }
        } catch (Exception e) {
            System.err.println("❌ [GIT] Push error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Create pull request (webhook to GitHub/GitLab/Bitbucket)
     */
    public String createPullRequest(String projectId, String fromBranch, String toBranch, 
                                   String title, String description, String githubToken) {
        try {
            System.out.println("🔀 [GIT] Creating pull request: " + title);
            
            // In production, use GitHub API:
            // POST https://api.github.com/repos/{owner}/{repo}/pulls
            // With token authentication
            
            logGitAction(projectId, "PR", "🔀 PR created: " + title);
            return "PR#123"; // Mock PR number
        } catch (Exception e) {
            System.err.println("❌ [GIT] PR creation error: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get current branch status
     */
    public Map<String, Object> getRepositoryStatus(String projectId) {
        try {
            String projectPath = workspaceRoot + File.separator + projectId;
            Map<String, Object> status = new HashMap<>();
            
            // Get branch name
            ProcessBuilder pbBranch = new ProcessBuilder("git", "rev-parse", "--abbrev-ref", "HEAD");
            pbBranch.directory(new File(projectPath));
            String branch = readProcessOutput(pbBranch.start()).trim();
            status.put("branch", branch);
            
            // Get commit count
            ProcessBuilder pbCommits = new ProcessBuilder("git", "rev-list", "--count", "HEAD");
            pbCommits.directory(new File(projectPath));
            String commits = readProcessOutput(pbCommits.start()).trim();
            status.put("commits", commits);
            
            // Get modified files
            ProcessBuilder pbStatus = new ProcessBuilder("git", "status", "--porcelain");
            pbStatus.directory(new File(projectPath));
            String modified = readProcessOutput(pbStatus.start()).trim();
            status.put("modified_files", modified.isEmpty() ? 0 : modified.split("\n").length);
            
            return status;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
    
    private void logGitAction(String projectId, String action, String result) {
        try {
            Map<String, Object> logEntry = new HashMap<>();
            logEntry.put("action", action);
            logEntry.put("result", result);
            logEntry.put("timestamp", System.currentTimeMillis());
            // TODO: Implement Firebase logging method
            // firebase.saveGitLog(projectId, logEntry);
        } catch (Exception e) {
            System.err.println("Failed to log git action: " + e.getMessage());
        }
    }
    
    private String readProcessOutput(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        return output.toString();
    }
    
    private void readProcessError(Process process) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.err.println(line);
        }
    }
}
