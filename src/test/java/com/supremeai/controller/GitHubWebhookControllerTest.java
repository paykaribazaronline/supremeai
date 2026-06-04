package com.supremeai.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.supremeai.security.SecretManagerService;
import com.supremeai.service.SelfHealingService;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
public class GitHubWebhookControllerTest {

  @Mock private SelfHealingService selfHealingService;

  @Mock private WebSocketController webSocketController;

  @Mock private SecretManagerService secretManagerService;

  @Mock private org.springframework.boot.web.client.RestTemplateBuilder restTemplateBuilder;

  @Mock private RestTemplate restTemplate;

  private GitHubWebhookController controller;

  @BeforeEach
  void setUp() {
    when(restTemplateBuilder.build()).thenReturn(restTemplate);
    controller =
        new GitHubWebhookController(
            selfHealingService, webSocketController, secretManagerService, restTemplateBuilder);
  }

  @Test
  void testHandleWorkflowEvent_FailureTriggersSelfHealing() {
    // Setup payload
    Map<String, Object> payload = new HashMap<>();
    payload.put("action", "completed");

    Map<String, Object> workflowJob = new HashMap<>();
    workflowJob.put("conclusion", "failure");
    workflowJob.put("id", 12345L);
    workflowJob.put("workflow_name", "Build Test");
    payload.put("workflow_job", workflowJob);

    Map<String, Object> repository = new HashMap<>();
    repository.put("full_name", "org/repo");
    payload.put("repository", repository);

    // Execute
    ResponseEntity<String> response = controller.handleWorkflowEvent(payload);

    // Verify
    assertEquals(200, response.getStatusCodeValue());
    assertEquals("Self-healing triggered", response.getBody());
    verify(selfHealingService).handleWorkflowFailure(eq("org/repo"), eq("12345"), anyString());
    verify(webSocketController)
        .broadcastPipelineNotification(eq("failure"), anyString(), anyString());
  }

  @Test
  void testHandleWorkflowEvent_SuccessDoesNotTriggerSelfHealing() {
    Map<String, Object> payload = new HashMap<>();
    payload.put("action", "completed");

    Map<String, Object> workflowJob = new HashMap<>();
    workflowJob.put("conclusion", "success");
    payload.put("workflow_job", workflowJob);

    ResponseEntity<String> response = controller.handleWorkflowEvent(payload);

    assertEquals(200, response.getStatusCodeValue());
    assertEquals("Event ignored", response.getBody());
    verify(selfHealingService, never())
        .handleWorkflowFailure(anyString(), anyString(), anyString());
    verify(webSocketController)
        .broadcastPipelineNotification(eq("success"), anyString(), anyString());
  }

  @Test
  void testHandleWorkflowEvent_SuccessDeploymentTriggersAnalysis() {
    // Setup payload for a successful deployment
    Map<String, Object> payload = new HashMap<>();
    payload.put("action", "completed");
    payload.put("after", "sha12345");

    Map<String, Object> workflowJob = new HashMap<>();
    workflowJob.put("conclusion", "success");
    workflowJob.put("id", 999L);
    workflowJob.put("workflow_name", "Production Deployment");
    payload.put("workflow_job", workflowJob);

    Map<String, Object> repository = new HashMap<>();
    repository.put("full_name", "org/repo");
    Map<String, Object> owner = new HashMap<>();
    owner.put("login", "user123");
    repository.put("owner", owner);
    payload.put("repository", repository);

    when(secretManagerService.getSecret("GITHUB_TOKEN")).thenReturn("mock-token");

    // Execute
    ResponseEntity<String> response = controller.handleWorkflowEvent(payload);

    // Verify immediate response
    assertEquals(200, response.getStatusCodeValue());
    assertEquals(
        "Event ignored",
        response.getBody()); // Success deployments return "Event ignored" after firing
    // thread

    // Verify asynchronous AI analysis trigger (using timeout because it's in a
    // separate thread)
    verify(restTemplate, timeout(1000))
        .postForEntity(
            eq("https://us-central1-supremeai-a.cloudfunctions.net/analyzeDeployment"),
            any(Map.class),
            eq(String.class));
  }

  @Test
  void testHandleWorkflowEvent_IgnoresIncompletePayload() {
    Map<String, Object> payload = new HashMap<>();
    // Missing "action" or "workflow_job"

    ResponseEntity<String> response = controller.handleWorkflowEvent(payload);

    assertEquals(200, response.getStatusCodeValue());
    assertEquals("Event ignored", response.getBody());
    verifyNoInteractions(selfHealingService);
  }

  @Test
  void testHandleWorkflowEvent_IgnoresNonCompletedAction() {
    Map<String, Object> payload = new HashMap<>();
    payload.put("action", "queued");

    Map<String, Object> workflowJob = new HashMap<>();
    workflowJob.put("conclusion", "failure");
    payload.put("workflow_job", workflowJob);

    ResponseEntity<String> response = controller.handleWorkflowEvent(payload);

    assertEquals(200, response.getStatusCodeValue());
    assertEquals("Event ignored", response.getBody());
  }

  @Test
  void testHandleWorkflowEvent_NullRepository() {
    Map<String, Object> payload = new HashMap<>();
    payload.put("action", "completed");

    Map<String, Object> workflowJob = new HashMap<>();
    workflowJob.put("conclusion", "failure");
    workflowJob.put("id", 123L);
    payload.put("workflow_job", workflowJob);

    ResponseEntity<String> response = controller.handleWorkflowEvent(payload);

    assertEquals(200, response.getStatusCodeValue());
    verify(selfHealingService).handleWorkflowFailure(eq("unknown"), anyString(), anyString());
  }
}
