package com.supremeai.service;

import com.supremeai.agent.GPublishAgent;
import com.supremeai.agentorchestration.AdaptiveAgentOrchestrator;
import com.supremeai.model.EntityDefinition;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * AppOrchestrationService — The "Brain" of the system. Handles the end-to-end flow: Requirement ->
 * Code Generation -> GitHub Repo -> Code Push.
 */
@Service
public class AppOrchestrationService {

  private static final Logger log = LoggerFactory.getLogger(AppOrchestrationService.class);

  @Autowired(required = false)
  private AdaptiveAgentOrchestrator orchestrator;

  @Autowired private CodeGenerationService codeGenerationService;

  @Autowired private GPublishAgent publishAgent;

  @Autowired private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

  private void broadcastProgress(int step, int progress, String message) {
    Map<String, Object> payload = new java.util.HashMap<>();
    payload.put("step", step);
    payload.put("progress", progress);
    payload.put("message", message);
    payload.put("timestamp", System.currentTimeMillis());
    messagingTemplate.convertAndSend("/topic/pipeline/progress", payload);
  }

  /** Executes the full automated pipeline. */
  public Mono<Map<String, Object>> runFullPipeline(
      String requirement, Map<String, String> githubConfig) {
    log.info("Starting full automation pipeline for requirement: {}", requirement);
    broadcastProgress(0, 5, "Initializing SupremeAI Pipeline...");

    if (orchestrator == null) {
      return Mono.error(
          new IllegalStateException("Orchestrator unavailable. Check Firestore credentials."));
    }

    return Mono.fromCallable(
            () -> {
              broadcastProgress(1, 20, "Analyzing requirements with AI Orchestrator...");
              return orchestrator.orchestrate(requirement);
            })
        .flatMap(
            orchestrationResult -> {
              broadcastProgress(2, 45, "Designing architectural blueprint and entities...");
              Map<String, Object> generationContext = orchestrationResult.getGenerationContext();

              @SuppressWarnings("unchecked")
              Map<String, String> decisions = (Map<String, String>) (Map<?, ?>) generationContext;

              @SuppressWarnings("unchecked")
              List<EntityDefinition> entities =
                  (List<EntityDefinition>) generationContext.get("entities");

              log.info(
                  "Orchestration complete. Decisions: {}, Entities found: {}",
                  orchestrationResult.getContext().get("decisions"),
                  entities != null ? entities.size() : 0);

              broadcastProgress(3, 70, "Synthesizing components and generating code...");
              Map<String, Object> codeResult;
              if (entities != null && !entities.isEmpty()) {
                codeResult =
                    codeGenerationService.generateAppWithAI(
                        "GeneratedApp-" + System.currentTimeMillis(),
                        requirement,
                        entities,
                        decisions.getOrDefault("database", "PostgreSQL"),
                        decisions.getOrDefault("authType", "JWT"));
              } else {
                codeResult = codeGenerationService.generateFromContext(decisions);
              }

              Map<String, Object> response = new LinkedHashMap<>();
              response.put("status", "GENERATED");
              response.put("requirement", requirement);
              response.put("decisions", orchestrationResult.getContext().get("decisions"));
              response.put("entities", entities);
              response.put("generatedApp", codeResult);

              broadcastProgress(4, 85, "Finalizing deployment and pushing to GitHub...");
              // Determine GitHub Config (Explicit or Automated via System Rules)
              return handleGitHubDeployment(codeResult, githubConfig)
                  .map(
                      deployResult -> {
                        broadcastProgress(4, 100, "Pipeline completed successfully. Code is live.");
                        response.put("github", deployResult);
                        response.put("status", "COMPLETED");
                        return response;
                      })
                  .defaultIfEmpty(response);
            })
        .doOnSuccess(res -> log.info("Full pipeline execution completed successfully"))
        .doOnError(
            e -> {
              broadcastProgress(4, 0, "Pipeline failed: " + e.getMessage());
              log.error("Pipeline execution failed: {}", e.getMessage());
            });
  }

  private Mono<Map<String, Object>> handleGitHubDeployment(
      Map<String, Object> codeResult, Map<String, String> explicitConfig) {
    @SuppressWarnings("unchecked")
    Map<String, String> files = (Map<String, String>) codeResult.get("files");

    if (files == null || files.isEmpty()) {
      return Mono.empty();
    }

    if (explicitConfig != null
        && explicitConfig.containsKey("repo")
        && explicitConfig.containsKey("owner")) {
      // Use explicit config
      return publishAgent.deployToGitHub(
          explicitConfig.get("owner"),
          explicitConfig.get("repo"),
          files,
          explicitConfig.get("installationId"));
    } else {
      // Check for automated push rule
      return publishAgent
          .getAutoPushConfig()
          .flatMap(
              autoConfig -> {
                if ("true".equals(autoConfig.get("autoPush"))) {
                  String owner = autoConfig.get("owner");
                  String repo = "app-" + System.currentTimeMillis();
                  return publishAgent.deployToGitHub(owner, repo, files, null);
                }
                return Mono.empty();
              });
    }
  }
}
