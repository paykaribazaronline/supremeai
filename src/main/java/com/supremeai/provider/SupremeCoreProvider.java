package com.supremeai.provider;

import com.supremeai.model.APIProvider;
import com.supremeai.repository.ProviderRepository;
import java.util.Comparator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class SupremeCoreProvider implements AIProvider {

  private static final Logger logger = LoggerFactory.getLogger(SupremeCoreProvider.class);

  private final ProviderRepository providerRepository;
  private final AIProviderFactory providerFactory;
  private final String name;

  public SupremeCoreProvider(
      ProviderRepository providerRepository, AIProviderFactory providerFactory) {
    this.providerRepository = providerRepository;
    this.providerFactory = providerFactory;
    this.name = "SupremeAI-Core";
    logger.info("[SUPREME-CORE] Initialized as PRIMARY ORCHESTRATOR — cloud-only mode.");
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Map<String, Object> getCapabilities() {
    return Map.of(
        "name", name,
        "role", "primary_orchestrator",
        "type", "internal",
        "cloudOnly", true,
        "speed", "adaptive");
  }

  @Override
  public Mono<String> generate(String prompt) {
    logger.info(
        "[SUPREME-CORE] Processing prompt via cloud orchestration pipeline: {}", truncate(prompt));

    if (providerRepository != null && providerFactory != null) {
      return generateFromCloud(prompt)
          .switchIfEmpty(
              Mono.just(
                  "[SupremeAI Core] সকল cloud provider বর্তমানে অনুপলব্ধ। Admin প্যানেল থেকে provider চেক করুন।"));
    }

    return Mono.just(
        "[SupremeAI Core] Cloud provider repository not configured. Please set up providers in Firestore.");
  }

  private Mono<String> generateFromCloud(String prompt) {
    return Flux.defer(() -> providerRepository.findAll())
        .onErrorResume(
            e -> {
              logger.error(
                  "[SUPREME-CORE] Failed to load providers from Firestore: {}", e.getMessage());
              return Flux.empty();
            })
        .filter(p -> "active".equalsIgnoreCase(p.getStatus()))
        .sort(Comparator.comparingInt(APIProvider::getPriority))
        .flatMap(
            config -> {
              AIProvider cloudProvider = providerFactory.createProviderFromConfig(config);
              if (cloudProvider == null) return Flux.empty();
              logger.info(
                  "[SUPREME-CORE] Delegating to cloud provider: {} (priority={})",
                  config.getName(),
                  config.getPriority());
              return cloudProvider
                  .generate(prompt)
                  .onErrorResume(
                      e -> {
                        logger.warn(
                            "[SUPREME-CORE] Cloud provider '{}' failed: {}. Trying next.",
                            config.getName(),
                            e.getMessage());
                        return Mono.empty();
                      })
                  .flux();
            },
            1)
        .next();
  }

  private String truncate(String text) {
    if (text == null) return "";
    return text.length() > 80 ? text.substring(0, 80) + "..." : text;
  }
}
