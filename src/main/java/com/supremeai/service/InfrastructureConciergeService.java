package com.supremeai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supremeai.model.GeneratedApp;
import com.supremeai.model.InfrastructureAdvice;
import com.supremeai.provider.AIProvider;
import com.supremeai.repository.GeneratedAppRepository;
import com.supremeai.repository.InfrastructureAdviceRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class InfrastructureConciergeService {

  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(InfrastructureConciergeService.class);

  private final AIProviderDiscoveryService providerDiscoveryService;
  private final GitHubAutomationService githubService;
  private final InfrastructureAdviceRepository adviceRepository;
  private final GeneratedAppRepository appRepository;
  private final ObjectMapper objectMapper;

  // Manual constructor for dependency injection
  public InfrastructureConciergeService(
      AIProviderDiscoveryService providerDiscoveryService,
      GitHubAutomationService githubService,
      InfrastructureAdviceRepository adviceRepository,
      GeneratedAppRepository appRepository,
      ObjectMapper objectMapper) {
    this.providerDiscoveryService = providerDiscoveryService;
    this.githubService = githubService;
    this.adviceRepository = adviceRepository;
    this.appRepository = appRepository;
    this.objectMapper = objectMapper;
  }

  /** Returns all generated infrastructure advice. */
  public reactor.core.publisher.Flux<InfrastructureAdvice> getAllAdvice() {
    return adviceRepository.findAll();
  }

  /** Gets existing advice or generates new one. */
  public Mono<InfrastructureAdvice> getOrGenerateAdvice(String appId) {
    return adviceRepository
        .findByAppId(appId)
        .switchIfEmpty(
            appRepository
                .findById(appId)
                .flatMap(this::generateAdvice)
                .flatMap(adviceRepository::save));
  }

  /** Generates a raw Markdown string for UI consumption before project creation. */
  public Mono<String> generateRawAdvice(String name, String description, String techStack) {
    String prompt =
        String.format(
            "Provide a high-level infrastructure advice for: %s. Description: %s. Tech Stack: %s. "
                + "Output MUST be in Markdown format with headers, bullet points, and cost estimates.",
            name, description, techStack);
    AIProvider provider = providerDiscoveryService.getBestProviderForTask("reasoning");
    return provider.generateContent(prompt);
  }

  /** Generates infrastructure advice based on the app's metadata and generated code. */
  private Mono<InfrastructureAdvice> generateAdvice(GeneratedApp app) {
    log.info("Generating infrastructure advice for app: {}", app.getName());

    String prompt = constructPrompt(app);
    AIProvider provider = providerDiscoveryService.getBestProviderForTask("reasoning");

    return provider
        .generateContent(prompt)
        .map(this::parseAdviceFromAI)
        .map(
            advice -> {
              advice.setAppId(app.getId());
              advice.setAppName(app.getName());
              advice.setCreatedAt(System.currentTimeMillis());
              advice.setId(UUID.randomUUID().toString());
              return advice;
            })
        .doOnError(e -> log.error("Failed to generate infra advice", e));
  }

  private String constructPrompt(GeneratedApp app) {
    return String.format(
        "As an Infrastructure Architect, analyze the following application and provide hosting recommendations.\n"
            + "App Name: %s\n"
            + "Tech Stack: %s\n"
            + "Description: %s\n\n"
            + "Return a JSON format exactly like this:\n"
            + "{\n"
            + "  \"recommendedProvider\": \"...\",\n"
            + "  \"recommendedTier\": \"...\",\n"
            + "  \"estimatedMonthlyCost\": 0.0,\n"
            + "  \"summary\": \"...\",\n"
            + "  \"components\": [{\"name\": \"...\", \"service\": \"...\", \"reason\": \"...\"}],\n"
            + "  \"securityBestPractices\": [\"...\"],\n"
            + "  \"scalabilityTips\": [\"...\"]\n"
            + "}",
        app.getName(), app.getTechStack(), app.getDescription());
  }

  private InfrastructureAdvice parseAdviceFromAI(String aiResponse) {
    log.info("AI Response for Infra: {}", aiResponse);
    try {
      // Remove markdown code blocks if present
      String json = aiResponse;
      if (json.contains("```json")) {
        json = json.substring(json.indexOf("```json") + 7);
        json = json.substring(0, json.indexOf("```"));
      } else if (json.contains("```")) {
        json = json.substring(json.indexOf("```") + 3);
        json = json.substring(0, json.indexOf("```"));
      }

      return objectMapper.readValue(json.trim(), InfrastructureAdvice.class);
    } catch (Exception e) {
      log.warn("Failed to parse AI response as JSON: {}. Attempting fallback.", e.getMessage());
      return getFallbackAdvice();
    }
  }

  private InfrastructureAdvice getFallbackAdvice() {
    return InfrastructureAdvice.builder()
        .recommendedProvider("Firebase")
        .recommendedTier("Spark Plan (Free)")
        .summary("We recommend starting with Firebase Free Tier for rapid deployment.")
        .build();
  }
}
