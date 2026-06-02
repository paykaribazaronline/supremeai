package com.supremeai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service to handle high-level GitHub automation tasks like creating repositories
 * and managing content via the GitHub API.
 */
@Service
public class GitHubAutomationService {

    private static final Logger log = LoggerFactory.getLogger(GitHubAutomationService.class);

    private final GitHubAppService gitHubAppService;
    private final WebClient webClient;

    @Autowired
    public GitHubAutomationService(@Autowired(required = false) GitHubAppService gitHubAppService) {
        this.gitHubAppService = gitHubAppService;
        this.webClient = WebClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .build();
    }

    /**
     * Creates a new repository in an organization or for the authenticated user.
     */
    public Mono<Map<String, Object>> createRepository(String orgName, String repoName, String description, String installationId) {
        log.info("Creating GitHub repository: {}/{}", orgName, repoName);
        
        return gitHubAppService.getInstallationToken(installationId)
                .flatMap(token -> {
                    Map<String, Object> body = new HashMap<>();
                    body.put("name", repoName);
                    body.put("description", description);
                    body.put("auto_init", true); // Create an initial commit with a README
                    body.put("private", false);

                    String uri = (orgName != null && !orgName.isEmpty()) 
                            ? "/orgs/" + orgName + "/repos" 
                            : "/user/repos";

                    return webClient.post()
                            .uri(uri)
                            .header("Authorization", "token " + token)
                            .bodyValue(body)
                            .retrieve()
                            .bodyToMono(Map.class)
                            .map(res -> (Map<String, Object>) res);
                })
                .doOnSuccess(res -> log.info("Successfully created repository: {}", res.get("html_url")))
                .doOnError(e -> log.error("Failed to create repository: {}", e.getMessage()));
    }

    /**
     * Adds or updates a README file in the repository.
     */
    public Mono<Void> addReadme(String owner, String repo, String content, String installationId) {
        log.info("Adding README to {}/{}", owner, repo);
        
        return gitHubAppService.getInstallationToken(installationId)
                .flatMap(token -> {
                    Map<String, Object> body = new HashMap<>();
                    body.put("message", "Add README via SupremeAI Automation");
                    body.put("content", java.util.Base64.getEncoder().encodeToString(content.getBytes()));

                    return webClient.put()
                            .uri("/repos/{owner}/{repo}/contents/README.md", owner, repo)
                            .header("Authorization", "token " + token)
                            .bodyValue(body)
                            .retrieve()
                            .bodyToMono(Void.class);
                })
                .doOnSuccess(v -> log.info("Successfully added README to {}/{}", owner, repo))
                .doOnError(e -> log.error("Failed to add README: {}", e.getMessage()));
    }

    /**
     * Pushes multiple files to a GitHub repository.
     * This method writes the files to a temporary directory, initializes git, and pushes to the remote.
     */
    public Mono<String> pushGeneratedCode(String owner, String repo, Map<String, String> files, String installationId) {
        return gitHubAppService.getAuthenticatedGitUrl(owner, repo, installationId)
                .flatMap(authUrl -> Mono.fromCallable(() -> {
                    Path tempDir = Files.createTempDirectory("supremeai-push-" + repo);
                    try {
                        log.info("Preparing to push code to {}/{} in temp dir {}", owner, repo, tempDir);
                        
                        // 1. Write all files to temp directory
                        for (Map.Entry<String, String> entry : files.entrySet()) {
                            Path filePath = tempDir.resolve(entry.getKey());
                            Files.createDirectories(filePath.getParent());
                            Files.writeString(filePath, entry.getValue());
                        }

                        // 2. Run git commands
                        runCommand(tempDir, "git", "init", "-b", "main");
                        runCommand(tempDir, "git", "config", "user.email", "automation@supremeai.com");
                        runCommand(tempDir, "git", "config", "user.name", "SupremeAI Automation");
                        runCommand(tempDir, "git", "add", ".");
                        runCommand(tempDir, "git", "commit", "-m", "Initial code generation by SupremeAI");
                        
                        // Add remote (force if already exists)
                        runCommand(tempDir, "git", "remote", "add", "origin", authUrl);
                        
                        // Push
                        runCommand(tempDir, "git", "push", "-u", "origin", "main", "--force");

                        log.info("Successfully pushed code to {}/{}", owner, repo);
                        return "https://github.com/" + owner + "/" + repo;
                    } finally {
                        // Cleanup temp directory
                        deleteDirectory(tempDir.toFile());
                    }
                }))
                .doOnError(e -> log.error("Failed to push code to {}: {}", repo, e.getMessage()));
    }

