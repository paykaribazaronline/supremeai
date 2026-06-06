package com.supremeai.agent;

import com.supremeai.repository.SystemLearningRepository;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ThetaAgent implements AgentCapability {
  private static final Logger log = LoggerFactory.getLogger(ThetaAgent.class);

  private final Map<String, KnowledgeEntry> knowledgeCache = new HashMap<>();

  @Autowired private AgentRuleService ruleService;

  @Autowired private SystemLearningRepository learningRepository;

  @Override
  public String getAgentId() {
    return "THETA";
  }

  @Override
  public String getAgentName() {
    return "Theta-Learning";
  }

  @Override
  public List<String> getTriggerKeywords() {
    return Arrays.asList("learn", "rag", "knowledge", "context", "history", "retrieve", "search");
  }

  @Override
  public Mono<String> process(String task, Map<String, Object> context) {
    log.info("[ThetaAgent] Performing RAG retrieval for task: {}", task);

    return Mono.fromCallable(
            () -> {
              String query = (String) context.getOrDefault("query", task);
              Integer maxResults = (Integer) context.getOrDefault("maxResults", 5);
              Double minSimilarity = (Double) context.getOrDefault("minSimilarity", 0.7);

              KnowledgeResult result = retrieveKnowledge(query, maxResults, minSimilarity);

              return generateRAGReport(query, result);
            })
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
  }

  public KnowledgeResult retrieveKnowledge(String query, Integer maxResults, Double minSimilarity) {
    List<KnowledgeEntry> entries = new ArrayList<>(knowledgeCache.values());

    entries.sort(
        (a, b) -> Double.compare(similarity(query, b.content), similarity(query, a.content)));

    List<KnowledgeEntry> relevant =
        entries.stream()
            .filter(e -> similarity(query, e.content) >= minSimilarity)
            .limit(maxResults)
            .collect(Collectors.toList());

    return new KnowledgeResult(relevant, query, LocalDateTime.now());
  }

  public void addKnowledge(String key, String content, String source, double weight) {
    knowledgeCache.put(key, new KnowledgeEntry(key, content, source, weight, LocalDateTime.now()));
  }

  private double similarity(String query, String content) {
    String[] queryWords = query.toLowerCase().split("\\s+");
    String[] contentWords = content.toLowerCase().split("\\s+");

    Set<String> querySet = new HashSet<>(Arrays.asList(queryWords));
    Set<String> contentSet = new HashSet<>(Arrays.asList(contentWords));

    if (querySet.isEmpty()) return 0.0;

    int matches = 0;
    for (String word : querySet) {
      if (content.contains(word)) matches++;
    }

    return (double) matches / querySet.size();
  }

  public List<KnowledgeEntry> getRecentKnowledge(int hours) {
    LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
    return knowledgeCache.values().stream()
        .filter(e -> e.timestamp.isAfter(cutoff))
        .sorted((a, b) -> b.timestamp.compareTo(a.timestamp))
        .collect(Collectors.toList());
  }

  private String generateRAGReport(String query, KnowledgeResult result) {
    StringBuilder report = new StringBuilder();
    report.append("[ThetaAgent] RAG Learning Results:\n\n");
    report.append("Query: ").append(query).append("\n");
    report.append("Matches Found: ").append(result.entries().size()).append("\n\n");

    if (result.entries().isEmpty()) {
      report.append("No relevant knowledge found for this query.\n");
      report.append("Recommendation: Add this knowledge to the system via learning flow.\n");
    } else {
      report.append("Top Results:\n");
      for (int i = 0; i < result.entries().size(); i++) {
        KnowledgeEntry entry = result.entries().get(i);
        double sim = similarity(query, entry.content);
        report
            .append("  #")
            .append(i + 1)
            .append(" (sim=")
            .append(String.format("%.2f", sim))
            .append(")\n");
        report.append("    Source: ").append(entry.source).append("\n");
        report.append("    ").append(truncate(entry.content, 150)).append("\n\n");
      }
    }

    return report.toString();
  }

  private String truncate(String text, int maxLength) {
    return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
  }

  public record KnowledgeResult(
      List<KnowledgeEntry> entries, String query, LocalDateTime timestamp) {}

  public record KnowledgeEntry(
      String key, String content, String source, double weight, LocalDateTime timestamp) {}
}
