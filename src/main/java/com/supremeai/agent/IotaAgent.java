package com.supremeai.agent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class IotaAgent implements AgentCapability {
  private static final Logger log = LoggerFactory.getLogger(IotaAgent.class);

  private final Map<String, VectorEntry> vectorStore = new ConcurrentHashMap<>();
  private final Map<String, EmbeddingCache> embeddingCache = new ConcurrentHashMap<>();

  @Override
  public String getAgentId() {
    return "IOTA";
  }

  @Override
  public String getAgentName() {
    return "Iota-Knowledge";
  }

  @Override
  public List<String> getTriggerKeywords() {
    return Arrays.asList(
        "vector", "embedding", "semantic", "search", "similarity", "pinecone", "qdrant");
  }

  @Override
  public Mono<String> process(String task, Map<String, Object> context) {
    log.info("[IotaAgent] Performing vector search for task: {}", task);

    return Mono.fromCallable(
            () -> {
              String query = (String) context.getOrDefault("query", task);
              Integer topK = (Integer) context.getOrDefault("topK", 10);
              String collection = (String) context.getOrDefault("collection", "default");

              List<VectorMatch> matches = searchSimilar(query, topK, collection);

              return generateVectorReport(query, matches, collection);
            })
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
  }

  public void indexDocument(String id, String content, String collection) {
    double[] embedding = generateSimpleEmbedding(content);
    vectorStore.put(
        id, new VectorEntry(id, content, embedding, collection, System.currentTimeMillis()));
    embeddingCache.put(id, new EmbeddingCache(embedding, System.currentTimeMillis()));
  }

  public List<VectorMatch> searchSimilar(String query, Integer topK, String collection) {
    double[] queryEmbedding = generateSimpleEmbedding(query);

    return vectorStore.values().stream()
        .filter(e -> collection == null || collection.equals(e.collection()))
        .map(
            e -> {
              double similarity = cosineSimilarity(queryEmbedding, e.embedding());
              return new VectorMatch(e.id(), e.content(), similarity, e.collection());
            })
        .filter(m -> m.similarity() > 0.3)
        .sorted((a, b) -> Double.compare(b.similarity(), a.similarity()))
        .limit(topK)
        .collect(Collectors.toList());
  }

  private double[] generateSimpleEmbedding(String text) {
    int dimension = 128;
    double[] embedding = new double[dimension];

    String[] words = text.toLowerCase().split("\\s+");
    Random random = new Random(text.hashCode());

    for (int i = 0; i < dimension; i++) {
      embedding[i] = random.nextDouble();
    }

    for (String word : words) {
      int index = Math.abs(word.hashCode()) % dimension;
      embedding[index] += 0.1;
    }

    return embedding;
  }

  private double cosineSimilarity(double[] a, double[] b) {
    double dot = 0, normA = 0, normB = 0;
    for (int i = 0; i < a.length && i < b.length; i++) {
      dot += a[i] * b[i];
      normA += a[i] * a[i];
      normB += b[i] * b[i];
    }
    return dot / (Math.sqrt(normA) * Math.sqrt(normB) + 1e-10);
  }

  public void deleteCollection(String collection) {
    vectorStore.entrySet().removeIf(e -> e.getValue().collection().equals(collection));
  }

  public int getCollectionSize(String collection) {
    return (int)
        vectorStore.values().stream().filter(e -> e.collection().equals(collection)).count();
  }

  private String generateVectorReport(String query, List<VectorMatch> matches, String collection) {
    StringBuilder report = new StringBuilder();
    report.append("[IotaAgent] Vector Search Results:\n\n");
    report.append("Query: ").append(query).append("\n");
    report.append("Collection: ").append(collection).append("\n");
    report.append("Matches Found: ").append(matches.size()).append("\n\n");

    if (matches.isEmpty()) {
      report.append("No similar vectors found. Consider indexing more documents.\n");
    } else {
      report.append("Top Matches:\n");
      for (int i = 0; i < matches.size(); i++) {
        VectorMatch m = matches.get(i);
        report
            .append("  #")
            .append(i + 1)
            .append(" (sim=")
            .append(String.format("%.3f", m.similarity()))
            .append(")\n");
        report.append("    ID: ").append(m.id()).append("\n");
        report.append("    ").append(truncate(m.content(), 100)).append("\n\n");
      }
    }

    report.append("Vector Store Stats:\n");
    report.append("  Total Vectors: ").append(vectorStore.size()).append("\n");
    report
        .append("  Collections: ")
        .append(
            vectorStore.values().stream()
                .map(VectorEntry::collection)
                .distinct()
                .collect(Collectors.joining(", ")))
        .append("\n");

    return report.toString();
  }

  private String truncate(String text, int maxLength) {
    return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
  }

  public record VectorMatch(String id, String content, double similarity, String collection) {}

  public record VectorEntry(
      String id, String content, double[] embedding, String collection, long timestamp) {}

  public record EmbeddingCache(double[] embedding, long timestamp) {}
}