    /**
     * Enables GitHub Pages for a repository using the 'main' branch and root directory.
     */
    public Mono<String> enablePages(String owner, String repo, String installationId) {
        log.info("Enabling GitHub Pages for {}/{}", owner, repo);
        
        return gitHubAppService.getInstallationToken(installationId)
                .flatMap(token -> {
                    Map<String, Object> source = new HashMap<>();
                    source.put("branch", "main");
                    source.put("path", "/");

                    Map<String, Object> body = new HashMap<>();
                    body.put("source", source);

                    return webClient.post()
                            .uri("/repos/{owner}/{repo}/pages", owner, repo)
                            .header("Authorization", "token " + token)
                            .header("Accept", "application/vnd.github.switcheroo-preview+json") // Required for Pages API
                            .bodyValue(body)
                            .retrieve()
                            .bodyToMono(Map.class)
                            .map(res -> {
                                // GitHub Pages URL pattern: https://<owner>.github.io/<repo>/
                                return String.format("https://%s.github.io/%s/", owner.toLowerCase(), repo);
                            })
                            .onErrorResume(e -> {
                                log.warn("Pages might already be enabled or failed: {}", e.getMessage());
                                return Mono.just(String.format("https://%s.github.io/%s/", owner.toLowerCase(), repo));
                            });
                });
    }

    /**
     * Fetch open Pull Requests to view repo status on the Admin Dashboard
     */
    public Mono<List> getOpenPullRequests(String owner, String repo, String installationId) {
        log.info("Fetching open pull requests for {}/{}", owner, repo);
        return gitHubAppService.getInstallationToken(installationId)
                .flatMap(token -> webClient.get()
                        .uri("/repos/{owner}/{repo}/pulls?state=open", owner, repo)
                        .header("Authorization", "token " + token)
                        .retrieve()
                        .bodyToMono(List.class)
                )
                .doOnError(e -> log.error("Failed to fetch PRs: {}", e.getMessage()));
    }

    /**
     * Accept and Merge a Pull Request directly from the Admin Dashboard
     */
    public Mono<Map<String, Object>> mergePullRequest(String owner, String repo, int pullNumber, String commitTitle, String installationId) {
        log.info("Merging PR #{} for {}/{}", pullNumber, owner, repo);
        
        return gitHubAppService.getInstallationToken(installationId)
                .flatMap(token -> {
                    Map<String, Object> body = new HashMap<>();
                    body.put("commit_title", commitTitle);
                    body.put("merge_method", "squash"); // 'squash', 'merge', or 'rebase'

                    return webClient.put()
                            .uri("/repos/{owner}/{repo}/pulls/{pull_number}/merge", owner, repo, pullNumber)
                            .header("Authorization", "token " + token)
                            .bodyValue(body)
                            .retrieve()
                            .bodyToMono(Map.class)
                            .map(res -> (Map<String, Object>) res);
                })
                .doOnSuccess(res -> log.info("Successfully merged PR #{}: {}", pullNumber, res))
                .doOnError(e -> log.error("Failed to merge PR #{}: {}", pullNumber, e.getMessage()));
    }

    /**
     * Trigger a GitHub Actions workflow manually (e.g., from Admin Dashboard)
     */
    public Mono<Void> triggerWorkflow(String owner, String repo, String workflowId, String branch, String installationId) {
        log.info("Triggering workflow {} for {}/{} on branch {}", workflowId, owner, repo, branch);
        
        return gitHubAppService.getInstallationToken(installationId)
                .flatMap(token -> {
                    Map<String, Object> body = new HashMap<>();
                    body.put("ref", branch);
                    return webClient.post()
                            .uri("/repos/{owner}/{repo}/actions/workflows/{workflow_id}/dispatches", owner, repo, workflowId)
                            .header("Authorization", "token " + token)
                            .bodyValue(body)
                            .retrieve()
                            .bodyToMono(Void.class);
                })
                .doOnSuccess(v -> log.info("Successfully triggered workflow {} for {}/{}", workflowId, owner, repo))
                .doOnError(e -> log.error("Failed to trigger workflow: {}", e.getMessage()));
    }

    /**
     * Fetch the latest workflow run status (e.g., to check if deployment failed)
     */
    public Mono<Map<String, Object>> getLatestWorkflowRun(String owner, String repo, String workflowId, String installationId) {
        log.info("Fetching latest workflow run for {}/{}", owner, repo);
        @SuppressWarnings("unchecked")
        Mono<Map<String, Object>> result = gitHubAppService.getInstallationToken(installationId)
                .flatMap(token -> webClient.get()
                        .uri("/repos/{owner}/{repo}/actions/workflows/{workflow_id}/runs?per_page=1", owner, repo, workflowId)
                        .header("Authorization", "token " + token)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .map(res -> {
                            List<Map<String, Object>> runs = (List<Map<String, Object>>) res.get("workflow_runs");
                            if (runs != null && !runs.isEmpty()) {
                                return runs.get(0);
                            }
                            return java.util.Collections.<String, Object>emptyMap();
                        })
                );
        return result.doOnError(e -> log.error("Failed to fetch workflow runs: {}", e.getMessage()));
    }

    private void runCommand(Path directory, String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(directory.toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        String output = new String(process.getInputStream().readAllBytes());
        boolean finished = process.waitFor(60, TimeUnit.SECONDS);
        
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Command timed out: " + String.join(" ", command));
        }
        
        if (process.exitValue() != 0) {
            log.error("Command failed: {} \nOutput: {}", String.join(" ", command), output);
            throw new IOException("Command failed with exit code " + process.exitValue() + ": " + String.join(" ", command));
        }
    }

    private void deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directoryToBeDeleted.delete();
    }
}
