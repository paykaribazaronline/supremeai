package com.supremeai.controller;

import com.supremeai.service.GitHubAutomationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/github")
@PreAuthorize("hasRole('ADMIN')")
public class GitHubAdminController {

    private final GitHubAutomationService gitHubAutomationService;

    public GitHubAdminController(GitHubAutomationService gitHubAutomationService) {
        this.gitHubAutomationService = gitHubAutomationService;
    }

    @GetMapping("/repos/{owner}/{repo}/pulls")
    public Mono<ResponseEntity<List>> getOpenPullRequests(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam String installationId) {
        
        return gitHubAutomationService.getOpenPullRequests(owner, repo, installationId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PutMapping("/repos/{owner}/{repo}/pulls/{pullNumber}/merge")
    public Mono<ResponseEntity<Map<String, Object>>> mergePullRequest(
            @PathVariable String owner,
            @PathVariable String repo,
            @PathVariable int pullNumber,
            @RequestBody MergeRequest mergeRequest) {
        
        return gitHubAutomationService.mergePullRequest(
                        owner, repo, pullNumber, mergeRequest.getCommitTitle(), mergeRequest.getInstallationId())
                .map(ResponseEntity::ok);
    }

    @PostMapping("/repos/{owner}/{repo}/workflows/{workflowId}/dispatch")
    public Mono<ResponseEntity<Map<String, String>>> triggerWorkflow(
            @PathVariable String owner,
            @PathVariable String repo,
            @PathVariable String workflowId,
            @RequestBody DispatchRequest request) {
        
        return gitHubAutomationService.triggerWorkflow(
                        owner, repo, workflowId, request.getBranch(), request.getInstallationId())
                .then(Mono.just(ResponseEntity.ok(Map.of("status", "success", "message", "Workflow triggered successfully"))));
    }

    @GetMapping("/repos/{owner}/{repo}/workflows/{workflowId}/runs/latest")
    public Mono<ResponseEntity<Map<String, Object>>> getLatestWorkflowRun(
            @PathVariable String owner,
            @PathVariable String repo,
            @PathVariable String workflowId,
            @RequestParam String installationId) {
        
        return gitHubAutomationService.getLatestWorkflowRun(owner, repo, workflowId, installationId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    public static class MergeRequest {
        private String commitTitle;
        private String installationId;

        public String getCommitTitle() { return commitTitle; }
        public void setCommitTitle(String commitTitle) { this.commitTitle = commitTitle; }
        public String getInstallationId() { return installationId; }
        public void setInstallationId(String installationId) { this.installationId = installationId; }
    }

    public static class DispatchRequest {
        private String branch;
        private String installationId;

        public String getBranch() { return branch; }
        public void setBranch(String branch) { this.branch = branch; }
        public String getInstallationId() { return installationId; }
        public void setInstallationId(String installationId) { this.installationId = installationId; }
    }
}