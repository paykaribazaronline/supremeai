package com.supremeai.service;

import com.supremeai.model.APIProvider;
import com.supremeai.repository.ProviderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service to synchronize providers from Realtime Database to Firestore at startup. Ensures that the
 * admin dashboard (which uses Firestore) reflects the models configured in the Firebase Realtime
 * Database.
 */
@Service
public class ProviderInitializationService {

  private static final Logger log = LoggerFactory.getLogger(ProviderInitializationService.class);

  @Autowired private FirebaseRealtimeService realtimeService;

  @Autowired private ProviderRepository providerRepository;

  @Autowired private ProviderTypeRegistry providerTypeRegistry;

  @Value("${ai.providers.ollama.endpoint:http://localhost:11434}")
  private String ollamaEndpoint;

  /**
   * Infer a provider type category from the provider name. Uses dynamic registry as authoritative
   * source — matches against {@code keywords} list in each {@code provider_types} Firestore
   * document. Falls back to "generic" only if no keyword match is found.
   */
  private String determineType(String name) {
    if (name == null) return "generic";
    String upper = name.toUpperCase();

    // Dynamic matching: iterate all registered types and check their keywords
    for (var entry : providerTypeRegistry.getAllTypes().entrySet()) {
      com.supremeai.model.ProviderTypeConfig config = entry.getValue();
      if (config.getKeywords() != null) {
        for (String keyword : config.getKeywords()) {
          if (keyword != null && upper.contains(keyword.toUpperCase())) {
            return entry.getKey(); // typeId
          }
        }
      }
      // Also match against displayName as fallback
      if (config.getDisplayName() != null
          && upper.contains(config.getDisplayName().toUpperCase())) {
        return entry.getKey();
      }
    }

    log.debug("[STARTUP] No dynamic type match for '{}' — defaulting to 'generic'", name);
    return "generic";
  }

  /** Enriches a provider with default metadata if not already present. */
  private void enrichProviderMetadata(APIProvider provider) {
    if (provider.getName() == null) return;
    if (provider.getBaseUrl() != null && !provider.getBaseUrl().isBlank()) return;

    String type = provider.getType();
    if (type != null && !type.isBlank()) {
      com.supremeai.model.ProviderTypeConfig typeConfig = providerTypeRegistry.getTypeConfig(type);
      if (typeConfig != null) {
        if (provider.getBaseUrl() == null || provider.getBaseUrl().isBlank()) {
          provider.setBaseUrl(typeConfig.getDefaultBaseUrl());
        }
        if ((provider.getModels() == null || provider.getModels().isEmpty())
            && typeConfig.getSupportedModels() != null) {
          provider.setModels(typeConfig.getSupportedModels());
        }
        if (provider.getCapabilities() == null || provider.getCapabilities().isEmpty()) {
          provider.setCapabilities(typeConfig.getCapabilities());
        }
        return;
      }
    }

    log.warn(
        "No type config found for provider '{}' — admin must configure manually in dashboard",
        provider.getName());
  }

  /**
   * Detect deploymentSource for a provider based on its type, name, and email. Mirrors the same
   * logic in ProviderAdminService for consistency.
   */
  private String detectDeploymentSource(String type, String accountEmail, String baseUrl) {
    if (type != null) {
      String upperType = type.toUpperCase();
      if (upperType.equals("GOOGLE") || upperType.equals("GEMINI") || upperType.equals("VERTEX")) {
        return "gcloud";
      }
      if (upperType.equals("LOCAL") || upperType.equals("OLLAMA")) {
        return "ollama";
      }
    }
    if (baseUrl != null && !baseUrl.isBlank()) {
      String lowerUrl = baseUrl.toLowerCase();
      if (lowerUrl.contains("localhost")
          || lowerUrl.contains("127.0.0.1")
          || lowerUrl.contains("ollama")) {
        return "ollama";
      }
    }
    if (accountEmail != null && !accountEmail.isBlank()) {
      String lowerEmail = accountEmail.toLowerCase();
      if (lowerEmail.contains("@google.com")
          || lowerEmail.contains("@gmail.com")
          || lowerEmail.contains("googleapis.com")
          || lowerEmail.contains("firebase")) {
        return "gcloud";
      }
    }
    return "api";
  }

  @EventListener(ApplicationReadyEvent.class)
  public void syncProvidersOnStartup() {
    log.info("[STARTUP] Initializing AI provider synchronization in background...");

    realtimeService
        .getData("config/api_keys")
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
        .flatMapMany(
            keysMap -> {
              if (keysMap == null || keysMap.isEmpty()) {
                log.warn(
                    "[STARTUP] No API keys found in Realtime Database at config/api_keys. Skipping key sync.");
                return Flux.empty();
              }
              return Flux.fromIterable(keysMap.entrySet());
            })
        .flatMap(
            entry -> {
              String name = entry.getKey();
              if (name == null) return Mono.empty();

              String key = String.valueOf(entry.getValue());

              return providerRepository
                  .findById(name.toLowerCase())
                  .switchIfEmpty(
                      Mono.just(
                          new APIProvider(name.toLowerCase(), name, determineType(name), "ACTIVE")))
                  .flatMap(
                      provider -> {
                        provider.setApiKey(key);
                        provider.setLastCheck(java.time.LocalDateTime.now());

                        // Backfill deploymentSource for existing providers
                        if (provider.getDeploymentSource() == null
                            || provider.getDeploymentSource().isBlank()) {
                          provider.setDeploymentSource(
                              detectDeploymentSource(
                                  provider.getType(),
                                  provider.getAccountEmail(),
                                  provider.getBaseUrl()));
                          log.info(
                              "[STARTUP BACKFILL] Set deploymentSource='{}' for provider '{}'",
                              provider.getDeploymentSource(),
                              provider.getName());
                        }

                        if (provider.getBaseUrl() == null || provider.getBaseUrl().isBlank()) {
                          enrichProviderMetadata(provider);
                        }

                        if (provider.getModels() == null || provider.getModels().isEmpty()) {
                          com.supremeai.model.ProviderTypeConfig typeConfig =
                              providerTypeRegistry.getTypeConfig(provider.getType());
                          if (typeConfig != null
                              && typeConfig.getSupportedModels() != null
                              && !typeConfig.getSupportedModels().isEmpty()) {
                            provider.setModels(typeConfig.getSupportedModels());
                          }
                        }

                        log.debug("[STARTUP] Syncing provider key for: {}", name);
                        return providerRepository.save(provider);
                      })
                  .onErrorResume(
                      e -> {
                        log.error("[STARTUP] Failed to sync provider {}: {}", name, e.getMessage());
                        return Mono.empty();
                      });
            })
        .collectList()
        .subscribe(
            list ->
                log.info(
                    "[STARTUP] Provider key sync complete. Total providers updated: {}",
                    list.size()),
            err ->
                log.error(
                    "[STARTUP] Critical error during provider key synchronization: {}",
                    err.getMessage()));
  }
}
