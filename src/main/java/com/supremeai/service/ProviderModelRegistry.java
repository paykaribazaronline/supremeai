package com.supremeai.service;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.ListenerRegistration;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Registry to manage supported models for AI providers dynamically from Firestore collection
 * 'provider_models'. Adheres to Zero Hardcode and Solo Mode principles.
 */
@Service
public class ProviderModelRegistry {
  private static final Logger logger = LoggerFactory.getLogger(ProviderModelRegistry.class);

  @Autowired(required = false)
  private Firestore firestore;

  private final Map<String, List<String>> registryCache = new ConcurrentHashMap<>();
  private final Map<String, List<String>> fallbackModels = new HashMap<>();
  private ListenerRegistration listenerRegistration;
  private final Executor listenerExecutor = Executors.newSingleThreadExecutor();

  public ProviderModelRegistry() {
    // Fallback models for robust solo mode execution
    fallbackModels.put("openai", List.of("gpt-4", "gpt-4-turbo-preview", "gpt-3.5-turbo"));
    fallbackModels.put("google", List.of("gemini-1.5-pro", "gemini-1.5-flash"));
    fallbackModels.put(
        "anthropic", List.of("claude-3-5-sonnet", "claude-3-opus", "claude-3-haiku"));
    fallbackModels.put("ollama", List.of("phi-3-mini", "llama3"));
  }

  @PostConstruct
  public void init() {
    logger.info("Initializing ProviderModelRegistry with dynamic Firestore synchronization...");
    registryCache.putAll(fallbackModels);

    if (firestore == null) {
      logger.warn("Firestore is null - ProviderModelRegistry will run with default fallbacks");
      return;
    }

    try {
      this.listenerRegistration =
          firestore
              .collection("provider_models")
              .addSnapshotListener(
                  listenerExecutor,
                  (snapshot, error) -> {
                    if (error != null) {
                      logger.error("Firestore listener error for provider_models", error);
                      return;
                    }

                    if (snapshot != null) {
                      snapshot
                          .getDocumentChanges()
                          .forEach(
                              change -> {
                                try {
                                  String docId = change.getDocument().getId();
                                  String providerId = change.getDocument().getString("providerId");
                                  @SuppressWarnings("unchecked")
                                  List<String> models =
                                      (List<String>) change.getDocument().get("supportedModels");

                                  if (providerId != null && models != null) {
                                    String providerKey = providerId.toLowerCase().trim();
                                    if (change.getType()
                                        == com.google.cloud.firestore.DocumentChange.Type.REMOVED) {
                                      registryCache.remove(providerKey);
                                      if (fallbackModels.containsKey(providerKey)) {
                                        registryCache.put(
                                            providerKey, fallbackModels.get(providerKey));
                                      }
                                      logger.info(
                                          "Provider models configuration removed: {}", providerKey);
                                    } else {
                                      registryCache.put(providerKey, models);
                                      logger.info(
                                          "Provider models configuration updated for {}: {}",
                                          providerKey,
                                          models);
                                    }
                                  }
                                } catch (Exception e) {
                                  logger.error(
                                      "Error processing provider_models change from Firestore", e);
                                }
                              });
                    }
                  });
    } catch (Exception e) {
      logger.error("Failed to setup ProviderModelRegistry snapshot listener", e);
    }
  }

  @PreDestroy
  public void cleanup() {
    if (listenerRegistration != null) {
      listenerRegistration.remove();
    }
  }

  /**
   * Get the list of supported models for a given provider dynamically.
   *
   * @param providerId The provider ID (e.g. "openai", "google")
   * @return List of supported models
   */
  public List<String> getSupportedModels(String providerId) {
    if (providerId == null || providerId.isBlank()) {
      return List.of();
    }

    String key = providerId.toLowerCase().trim();
    return registryCache.getOrDefault(key, List.of());
  }
}
