package com.supremeai.controller;

import com.supremeai.repository.*;
import com.supremeai.response.ApiResponse;
import com.supremeai.service.AdminDashboardFacadeService;
import com.supremeai.service.ContextualAIRankingService;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * AdminDashboardController - Core Admin Dashboard API.
 *
 * <p>After refactoring Phase 4: - User management moved to AdminUserManagementController -
 * Improvement proposals moved to AdminImprovementController - Prompt engineering moved to
 * AdminPromptController
 */
@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController extends BaseAdminController<Object, String> {

  private static final Logger log = LoggerFactory.getLogger(AdminDashboardController.class);

  private final AdminDashboardFacadeService facadeService;
  private final SolutionMemoryRepository solutionMemoryRepository;
  private final ContextualAIRankingService contextualRankingService;
  private final com.supremeai.service.ChatProcessingService chatProcessingService;

  @Autowired
  public AdminDashboardController(
      AdminDashboardFacadeService facadeService,
      SolutionMemoryRepository solutionMemoryRepository,
      ContextualAIRankingService contextualRankingService,
      com.supremeai.service.ChatProcessingService chatProcessingService) {
    this.facadeService = facadeService;
    this.solutionMemoryRepository = solutionMemoryRepository;
    this.contextualRankingService = contextualRankingService;
    this.chatProcessingService = chatProcessingService;
  }

  @GetMapping("/plans")
  public Mono<ResponseEntity<Map<String, Object>>> getPlans(
      @RequestParam(defaultValue = "true") boolean active_only) {
    return chatProcessingService
        .getPlans(active_only)
        .map(plans -> ResponseEntity.ok(Map.of("success", true, "plans", plans)));
  }

  @GetMapping("/dashboard/contract")
  public Mono<ApiResponse<Map<String, Object>>> getContract() {
    return Mono.zipDelayError(
            facadeService.buildContractDataSources(), data -> facadeService.buildContract(data))
        .map(ApiResponse::ok)
        .onErrorResume(
            e -> {
              log.error("Failed to build dashboard contract: {}", e.getMessage());
              return Mono.just(ApiResponse.ok(facadeService.buildDefaultContract()));
            });
  }

  /** Mark a solution memory as obsolete (soft-delete) to unlearn it. */
  @PostMapping("/knowledge/obsolete/{solutionId}")
  public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> markSolutionObsolete(
      @PathVariable String solutionId, @RequestBody Map<String, String> request) {
    String reason = request.getOrDefault("reason", "No reason provided");

    return solutionMemoryRepository
        .findById(solutionId)
        .flatMap(
            solution -> {
              solution.markObsolete(reason);
              return solutionMemoryRepository.save(solution);
            })
        .map(
            updated -> {
              Map<String, Object> res = new HashMap<>();
              res.put("status", "obsoleted");
              res.put("solutionId", updated.getId());
              res.put("reason", updated.getObsoleteReason());
              return ResponseEntity.ok(ApiResponse.ok(res));
            })
        .defaultIfEmpty(
            ResponseEntity.status(404)
                .body(ApiResponse.error("Solution not found", Map.of("solutionId", solutionId))))
        .onErrorResume(
            e -> {
              log.error("Failed to obsolete solution {}: {}", solutionId, e.getMessage());
              return Mono.just(
                  ResponseEntity.status(500)
                      .body(
                          ApiResponse.<Map<String, Object>>error(
                              "Failed to obsolete solution: " + e.getMessage())));
            });
  }

  /** Get AI provider rankings based on success rates. */
  @GetMapping("/providers/rankings")
  public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> getProviderRankings() {
    return Mono.just(
        ResponseEntity.ok(
            ApiResponse.ok(
                Map.of(
                    "rankings", contextualRankingService.getStatistics(),
                    "timestamp", System.currentTimeMillis()))));
  }
}
