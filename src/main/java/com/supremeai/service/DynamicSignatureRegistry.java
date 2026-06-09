package com.supremeai.service;

import com.supremeai.model.SystemLearning;
import com.supremeai.repository.SystemLearningRepository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class DynamicSignatureRegistry {

  private static final Logger log = LoggerFactory.getLogger(DynamicSignatureRegistry.class);

  @Autowired
  private SystemLearningRepository learningRepository;

  private final Map<String, Set<String>> signatureCache = new ConcurrentHashMap<>();
  private final Map<String, Pattern> compiledPatterns = new ConcurrentHashMap<>();
  private volatile boolean initialized = false;

  @EventListener(ApplicationReadyEvent.class)
  public void initialize() {
    refreshSignatures()
        .subscribe(
            count -> {
              initialized = true;
              log.info("[SignatureRegistry] Loaded {} dynamic signatures", count);
            },
            error -> log.warn("[SignatureRegistry] Failed to load signatures: {}", error.getMessage()));
  }

  public Mono<Long> refreshSignatures() {
    return learningRepository
        .findByCategory("SIGNATURE_REGISTRY")
        .flatMap(learning -> {
          Map<String, Object> metadata = learning.getMetadata();
          if (metadata != null && metadata.containsKey("signatures")) {
            Object sigObj = metadata.get("signatures");
            if (sigObj instanceof Map) {
              @SuppressWarnings("unchecked")
              Map<String, Object> sigMap = (Map<String, Object>) sigObj;
              sigMap.forEach((group, values) -> {
                if (values instanceof List) {
                  @SuppressWarnings("unchecked")
                  List<String> list = (List<String>) values;
                  signatureCache.put(group, concurrentSet(list));
                }
              });
            }
          }
          return Mono.just(learning);
        })
        .count()
        .doOnNext(
            count -> {
              setFallbackSignatures();
              log.info("[SignatureRegistry] Loaded {} signature entries", count);
            });
  }

  private void setFallbackSignatures() {
    signatureCache.putIfAbsent("GREETING_PATTERNS", ConcurrentHashMap.newKeySet());
    signatureCache.putIfAbsent("COMMON_PHRASES", ConcurrentHashMap.newKeySet());
    signatureCache.putIfAbsent("TECH_STACK_KEYWORDS", ConcurrentHashMap.newKeySet());
    signatureCache.putIfAbsent("DOMAIN_MAPPINGS", ConcurrentHashMap.newKeySet());
    signatureCache.putIfAbsent("STOP_WORDS", ConcurrentHashMap.newKeySet());
    signatureCache.putIfAbsent("INSUFFICIENT_MARKERS", ConcurrentHashMap.newKeySet());
    signatureCache.putIfAbsent("COMPLEX_TASK_PATTERNS", ConcurrentHashMap.newKeySet());
    signatureCache.putIfAbsent("CODE_LANGUAGE_INDICATORS", ConcurrentHashMap.newKeySet());
    signatureCache.putIfAbsent("DATABASE_INDICATORS", ConcurrentHashMap.newKeySet());
    signatureCache.putIfAbsent("MOBILE_OPTION_TEMPLATES", ConcurrentHashMap.newKeySet());
    signatureCache.putIfAbsent("BUILD_OPTION_TEMPLATES", ConcurrentHashMap.newKeySet());
    signatureCache.putIfAbsent("DATABASE_OPTION_TEMPLATES", ConcurrentHashMap.newKeySet());
    signatureCache.putIfAbsent("DEBUG_OPTION_TEMPLATES", ConcurrentHashMap.newKeySet());
    signatureCache.putIfAbsent("API_OPTION_TEMPLATES", ConcurrentHashMap.newKeySet());
    signatureCache.putIfAbsent("GENERAL_OPTION_TEMPLATES", ConcurrentHashMap.newKeySet());
    signatureCache.putIfAbsent("MISSING_INFO_QUESTIONS", ConcurrentHashMap.newKeySet());
    signatureCache.putIfAbsent("GREETING_RESPONSES", ConcurrentHashMap.newKeySet());
    signatureCache.putIfAbsent("RESPONSE_TEMPLATES", ConcurrentHashMap.newKeySet());
    signatureCache.putIfAbsent("KNOWN_TOPICS", ConcurrentHashMap.newKeySet());
    signatureCache.putIfAbsent("TOPIC_CATEGORIES", ConcurrentHashMap.newKeySet());
    signatureCache.putIfAbsent("JAVA_VERSIONS", ConcurrentHashMap.newKeySet());
    signatureCache.putIfAbsent("MIN_USEFUL_SNIPPET_LENGTH", concurrentSet(java.util.List.of("30")));

    // Default signatures for CodeGenerationService
    signatureCache.putIfAbsent("DEFAULT_ARCHITECTURE", concurrentSet(java.util.List.of("monolith")));
    signatureCache.putIfAbsent("DEFAULT_DATABASE", concurrentSet(java.util.List.of("PostgreSQL")));
    signatureCache.putIfAbsent("DEFAULT_API_STYLE", concurrentSet(java.util.List.of("REST")));
    signatureCache.putIfAbsent("DEFAULT_AUTH", concurrentSet(java.util.List.of("JWT")));
    signatureCache.putIfAbsent("DEFAULT_FRONTEND", concurrentSet(java.util.List.of("React")));
    signatureCache.putIfAbsent("DEFAULT_DEPLOYMENT", concurrentSet(java.util.List.of("GCP")));
  }

  private Set<String> concurrentSet(Collection<String> values) {
    Set<String> set = ConcurrentHashMap.newKeySet();
    set.addAll(values);
    return set;
  }

  public Set<String> getSignatures(String group) {
    return signatureCache.getOrDefault(group, ConcurrentHashMap.newKeySet());
  }

  public Map<String, String> getPatternsByCategory(String category) {
    return getSignatures(category).stream().collect(Collectors.toMap(value -> value, value -> value));
  }

  public String getDefault(String group, String defaultVal) {
    Set<String> signatures = signatureCache.getOrDefault(group, ConcurrentHashMap.newKeySet());
    return signatures.stream().findFirst().orElse(defaultVal);
  }

  public Set<String> findMatches(String content, String group) {
    Set<String> signatures = signatureCache.getOrDefault(group, Collections.emptySet());
    return signatures.stream()
        .filter(sig -> {
          if (sig.contains("|")) {
            String[] alternatives = sig.split("\\|");
            return Arrays.stream(alternatives).anyMatch(alt -> content.contains(alt.toLowerCase()));
          }
          return content.contains(sig.toLowerCase());
        })
        .collect(Collectors.toSet());
  }

  public void registerSignature(String group, String signature) {
    signatureCache.computeIfAbsent(group, k -> ConcurrentHashMap.newKeySet()).add(signature);
  }

  public void registerSignatures(String group, Set<String> signatures) {
    signatureCache.computeIfAbsent(group, k -> ConcurrentHashMap.newKeySet()).addAll(signatures);
  }

  public boolean matchesAny(String input, String group) {
    Set<String> signatures = signatureCache.getOrDefault(group, Collections.emptySet());
    String lowerInput = input.toLowerCase();
    return signatures.stream().anyMatch(s -> lowerInput.contains(s.toLowerCase()));
  }

  public String detectCategory(String input) {
    String lowerInput = input.toLowerCase();
    if (signatureCache.getOrDefault("GREETING_PATTERNS", Collections.emptySet()).stream()
        .anyMatch(lowerInput::contains)) {
      return "GREETING";
    }
    if (signatureCache.getOrDefault("COMMON_PHRASES", Collections.emptySet()).stream()
        .anyMatch(lowerInput::contains)) {
      return "COMMON";
    }
    if (signatureCache.getOrDefault("TEMPORAL_PATTERNS", Collections.emptySet()).stream()
        .anyMatch(lowerInput::contains)) {
      return "TEMPORAL";
    }
    return "GENERAL";
  }

  public boolean isInitialized() {
    return initialized;
  }
}
