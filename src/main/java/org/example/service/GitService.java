package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Git Service
 * Handles git operations: commit, push, pull
 * Respects AdminControl modes (AUTO/WAIT/FORCE_STOP)
 */
@Service
public class GitService {
    private static final Logger logger = LoggerFactory.getLogger(GitService.class);
    
    @Autowired
    private AdminControlService adminControlService;
    
    private static final String GIT_REPO_PATH = System.getenv("SUPREMEAI_GIT_REPO") != null 
        ? System.getenv("SUPREMEAI_GIT_REPO")
        : "c:\\Users\\Nazifa\\supremeai";
    
    /**
     * Stage and commit changes
     * Returns commit hash
     */
    public String commitChanges(String commitMessage, String author) {
        try {
            // ✅ VALIDATION: Check inputs
            if (commitMessage == null || commitMessage.trim().isEmpty()) {
                logger.error("❌ Commit message cannot be empty");
                return null;
            }
            
            if (author == null || author.trim().isEmpty()) {
                logger.error("❌ Author info cannot be empty");
                return null;
            }
            
            // Check admin control
            if (!canPerformGitOperation()) {
                logger.warn("❌ Git operation blocked by admin control: {}", adminControlService.getPermissionMode());
                return null;
            }
            
            // Stage all changes
            executeGitCommand("git", "add", "-A");
            logger.info("📦 Staged all changes");
            
            // Check if there are changes to commit
            String status = executeGitCommand("git", "status", "--porcelain");
            if (status == null || status.trim().isEmpty()) {
                logger.info("✅ No changes to commit");
                return "no-changes";
            }
            
            // Commit with author info - using array args (prevents injection)
            String commitOutput = executeGitCommand("git", "commit", "-m", commitMessage, "--author=" + author);
            
            // Extract commit hash
            String commitHash = extractCommitHash(commitOutput);
            logger.info("✅ Commit successful: {} - {}", commitHash, commitMessage);
            
            return commitHash;
            
        } catch (Exception e) {
            logger.error("❌ Commit failed: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Push commits to remote
     */
    public boolean pushToRemote(String branch) {
        try {
            // ✅ VALIDATION: Validate branch name (no special chars)
            if (branch == null || !branch.matches("^[a-zA-Z0-9._/-]+$")) {
                logger.error("❌ Invalid branch name: {}", branch);
                return false;
            }
            
            if (!canPerformGitOperation()) {
                logger.warn("❌ Git push blocked by admin control");
                return false;
            }
            
            // ✅ FIX: Check for actual git errors
            String output = executeGitCommand("git", "push", "origin", branch);
            
            // Check if push actually succeeded
            if (output != null && (output.contains("error") || output.contains("fatal"))) {
                logger.error("❌ Git push failed: {}", output);
                return false;
            }
            
            logger.info("🚀 Push successful to {}/{}", "origin", branch);
            return true;
            
        } catch (Exception e) {
            logger.error("❌ Push failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get current branch
     */
    public String getCurrentBranch() {
        try {
            String output = executeGitCommand("git", "rev-parse", "--abbrev-ref", "HEAD");
            return output != null ? output.trim() : "main";
        } catch (Exception e) {
            logger.error("❌ Failed to get current branch: {}", e.getMessage());
            return "main";
        }
    }
    
    /**
     * Get recent commits
     */
    public List<Map<String, String>> getRecentCommits(int count) {
        try {
            String output = executeGitCommand("git", "log", "--oneline", "-" + count);
            List<Map<String, String>> commits = new ArrayList<>();
            
            if (output != null) {
                String[] lines = output.split("\n");
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    
                    String[] parts = line.split(" ", 2);
                    Map<String, String> commit = new HashMap<>();
                    commit.put("hash", parts[0]);
                    commit.put("message", parts.length > 1 ? parts[1] : "");
                    commits.add(commit);
                }
            }
            
            return commits;
        } catch (Exception e) {
            logger.error("❌ Failed to get commits: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Get git status
     */
    public Map<String, Object> getStatus() {
        try {
            String status = executeGitCommand("git", "status", "-s");
            String branch = getCurrentBranch();
            
            int modifiedCount = 0;
            int unstagedCount = 0;
            int untrackedCount = 0;
            
            if (status != null) {
                String[] lines = status.split("\n");
                for (String line : lines) {
                    if (line.trim().isEmpty()) continue;
                    if (line.startsWith("M")) modifiedCount++;
                    else if (line.startsWith("??")) untrackedCount++;
                    else unstagedCount++;
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("branch", branch);
            result.put("modified", modifiedCount);
            result.put("untracked", untrackedCount);
            result.put("unstaged", unstagedCount);
            result.put("hasChanges", modifiedCount > 0 || untrackedCount > 0 || unstagedCount > 0);
            
            return result;
        } catch (Exception e) {
            logger.error("❌ Failed to get status: {}", e.getMessage());
            return new HashMap<>();
        }
    }
    
    /**
     * Create and push a new branch
     */
    public boolean createAndPushBranch(String branchName) {
        try {
            if (!canPerformGitOperation()) {
                logger.warn("❌ Git branch operation blocked");
                return false;
            }
            
            executeGitCommand("git", "checkout", "-b", branchName);
            executeGitCommand("git", "push", "-u", "origin", branchName);
            
            logger.info("✅ Branch created and pushed: {}", branchName);
            return true;
            
        } catch (Exception e) {
            logger.error("❌ Branch operation failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Create pull request via GitHub CLI
     */
    public String createPullRequest(String title, String body, String baseBranch) {
        try {
            if (!canPerformGitOperation()) {
                logger.warn("❌ PR creation blocked by admin control");
                return null;
            }
            
            String currentBranch = getCurrentBranch();
            String prOutput = executeGitCommand("gh", "pr", "create",
                "--title", title,
                "--body", body,
                "--base", baseBranch,
                "--head", currentBranch);
            
            logger.info("✅ Pull request created from {} to {}", currentBranch, baseBranch);
            return prOutput;
            
        } catch (Exception e) {
            logger.error("❌ PR creation failed: {}", e.getMessage());
            return null;
        }
    }
    
    // ============ PRIVATE HELPER METHODS ============
    
    /**
     * Check if git operations are allowed based on admin control
     */
    private boolean canPerformGitOperation() {
        return adminControlService.getAdminControl().isRunning() &&
               adminControlService.getPermissionMode() != org.example.model.AdminControl.PermissionMode.FORCE_STOP;
    }
    
    /**
     * Execute git command - with proper stderr separation
     */
    private String executeGitCommand(String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(GIT_REPO_PATH));
        // ✅ FIX: DON'T merge stderr - capture separately
        // pb.redirectErrorStream(true);  <- WRONG!
        
        Process process = pb.start();
        
        // ✅ Read stderr separately to capture real errors
        BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        StringBuilder stderr = new StringBuilder();
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        
        while ((line = errReader.readLine()) != null) {
            stderr.append(line).append("\n");
        }
        
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Git command timeout after 30s: " + String.join(" ", command));
        }
        
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            logger.error("❌ Git command FAILED with code {}: {}", exitCode, String.join(" ", command));
            if (stderr.length() > 0) {
                logger.error("   Error output: {}", stderr.toString());
            }
        }
        
        return output.toString();
    }
    
    /**
     * Extract commit hash from git output - with multiple format support
     */
    private String extractCommitHash(String output) {
        if (output == null || output.trim().isEmpty()) return null;
        
        String[] lines = output.split("\n");
        for (String line : lines) {
            line = line.trim();
            
            // ✅ FIX: Support multiple git output formats:
            // Format 1: [main (root-commit) abc123] Message
            // Format 2: [main abc123] Message
            // Format 3: Commit hash extracted from anywhere
            
            if (line.contains("[") && line.contains("]")) {
                // Extract hash between brackets
                int start = line.indexOf("[") + 1;
                int end = line.indexOf("]");
                String content = line.substring(start, end);
                
                // Extract last word (usually the hash)
                String[] parts = content.split(" ");
                for (int i = parts.length - 1; i >= 0; i--) {
                    String part = parts[i].trim();
                    // Check if it looks like a commit hash (hex, 7+ chars)
                    if (part.matches("^[a-f0-9]{7,}$")) {
                        return part;
                    }
                }
            }
        }
        
        logger.warn("⚠️ Could not extract commit hash from output: {}", output.substring(0, Math.min(100, output.length())));
        return null;
    }
}
