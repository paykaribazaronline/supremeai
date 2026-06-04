package com.supremeai.service;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.ListenerRegistration;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service to manage AI Provider tier configurations, loaded dynamically from Firestore collection
 * 'provider_tiers'. Adheres to Zero Hardcode and Solo Mode principles.
 */
@Service
public class ProviderTierService {
  private static final Logger logger = LoggerFactory.getLogger(ProviderTierService.class);

  @Autowired(required = false)
  private Firestore firestore;

  private final Map<String, String> tierCache = new ConcurrentHashMap<>();
  private final Map<String, String> fallbackTiers = new HashMap<>();
  private ListenerRegistration listenerRegistration;
  private final Executor listenerExecutor = Executors.newSingleThreadExecutor();

  public ProviderTierService() {
    // Fallback tiers to ensure flawless execution in solo mode / offline environments
    fallbackTiers.put("gpt-4", "premium");
    fallbackTiers.put("o1", "premium");
    fallbackTiers.put("claude", "premium");
    fallbackTiers.put("sonnet", "premium");
    fallbackTiers.put("opus", "premium");
    fallbackTiers.put("gpt-3.5", "standard");
    fallbackTiers.put("haiku", "standard");
    fallbackTiers.put("phi-3", "basic");
    fallbackTiers.put("phi", "basic");
    fallbackTiers.put("local", "basic");
  }

  @PostConstruct
  public void init() {
    logger.info("Initializing ProviderTierService with dynamic Firestore synchronization...");
    tierCache.putAll(fallbackTiers);

    if (firestore == null) {
      logger.warn("Firestore is null - ProviderTierService will run with default fallbacks");
      return;
    }

    try {
      this.listenerRegistration =
          firestore
              .collection("provider_tiers")
              .addSnapshotListener(
                  listenerExecutor,
                  (snapshot, error) -> {
                    if (error != null) {
                      logger.error("Firestore listener error for provider_tiers", error);
                      return;
                    }

                    if (snapshot != null) {
                      // If first snapshot or change occurs, refresh cache
                      snapshot
                          .getDocumentChanges()
                          .forEach(
                              change -> {
                                try {
                                  String docId = change.getDocument().getId();
                                  String modelPattern =
                                      change.getDocument().getString("modelPattern");
                                  String tier = change.getDocument().getString("tier");

                                  if (modelPattern != null && tier != null) {
                                    String patternKey = modelPattern.toLowerCase().trim();
                                    if (change.getType()
                                        == com.google.cloud.firestore.DocumentChange.Type.REMOVED) {
                                      tierCache.remove(patternKey);
                                      // Restore from fallback if removed
                                      if (fallbackTiers.containsKey(patternKey)) {
                                        tierCache.put(patternKey, fallbackTiers.get(patternKey));
                                      }
                                      logger.info(
                                          "Provider tier configuration removed: {}", patternKey);
                                    } else {
                                      tierCache.put(patternKey, tier.toLowerCase().trim());
                                      logger.info(
                                          "Provider tier configuration updated: {} -> {}",
                                          patternKey,
                                          tier);
                                    }
                                  }
                                } catch (Exception e) {
                                  logger.error(
                                      "Error processing provider_tier change from Firestore", e);
                                }
                              });
                    }
                  });
    } catch (Exception e) {
      logger.error("Failed to setup ProviderTierService snapshot listener", e);
    }
  }

  @PreDestroy
  public void cleanup() {
    if (listenerRegistration != null) {
      listenerRegistration.remove();
    }
  }

  /**
   * Determines the tier for a given model dynamically based on cached patterns.
   *
   * @param model Model name (e.g. "gpt-4-turbo", "claude-3-5-sonnet")
   * @return Tier string (e.g., "premium", "standard", "basic")
   */
  public String getTierForModel(String model) {
    if (model == null || model.isBlank()) {
      return "basic";
    }

    String lowerModel = model.toLowerCase();

    // Match the model against registered model patterns in tierCache
    for (Map.Entry<String, String> entry : tierCache.entrySet()) {
      if (lowerModel.contains(entry.getKey())) {
        return entry.getValue();
      }
    }

    return "basic"; // Default fallback tier
  }
}
