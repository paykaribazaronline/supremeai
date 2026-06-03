package com.supremeai.controller;

import com.supremeai.service.GitHubAutomationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitHubAdminControllerTest {GitHubAutomationServicepublic GitHubAdminControllerTest(GitHubAutomationService gitHubAutomationService, GitHubAdminController controller) {
GitHubAutomationService    this.gitHubAutomationService = gitHubAutomationService;
GitHubAutomationService    this.controller = controller;
GitHubAutomationService}




    @InjectMocks


    @Test
    void getOpenPullRequests_ReturnsList() {
        when(gitHubAutomationService.getOpenPullRequests("SupremeAI", "repo", "123"))
                .thenReturn(Mono.just(List.of(Map.of("id", 1))));

        StepVerifier.create(controller.getOpenPullRequests("SupremeAI", "repo", "123"))
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertEquals(1, response.getBody().size());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getOpenPullRequests_ReturnsNotFound() {
        when(gitHubAutomationService.getOpenPullRequests("SupremeAI", "repo", "123"))
                .thenReturn(Mono.empty());

        StepVerifier.create(controller.getOpenPullRequests("SupremeAI", "repo", "123"))
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void mergePullRequest_Success() {
        GitHubAdminController.MergeRequest req = new GitHubAdminController.MergeRequest();
        req.setCommitTitle("Merge PR");
        req.setInstallationId("123");

        assertEquals("Merge PR", req.getCommitTitle());
        assertEquals("123", req.getInstallationId());

        when(gitHubAutomationService.mergePullRequest("SupremeAI", "repo", 1, "Merge PR", "123"))
                .thenReturn(Mono.just(Map.of("merged", true)));

        StepVerifier.create(controller.mergePullRequest("SupremeAI", "repo", 1, req))
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertEquals(true, response.getBody().get("merged"));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void triggerWorkflow_Success() {
        GitHubAdminController.DispatchRequest req = new GitHubAdminController.DispatchRequest();
        req.setBranch("main");
        req.setInstallationId("123");

        assertEquals("main", req.getBranch());
        assertEquals("123", req.getInstallationId());

        when(gitHubAutomationService.triggerWorkflow("SupremeAI", "repo", "pipeline.yml", "main", "123"))
                .thenReturn(Mono.empty());

        StepVerifier.create(controller.triggerWorkflow("SupremeAI", "repo", "pipeline.yml", req))
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertEquals("success", response.getBody().get("status"));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getLatestWorkflowRun_ReturnsRun() {
        when(gitHubAutomationService.getLatestWorkflowRun("SupremeAI", "repo", "pipeline.yml", "123"))
                .thenReturn(Mono.just(Map.of("id", 456)));

        StepVerifier.create(controller.getLatestWorkflowRun("SupremeAI", "repo", "pipeline.yml", "123"))
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertEquals(456, response.getBody().get("id"));
                    return true;
                })
                .verifyComplete();
    }
}