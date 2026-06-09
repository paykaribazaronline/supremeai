package com.supremeai.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Stub provider for local-first operation. Provides REAL, useful offline
 * responses without
 * requiring external AI API keys. Uses comprehensive rule-based patterns to
 * generate genuinely
 * helpful answers.
 */
@Component
public class StubLocalProvider implements AIProvider {

  private static final Logger log = LoggerFactory.getLogger(StubLocalProvider.class);

  // Knowledge base for offline answers, loaded from JSON
  private final Map<String, String> knowledgeBase = new LinkedHashMap<>();

  @PostConstruct
  public void init() {
    try {
      ObjectMapper mapper = new ObjectMapper();
      ClassPathResource resource = new ClassPathResource("core_knowledge.json");
      if (resource.exists()) {
        List<Map<String, String>> loadedData = mapper.readValue(
            resource.getInputStream(), new TypeReference<List<Map<String, String>>>() {
            });
        for (Map<String, String> entry : loadedData) {
          knowledgeBase.put(entry.get("task"), entry.get("solution"));
        }
        log.info(
            "[StubLocalProvider] Loaded {} topics from core_knowledge.json", knowledgeBase.size());
      } else {
        log.warn("[StubLocalProvider] core_knowledge.json not found, knowledge base will be empty");
        // Add basic fallback
        knowledgeBase.put(
            "hello", "Hello! I'm SupremeAI (Offline). I couldn't load my knowledge base.");
      }
    } catch (Exception e) {
      log.error("[StubLocalProvider] Failed to load knowledge base: {}", e.getMessage());
    }
  }

  @Override
  public String getName() {
    return "stub-local";
  }

  @Override
  public Map<String, Object> getCapabilities() {
    Map<String, Object> caps = new HashMap<>();
    caps.put("offline", true);
    caps.put("localModel", true);
    caps.put("supportsCode", true);
    caps.put("supportsChat", true);
    caps.put("ruleBasedResponses", true);
    caps.put("topicCount", knowledgeBase.size());
    return caps;
  }

  @Override
  public Mono<String> generate(String prompt) {
    log.info("[StubLocalProvider] Generating real response for: {}", prompt);
    return Mono.just(generateRealResponse(prompt));
  }

  private String generateRealResponse(String prompt) {
    if (prompt == null || prompt.trim().isEmpty() || knowledgeBase.isEmpty()) {
      return "Please ask me a question — I can help with programming, development, DevOps, databases, and more.";
    }

    String p = prompt.toLowerCase().trim();

    // সুনির্দিষ্ট টপিক ম্যাচিং লজিক (Longest Key First)
    List<String> matchTriggers = List.of(
        "what is ",
        "what are ",
        "explain ",
        "tell me about ",
        "about ");
    List<String> sortedKeys = new ArrayList<>(knowledgeBase.keySet());
    sortedKeys.sort((a, b) -> Integer.compare(b.length(), a.length()));

    for (String key : sortedKeys) {
      String lowerKey = key.toLowerCase();

      // সরাসরি তথ্যমূলক প্রশ্নের জন্য চেক
      for (String trigger : matchTriggers) {
        if (p.contains(trigger + lowerKey)) {
          return knowledgeBase.get(key);
        }
      }

      // সঠিক শব্দ সীমানা (Word Boundary) চেক
      if (p.matches(".*\\b" + java.util.regex.Pattern.quote(lowerKey) + "\\b.*")) {
        log.info("[StubLocalProvider] Exact word matched topic: {}", key);
        return knowledgeBase.get(key);
      }
    }

    // Multi-word topic matching (e.g., "create flutter app" → matches "flutter")
    String[] words = p.split("\\s+");
    for (String word : words) {
      if (word.length() > 2 && knowledgeBase.containsKey(word)) {
        log.info("[StubLocalProvider] Word-matched topic: {}", word);
        return knowledgeBase.get(word);
      }
    }

    // KNOWLEDGE EXTERNALIZATION: Fixed guides removed.
    // If no match in knowledgeBase, return a neutral response allowing web fallback
    // to take over.
    return "## Here's what I can help you with:\n\n"
        + "No relevant information found in local knowledge. Redirecting to Brain...";
  }
}
