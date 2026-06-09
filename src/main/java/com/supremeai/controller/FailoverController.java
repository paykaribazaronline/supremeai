package com.supremeai.controller;

import com.supremeai.fallback.ThirdOpinionOrchestrator;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/failover")
@CrossOrigin(origins = "*")
public class FailoverController {

  private final ThirdOpinionOrchestrator thirdOpinionOrchestrator;

  public FailoverController(ThirdOpinionOrchestrator thirdOpinionOrchestrator) {
    this.thirdOpinionOrchestrator = thirdOpinionOrchestrator;
  }

  @PostMapping("/execute")
  public Mono<ResponseEntity<Map<String, String>>> executeWithFailover(
      @RequestBody Map<String, String> request) {
    String taskCategory = request.getOrDefault("taskCategory", "general");
    String errorSignature = request.getOrDefault("errorSignature", "unknown");
    String prompt = request.get("prompt");
    if (prompt == null || prompt.isEmpty()) {
      return Mono.just(ResponseEntity.badRequest().body(Map.of("error", "Missing 'prompt' field")));
    }
    return thirdOpinionOrchestrator
        .executeWithSupremeIntelligence(taskCategory, errorSignature, prompt)
        .subscribeOn(Schedulers.boundedElastic())
        .map(result -> ResponseEntity.ok(Map.of("status", "success", "result", result)))
        .onErrorResume(
            e -> Mono.just(ResponseEntity.status(500).body(Map.of("status", "failed", "error", e.getMessage()))));
  }

  @GetMapping("/providers")
  public ResponseEntity<Map<String, Object>> getProviderStatus() {
    return ResponseEntity.ok(thirdOpinionOrchestrator.getProviderHealthStatus());
  }
}
