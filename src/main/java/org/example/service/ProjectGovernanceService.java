package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ProjectGovernanceService
 *
 * Centralizes universal operational rules so all project workflows
 * (new generation + existing repo improvement) follow the same policy.
 */
@Service
public class ProjectGovernanceService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectGovernanceService.class);

    public static final String SUPREMEAI_BOT_INSTALL_URL = "https://github.com/apps/supremeai-bot";
    private static final String POLICY_VERSION = "2026-04-cloud-first-one-rule";
    private static final String RESERVED_MAIN_REPO = "https://github.com/paykaribazaronline/supremeai";

    /**
     * Validate universal project governance rules.
     */
    public void validateProjectGovernance(String projectId, String repoUrl, String branch) {
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalArgumentException("projectId is required");
        }
        if (repoUrl == null || repoUrl.isBlank()) {
            throw new IllegalArgumentException("repoUrl is required");
        }

        String normalizedRepo = normalizeRepoUrl(repoUrl);
        String normalizedMain = normalizeRepoUrl(RESERVED_MAIN_REPO);
        if (normalizedMain.equalsIgnoreCase(normalizedRepo)) {
            throw new IllegalArgumentException("Target repo cannot be the main SupremeAI repository. Use a dedicated project repository.");
        }

        if (branch == null || branch.isBlank()) {
            throw new IllegalArgumentException("repoBranch is required");
        }
    }

    /**
     * Attach universal rule metadata to response/status maps.
     */
    public void applyUniversalRuleMetadata(Map<String, Object> target) {
        if (target == null) {
            return;
        }

        target.put("policyVersion", POLICY_VERSION);
        target.put("cloudFirst", true);
        target.put("oneRuleForAllProjects", true);
        target.put("ciCdAutoEnabled", true);
        target.put("requiresSupremeAIBot", true);
        target.put("installSupremeAIBot", SUPREMEAI_BOT_INSTALL_URL);

        List<String> rules = new ArrayList<>();
        rules.add("Cloud-first execution with local fallback only when required");
        rules.add("One-rule policy across all projects (new and existing)");
        rules.add("CI/CD workflow auto-enabled and checked after push");
        rules.add("SupremeAI bot installation required for full repository access");
        target.put("governanceRules", rules);
    }

    /**
     * Build a compact map when a response needs policy details inline.
     */
    public Map<String, Object> buildPolicySummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        applyUniversalRuleMetadata(summary);
        return summary;
    }

    /**
     * Auto-detect repository lifecycle state from actual repo content.
     */
    public Map<String, Object> detectRepositoryState(String repoUrl, String repoToken, String branch) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("state", "UNKNOWN");
        result.put("autoDetected", false);

        if (repoUrl == null || repoUrl.isBlank()) {
            result.put("reason", "repoUrl missing");
            return result;
        }

        Path cloneDir = Paths.get(System.getProperty("java.io.tmpdir"), "supremeai-detect-" + System.currentTimeMillis());
        try {
            Files.createDirectories(cloneDir);
            String cloneUrl = repoUrl;
            if (repoToken != null && !repoToken.isBlank() && repoUrl.startsWith("https://")) {
                cloneUrl = repoUrl.replace("https://", "https://" + repoToken + "@");
            }

            int cloneExit = runProcess(cloneDir.getParent().toFile(),
                    "git", "clone", "--depth", "1", "--branch", branch == null || branch.isBlank() ? "main" : branch,
                    cloneUrl, cloneDir.toString());
            if (cloneExit != 0) {
                result.put("reason", "clone failed");
                return result;
            }

            long usefulFiles = Files.walk(cloneDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> !path.toString().contains(File.separator + ".git" + File.separator))
                    .filter(path -> {
                        String rel = cloneDir.relativize(path).toString().replace('\\', '/').toLowerCase();
                        return !(rel.equals("readme.md")
                                || rel.equals("license")
                                || rel.equals("license.md")
                                || rel.startsWith(".github/"));
                    })
                    .count();

            boolean hasCodeSignals = Files.exists(cloneDir.resolve("src"))
                    || Files.exists(cloneDir.resolve("lib"))
                    || Files.exists(cloneDir.resolve("app"))
                    || Files.exists(cloneDir.resolve("package.json"))
                    || Files.exists(cloneDir.resolve("pom.xml"))
                    || Files.exists(cloneDir.resolve("build.gradle"))
                    || Files.exists(cloneDir.resolve("build.gradle.kts"));

            String state = (hasCodeSignals || usefulFiles > 2) ? "EXISTING_CODEBASE" : "NEW_OR_EMPTY_REPO";
            result.put("state", state);
            result.put("autoDetected", true);
            result.put("usefulFiles", usefulFiles);
            result.put("hasCodeSignals", hasCodeSignals);
            return result;
        } catch (Exception e) {
            logger.warn("Repository state detection failed for {}: {}", repoUrl, e.getMessage());
            result.put("reason", "detection failed: " + e.getMessage());
            return result;
        } finally {
            deleteQuietly(cloneDir);
        }
    }

    private String normalizeRepoUrl(String repoUrl) {
        String normalized = repoUrl.trim();
        if (normalized.endsWith(".git")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private int runProcess(File workDir, String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workDir);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            while (br.readLine() != null) {
                // consume output
            }
        }
        return process.waitFor();
    }

    private void deleteQuietly(Path dir) {
        try {
            if (!Files.exists(dir)) {
                return;
            }
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception ignored) {
        }
    }
}
