package com.supremeai.service;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.ListenerRegistration;
import com.supremeai.model.APIProvider;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProviderRoleSuggestionService {

  private static final Logger log = LoggerFactory.getLogger(ProviderRoleSuggestionService.class);

  @Autowired private Firestore firestore;

  private final Map<String, List<String>> roleKeywordsCache = new ConcurrentHashMap<>();
  private ListenerRegistration listenerRegistration;
  private final Executor listenerExecutor = Executors.newSingleThreadExecutor();

  @PostConstruct
  public void init() {
    log.info("[RoleSuggestion] Initializing with Firestore real-time listener...");
    try {
      listenerRegistration =
          firestore
              .collection("role_keywords")
              .addSnapshotListener(
                  listenerExecutor,
                  (snapshot, error) -> {
                    if (error != null) {
                      log.error("[RoleSuggestion] Firestore listener error", error);
                      return;
                    }
                    if (snapshot != null) {
                      snapshot
                          .getDocumentChanges()
                          .forEach(
                              change -> {
                                try {
                                  String role = change.getDocument().getId();
                                  Object keywords = change.getDocument().get("keywords");
                                  if (keywords instanceof List) {
                                    List<String> kwList = new ArrayList<>();
                                    for (Object k : (List<?>) keywords) {
                                      if (k instanceof String)
                                        kwList.add(((String) k).toLowerCase());
                                    }
                                    switch (change.getType()) {
                                      case ADDED, MODIFIED -> {
                                        roleKeywordsCache.put(role.toLowerCase(), kwList);
                                        log.info(
                                            "[RoleSuggestion] Role keywords updated: {} -> {}",
                                            role,
                                            kwList);
                                      }
                                      case REMOVED -> {
                                        roleKeywordsCache.remove(role.toLowerCase());
                                        log.info(
                                            "[RoleSuggestion] Role keywords removed: {}", role);
                                      }
                                    }
                                  }
                                } catch (Exception e) {
                                  log.error(
                                      "[RoleSuggestion] Error deserializing role keywords", e);
                                }
                              });
                    }
                  });
    } catch (Exception e) {
      log.error("[RoleSuggestion] Failed to setup listener", e);
    }

    roleKeywordsCache.put("coding", List.of("coder", "code", "codegen", "extension", "functions"));
    roleKeywordsCache.put(
        "security",
        List.of(
            "audit",
            "exploit",
            "security",
            "defense",
            "hacking",
            "penetration",
            "cve",
            "vulnerability"));
    roleKeywordsCache.put(
        "reasoning",
        List.of("pro", "reasoning", "think", "complex", "logic", "arithmetic", "plan"));
    roleKeywordsCache.put(
        "fast_chat", List.of("flash", "mini", "light", "fast", "optimized", "compact"));
    roleKeywordsCache.put(
        "multimodal",
        List.of("vision", "audio", "omni", "picture", "image", "multimodal", "speech"));
  }

  @PreDestroy
  public void cleanup() {
    if (listenerRegistration != null) {
      listenerRegistration.remove();
    }
  }

  public List<String> suggestRoles(APIProvider provider) {
    Set<String> suggestions = new HashSet<>();
    String name = (provider.getName() + " " + provider.getType()).toLowerCase();

    for (Map.Entry<String, List<String>> entry : roleKeywordsCache.entrySet()) {
      for (String keyword : entry.getValue()) {
        if (name.contains(keyword)) {
          suggestions.add(entry.getKey());
          break;
        }
      }
    }

    if (suggestions.isEmpty()) {
      suggestions.add("general_chat");
    }

    return new ArrayList<>(suggestions);
  }
}
