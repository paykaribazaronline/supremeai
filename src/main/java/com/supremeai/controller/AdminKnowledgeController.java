package com.supremeai.controller;

import com.supremeai.model.KnowledgeDomain;
import com.supremeai.model.KnowledgeRecommendation;
import com.supremeai.repository.KnowledgeDomainRepository;
import com.supremeai.repository.KnowledgeRecommendationRepository;
import com.supremeai.response.ApiResponse;
import com.supremeai.service.KnowledgeService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.Collections;

@RestController
@RequestMapping("/api/admin/knowledge")
@PreAuthorize("hasRole('ADMIN')")
public class AdminKnowledgeController extends BaseAdminController<Object, String> {

  private static final Duration BLOCK_TIMEOUT = Duration.ofSeconds(10);

  private final KnowledgeDomainRepository domainRepository;
  private final KnowledgeRecommendationRepository recommendationRepository;
  private final KnowledgeService knowledgeService;

  @Autowired
  public AdminKnowledgeController(
      KnowledgeDomainRepository domainRepository,
      KnowledgeRecommendationRepository recommendationRepository,
      KnowledgeService knowledgeService) {
    this.domainRepository = domainRepository;
    this.recommendationRepository = recommendationRepository;
    this.knowledgeService = knowledgeService;
  }

  @GetMapping("/snapshot")
  public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> getKnowledgeSnapshot() {
    return knowledgeService.getKnowledgeSnapshot()
        .map(data -> ResponseEntity.ok(ApiResponse.ok(data)))
        .onErrorResume(e -> handleError("Failed to get snapshot", e));
  }

  @GetMapping("/domains")
  public Mono<ResponseEntity<ApiResponse<List<Map<String, Object>>>>> getDomains() {
    return domainRepository.findAll().collectList()
        .map(list -> {
          List<Map<String, Object>> uiDomains = list.stream()
              .map(
                  d -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", d.getId());
                    m.put("name", d.getName());
                    m.put("status", d.getStatus() != null ? d.getStatus().name() : "UNKNOWN");
                    m.put("keywords", d.getKeywords() != null ? d.getKeywords() : List.of());
                    m.put(
                        "knowledgeCount",
                        d.getNodesDiscovered() != null ? d.getNodesDiscovered() : 0);
                    return m;
                  })
              .collect(Collectors.toList());
          return ResponseEntity.ok(ApiResponse.ok(uiDomains));
        })
        .onErrorResume(e -> handleError("Failed to fetch domains", e));
  }

  @PostMapping("/domains")
  public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> createDomain(
      @RequestBody Map<String, Object> body) {
    String name = (String) body.get("name");
    @SuppressWarnings("unchecked")
    List<String> keywords = (List<String>) body.getOrDefault("keywords", Collections.emptyList());

    if (name == null || name.isEmpty()) {
      return Mono.just(ResponseEntity.badRequest().body(ApiResponse.error("Name is required")));
    }

    KnowledgeDomain domain = new KnowledgeDomain(name, keywords);
    return domainRepository.save(domain)
        .map(saved -> ResponseEntity.ok(ApiResponse.ok(Map.of("domain", saved))))
        .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(ApiResponse.error("Failed to create domain"))));
  }

  @GetMapping("/recommendations")
  public Mono<ResponseEntity<ApiResponse<List<Map<String, Object>>>>> getRecommendations() {
    return recommendationRepository.findAll().collectList()
        .map(list -> {
          List<Map<String, Object>> uiRecs = list.stream()
              .map(
                  r -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", r.getId());
                    m.put("title", r.getTopic());
                    m.put("description", r.getReasoning());
                    m.put("confidence", r.getConfidence() != null ? r.getConfidence() : 0.0);
                    m.put(
                        "suggestedKeywords", r.getKeywords() != null ? r.getKeywords() : List.of());
                    m.put(
                        "createdAt",
                        r.getCreatedAt() != null
                            ? r.getCreatedAt().toString()
                            : LocalDateTime.now().toString());
                    return m;
                  })
              .collect(Collectors.toList());
          return ResponseEntity.ok(ApiResponse.ok(uiRecs));
        })
        .onErrorResume(e -> handleError("Failed to fetch recommendations", e));
  }

  @PostMapping("/recommendations/{id}/approve")
  public Mono<ResponseEntity<ApiResponse<String>>> approveRecommendation(@PathVariable String id) {
    return recommendationRepository.findById(id)
        .flatMap(rec -> {
          rec.setStatus(KnowledgeRecommendation.Status.APPROVED);
          rec.setProcessedAt(LocalDateTime.now());

          KnowledgeDomain newDomain = new KnowledgeDomain(rec.getTopic(), rec.getKeywords());

          return Mono.zip(recommendationRepository.save(rec), domainRepository.save(newDomain))
              .thenReturn(ResponseEntity.ok(ApiResponse.ok("Recommendation approved and domain created")));
        })
        .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
        .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(ApiResponse.error("Approval failed"))));
  }

  @PostMapping("/recommendations/{id}/decline")
  public Mono<ResponseEntity<ApiResponse<String>>> declineRecommendation(@PathVariable String id) {
    return recommendationRepository.findById(id)
        .flatMap(rec -> {
          rec.setStatus(KnowledgeRecommendation.Status.DECLINED);
          rec.setProcessedAt(LocalDateTime.now());
          return recommendationRepository.save(rec)
              .thenReturn(ResponseEntity.ok(ApiResponse.ok("Recommendation declined")));
        })
        .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
        .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(ApiResponse.error("Operation failed"))));
  }

  @PostMapping("/domains/{domainId}/start")
  public Mono<ResponseEntity<ApiResponse<KnowledgeDomain>>> startLearning(@PathVariable String domainId) {
    return knowledgeService.startLearning(domainId)
        .map(data -> ResponseEntity.ok(ApiResponse.ok(data)))
        .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(ApiResponse.error("Failed to start learning"))));
  }

  @PostMapping("/domains/{domainId}/process")
  public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> processLearning(
      @PathVariable String domainId) {
    return knowledgeService.processLearningJob(domainId)
        .map(data -> ResponseEntity.ok(ApiResponse.ok(data)))
        .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(ApiResponse.error("Processing failed"))));
  }

  @PostMapping("/recommendations/generate")
  public Mono<ResponseEntity<ApiResponse<List<KnowledgeRecommendation>>>> generateRecommendations() {
    return knowledgeService.generateRecommendations()
        .map(data -> ResponseEntity.ok(ApiResponse.ok(data)))
        .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(ApiResponse.error("Generation failed"))));
  }
}
