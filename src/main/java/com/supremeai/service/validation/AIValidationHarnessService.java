package com.supremeai.service.validation;

import com.supremeai.model.APIProvider;
import com.supremeai.provider.AIProviderFactory;
import com.supremeai.repository.ProviderRepository;
import com.supremeai.service.EnhancedMultiAIConsensusService;
import com.supremeai.service.FirebaseRealtimeService;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * BV-01: AI Validation Harness Automated test suite running standard prompts against all active
 * providers + SupremeAI consensus. Results are stored in Firestore benchmark_results.
 */
@Service
public class AIValidationHarnessService {

  private static final Logger log = LoggerFactory.getLogger(AIValidationHarnessService.class);

  @Autowired private ProviderRepository providerRepository;

  @Autowired private AIProviderFactory providerFactory;

  @Autowired private EnhancedMultiAIConsensusService consensusService;

  @Autowired private FirebaseRealtimeService firebaseRealtimeService;

  // Standard prompts for validation
  private final List<ValidationPrompt> standardPrompts =
      Arrays.asList(
          new ValidationPrompt(
              "code_generation",
              "Write a Java method to reverse a string in-place without using extra memory."),
          new ValidationPrompt(
              "logic",
              "Solve this: A bat and a ball cost $1.10 in total. The bat costs $1.00 more than the ball. How much does the ball cost?"),
          new ValidationPrompt(
              "creative", "Write a 2-sentence poem about a robot learning to paint."));

  /** Run the benchmark suite weekly (every Sunday at 2 AM) */
  @Scheduled(cron = "0 0 2 * * SUN")
  public void runWeeklyBenchmark() {
    log.info("Starting weekly AI Validation Harness Benchmark...");
    runBenchmarkSuite()
        .subscribe(
            results ->
                log.info(
                    "Weekly benchmark completed successfully. Stored {} results.", results.size()),
            error -> log.error("Weekly benchmark failed", error));
  }

  /** Executes the benchmark suite. Can be triggered manually. */
  public Mono<List<Map<String, Object>>> runBenchmarkSuite() {
    String batchId = UUID.randomUUID().toString();
    long startTime = System.currentTimeMillis();

    return providerRepository
        .findAll()
        .filter(p -> "active".equalsIgnoreCase(p.getStatus()))
        .map(APIProvider::getName)
        .collectList()
        .flatMap(
            activeProviders -> {
              if (activeProviders.isEmpty()) {
                log.warn("No active providers found for benchmarking.");
                return Mono.just(Collections.emptyList());
              }

              return Flux.fromIterable(standardPrompts)
                  .flatMap(prompt -> runPromptBenchmark(prompt, activeProviders, batchId))
                  .collectList();
            });
  }

  private Mono<Map<String, Object>> runPromptBenchmark(
      ValidationPrompt prompt, List<String> activeProviders, String batchId) {
    log.info("Benchmarking prompt category: {}", prompt.category);

    Map<String, Object> resultRecord = new ConcurrentHashMap<>();
    resultRecord.put("batchId", batchId);
    resultRecord.put("category", prompt.category);
    resultRecord.put("prompt", prompt.text);
    resultRecord.put("timestamp", System.currentTimeMillis());

    List<Map<String, Object>> providerResults = Collections.synchronizedList(new ArrayList<>());

    // Run against all providers
    return Flux.fromIterable(activeProviders)
        .flatMap(
            providerName -> {
              long pStart = System.currentTimeMillis();
              return Mono.fromCallable(() -> providerFactory.getProvider(providerName))
                  .flatMap(provider -> provider.generate(prompt.text))
                  .map(
                      response -> {
                        Map<String, Object> pr = new HashMap<>();
                        pr.put("provider", providerName);
                        pr.put("responseLength", response.length());
                        pr.put("latencyMs", System.currentTimeMillis() - pStart);
                        pr.put("status", "SUCCESS");
                        return pr;
                      })
                  .onErrorResume(
                      e -> {
                        Map<String, Object> pr = new HashMap<>();
                        pr.put("provider", providerName);
                        pr.put("error", e.getMessage());
                        pr.put("status", "FAILED");
                        return Mono.just(pr);
                      });
            })
        .doOnNext(providerResults::add)
        .then(
            Mono.defer(
                () -> {
                  // Run against consensus service
                  long cStart = System.currentTimeMillis();
                  return consensusService
                      .discussAndVote(prompt.text, activeProviders, 1)
                      .map(
                          consensus -> {
                            Map<String, Object> cr = new HashMap<>();
                            cr.put("provider", "SupremeAI_Consensus");
                            cr.put("responseLength", consensus.consensusAnswer.length());
                            cr.put("latencyMs", System.currentTimeMillis() - cStart);
                            cr.put("confidence", consensus.confidence);
                            cr.put("strength", consensus.consensusStrength);
                            cr.put("status", "SUCCESS");
                            providerResults.add(cr);
                            return cr;
                          })
                      .onErrorResume(
                          e -> {
                            Map<String, Object> cr = new HashMap<>();
                            cr.put("provider", "SupremeAI_Consensus");
                            cr.put("error", e.getMessage());
                            cr.put("status", "FAILED");
                            providerResults.add(cr);
                            return Mono.just(cr);
                          });
                }))
        .then(
            Mono.defer(
                () -> {
                  resultRecord.put("results", providerResults);

                  // Save to Firestore
                  String documentId = "benchmark_results/" + batchId + "_" + prompt.category;
                  return firebaseRealtimeService
                      .setData(documentId, resultRecord)
                      .thenReturn(resultRecord);
                }));
  }

  private static class ValidationPrompt {
    String category;
    String text;

    public ValidationPrompt(String category, String text) {
      this.category = category;
      this.text = text;
    }
  }
}
