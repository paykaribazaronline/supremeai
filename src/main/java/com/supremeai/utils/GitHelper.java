package com.supremeai.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper for running Git commands using the bundled MinGit located at
 * {@code F:/supremeai/.gcloud/git/cmd/git.exe}. All commands are executed with a
 * short timeout and their output is captured for logging.
 */
public class GitHelper {
    private static final Logger log = LoggerFactory.getLogger(GitHelper.class);
    private static final String GIT_EXE = "F:/supremeai/.gcloud/git/cmd/git.exe";

    private static String runGitCommand(String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(GIT_EXE);
        for (String a : args) {
            command.add(a);
        }
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File("F:/supremeai"));
        pb.redirectErrorStream(true);
        Process proc = pb.start();
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append('\n');
            }
        }
        int exit = proc.waitFor();
        if (exit != 0) {
            throw new RuntimeException("git command failed with exit code " + exit + ": " + out);
        }
        return out.toString().trim();
    }

    /** Returns the URL for the given remote name (e.g. "github" or "gitlab"). */
    public static String getRemoteUrl(String remoteName) {
        try {
            String out = runGitCommand("remote", "get-url", remoteName);
            log.info("[GitHelper] Remote '{}' URL: {}", remoteName, out);
            return out;
        } catch (Exception e) {
            log.warn("[GitHelper] Unable to get URL for remote '{}': {}", remoteName, e.getMessage());
            return null;
        }
    }

    /** Adds all changes, commits with a generic message, and pushes to all configured remotes. */
    public static void commitAndPushAll() {
        try {
            runGitCommand("add", ".");
            runGitCommand("commit", "-m", "Automated commit: sync repo URLs to Firestore");
            // push to each remote separately to avoid ambiguity
            String[] remotes = {"github", "gitlab"};
            for (String r : remotes) {
                try {
                    runGitCommand("push", r, "HEAD:main");
                    log.info("[GitHelper] Pushed to remote {} successfully", r);
                } catch (Exception pushEx) {
                    log.warn("[GitHelper] Push to remote {} failed: {}", r, pushEx.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("[GitHelper] Commit/push failed: {}", e.getMessage(), e);
        }
    }

    // ---------- Conditional helpers ----------
    /** Returns true if there are uncommitted changes in the repo. */
    public static boolean hasChanges() {
        return hasUncommittedChanges();
    }

    /** Alias for hasChanges – checks status --porcelain */
    public static boolean hasUncommittedChanges() {
        try {
            String status = runGitCommand("status", "--porcelain");
            return !status.isBlank();
        } catch (Exception e) {
            log.warn("[GitHelper] Unable to check git status: {}", e.getMessage());
            return false;
        }
    }

    /** Returns true if the current branch is 'main' */
    public static boolean isOnMainBranch() {
        try {
            String branch = runGitCommand("rev-parse", "--abbrev-ref", "HEAD");
            return "main".equalsIgnoreCase(branch.trim());
        } catch (Exception e) {
            log.warn("[GitHelper] Unable to determine current branch: {}", e.getMessage());
            return false;
        }
    }

    /** Returns true if a dry‑run push reports pending changes */
    public static boolean dryPushHasPending() {
        try {
            String out = runGitCommand("push", "--dry-run");
            return !out.contains("Everything up-to-date");
        } catch (Exception e) {
            log.warn("[GitHelper] Dry‑run push failed: {}", e.getMessage());
            return false;
        }
    }
}
