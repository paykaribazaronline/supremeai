package com.supremeai.service.analysis;

import com.supremeai.model.analysis.CodeChunk;
import com.supremeai.repository.analysis.CodeChunkRepository;
import com.supremeai.service.ConfigService;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class VectorSearchService {

  private static final Logger log = LoggerFactory.getLogger(VectorSearchService.class);

  private final CodeChunkRepository codeChunkRepository;
  private final EmbeddingService embeddingService;
  private final ConfigService configService;

  @Autowired
  public VectorSearchService(
      CodeChunkRepository codeChunkRepository,
      EmbeddingService embeddingService,
      ConfigService configService) {
    this.codeChunkRepository = codeChunkRepository;
    this.embeddingService = embeddingService;
    this.configService = configService;
  }

  private boolean isMockAllowed() {
    return "true".equalsIgnoreCase(configService.getSetting("allow_mock_fallback", "false"));
  }

  public Mono<Void> storeEmbeddings(String projectId, List<CodeChunkData> chunks) {
    List<String> contents =
        chunks.stream().map(CodeChunkData::getContent).collect(Collectors.toList());
    List<List<Double>> embeddings;
    try {
      embeddings = embeddingService.generateBatchEmbeddings(contents);
    } catch (Exception e) {
      if (isMockAllowed()) {
        log.warn("Failed to generate embeddings, using defaults: {}", e.getMessage());
        List<Double> dummyEmbedding = new ArrayList<>();
        for (int i = 0; i < 768; i++) dummyEmbedding.add(0.0);
        embeddings = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
          embeddings.add(dummyEmbedding);
        }
      } else {
        log.error("Failed to generate embeddings and mock fallback is disabled", e);
        return Mono.error(
            new RuntimeException("Embedding generation failed: " + e.getMessage(), e));
      }
    }

    List<CodeChunk> codeChunks = new ArrayList<>();
    List<Double> dummyEmbedding = new ArrayList<>();
    for (int i = 0; i < 768; i++) dummyEmbedding.add(0.0);

    for (int i = 0; i < chunks.size(); i++) {
      CodeChunkData chunk = chunks.get(i);
      List<Double> embedding = i < embeddings.size() ? embeddings.get(i) : dummyEmbedding;

      codeChunks.add(
          CodeChunk.builder()
              .id(UUID.randomUUID().toString())
              .projectId(projectId)
              .file(chunk.getFile())
              .startLine(chunk.getStartLine())
              .endLine(chunk.getEndLine())
              .content(chunk.getContent())
              .hash(chunk.getHash())
              .language(chunk.getLanguage())
              .embedding(embedding)
              .createdAt(Instant.now().toString())
              .build());
    }

    return Flux.fromIterable(codeChunks)
        .flatMap(codeChunkRepository::save)
        .then()
        .doOnSuccess(
            v -> log.info("Stored {} embeddings for project {}", codeChunks.size(), projectId))
        .doOnError(
            e ->
                log.error(
                    "Failed to store embeddings for project {}: {}", projectId, e.getMessage()));
  }

  public List<CodeChunk> searchSimilarChunks(String projectId, String query, int topK) {
    List<Double> queryEmbedding;
    try {
      queryEmbedding = embeddingService.generateEmbedding(query);
    } catch (Exception e) {
      log.error("Failed to generate query embedding: {}", e.getMessage());
      throw new RuntimeException("Embedding generation failed: " + e.getMessage(), e);
    }

    List<CodeChunk> allChunks;
    try {
      allChunks =
          codeChunkRepository
              .findByProjectId(projectId)
              .collectList()
              .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
              .block(java.time.Duration.ofSeconds(30));
    } catch (Exception e) {
      log.warn("Failed to fetch chunks for project {}: {}", projectId, e.getMessage());
      return List.of();
    }

    if (allChunks == null || allChunks.isEmpty()) {
      return List.of();
    }

    return allChunks.stream()
        .filter(c -> c.getEmbedding() != null && !c.getEmbedding().isEmpty())
        .sorted(
            Comparator.comparingDouble(c -> -cosineSimilarity(queryEmbedding, c.getEmbedding())))
        .limit(topK)
        .collect(Collectors.toList());
  }

  public List<CodeChunk> searchByFileType(
      String projectId, String query, String language, int topK) {
    List<CodeChunk> candidates;
    try {
      candidates =
          codeChunkRepository.findByProjectIdAndLanguage(projectId, language).collectList().block();
    } catch (Exception e) {
      return List.of();
    }

    if (candidates == null || candidates.isEmpty()) {
      return List.of();
    }

    List<Double> queryEmbedding = embeddingService.generateEmbedding(query);

    return candidates.stream()
        .filter(c -> c.getEmbedding() != null && !c.getEmbedding().isEmpty())
        .sorted(
            Comparator.comparingDouble(c -> -cosineSimilarity(queryEmbedding, c.getEmbedding())))
        .limit(topK)
        .collect(Collectors.toList());
  }

  public Mono<Void> clearProjectEmbeddings(String projectId) {
    return codeChunkRepository
        .findByProjectId(projectId)
        .flatMap(codeChunkRepository::delete)
        .then()
        .doOnSuccess(v -> log.info("Cleared embeddings for project {}", projectId));
  }

  public double cosineSimilarity(List<Double> a, List<Double> b) {
    if (a.size() != b.size() || a.isEmpty()) return 0.0;

    double dotProduct = 0.0;
    double normA = 0.0;
    double normB = 0.0;

    for (int i = 0; i < a.size(); i++) {
      dotProduct += a.get(i) * b.get(i);
      normA += a.get(i) * a.get(i);
      normB += b.get(i) * b.get(i);
    }

    if (normA == 0.0 || normB == 0.0) return 0.0;
    return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
  }
}
