package com.supremeai.controller.analysis;

import com.supremeai.model.analysis.*;
import com.supremeai.repository.analysis.AnalysisFixRepository;
import com.supremeai.response.ApiResponse;
import com.supremeai.service.analysis.FixSuggestionService;
import com.supremeai.service.analysis.ProjectAnalysisService;
import com.supremeai.service.analysis.RAGContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private static final Logger log = LoggerFactory.getLogger(AnalysisController.class);

    private final ProjectAnalysisService projectAnalysisService;
    private final FixSuggestionService fixSuggestionService;
    private final AnalysisFixRepository fixRepository;
public AnalysisController(ProjectAnalysisService projectAnalysisService,
                             FixSuggestionService fixSuggestionService,
                             AnalysisFixRepository fixRepository) {
        this.projectAnalysisService = projectAnalysisService;
        this.fixSuggestionService = fixSuggestionService;
        this.fixRepository = fixRepository;
    }

    @PostMapping("/run")
    public Mono<ResponseEntity<ApiResponse<AnalysisResponse>>> runAnalysis(
        @RequestParam(value = "gitUrl", required = false) String gitUrl,
        @RequestParam(value = "branch", required = false, defaultValue = "main") String branch,
        @RequestParam(value = "projectType", required = false, defaultValue = "generic") String projectType,
        @RequestParam(value = "zipFile", required = false) MultipartFile zipFile,
        @RequestParam(value = "ragEnabled", required = false, defaultValue = "false") boolean ragEnabled,
        @RequestParam(value = "incrementalEnabled", required = false, defaultValue = "false") boolean incrementalEnabled,
        @RequestParam(value = "fixesEnabled", required = false, defaultValue = "true") boolean fixesEnabled,
        @RequestParam(value = "projectId", required = false) String projectId,
        @RequestParam(value = "baselineCommit", required = false) String baselineCommit,
        @RequestParam Map<String, String> allParams
    ) {
        log.info("[Analysis] Received analysis request - gitUrl: {}, hasZip: {}, rag: {}, incremental: {}, fixes: {}",
                gitUrl, zipFile != null, ragEnabled, incrementalEnabled, fixesEnabled);

        if ((gitUrl == null || gitUrl.isEmpty()) && zipFile == null) {
            return Mono.just(ResponseEntity.badRequest()
                .body(ApiResponse.error("Either gitUrl or zipFile must be provided")));
        }

        if (zipFile != null && gitUrl != null && !gitUrl.isEmpty()) {
            return Mono.just(ResponseEntity.badRequest()
                .body(ApiResponse.error("Provide either gitUrl OR zipFile, not both")));
        }

        Map<String, Boolean> agentConfig = new HashMap<>();
        allParams.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith("agents[") && entry.getKey().endsWith("]"))
            .forEach(entry -> {
                String agentKey = entry.getKey().substring(7, entry.getKey().length() - 1);
                agentConfig.put(agentKey, Boolean.parseBoolean(entry.getValue()));
            });

        if (agentConfig.isEmpty()) {
            agentConfig.put("SECURITY", true);
            agentConfig.put("QUALITY", true);
            agentConfig.put("DEPENDENCIES", true);
            agentConfig.put("ARCHITECTURE", true);
        }

        AnalysisRequest request = AnalysisRequest.builder()
            .gitUrl(gitUrl)
            .branch(branch)
            .projectType(projectType)
            .zipFile(zipFile)
            .includeDependencies(false)
            .agents(agentConfig)
            .maxFiles(1000)
            .maxSizeBytes(100L * 1024 * 1024)
            .ragEnabled(ragEnabled)
            .incrementalEnabled(incrementalEnabled)
            .fixesEnabled(fixesEnabled)
            .projectId(projectId)
            .baselineCommit(baselineCommit)
            .build();

        return Mono.fromCallable(() -> {
            try {
                AnalysisResponse response = projectAnalysisService.runAnalysis(request);
                return ResponseEntity.ok(ApiResponse.ok(response));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.<AnalysisResponse>error(e.getMessage()));
            } catch (Exception e) {
                log.error("[Analysis] Unexpected error during analysis", e);
                return ResponseEntity.status(500)
                    .body(ApiResponse.<AnalysisResponse>error("Analysis failed: " + e.getMessage()));
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/{jobId}")
    public Mono<ResponseEntity<ApiResponse<AnalysisResponse>>> getJobStatus(@PathVariable String jobId) {
        return projectAnalysisService.getJobStatus(jobId)
            .map(response -> ResponseEntity.ok(ApiResponse.ok(response)))
            .onErrorResume(e -> {
                if (e instanceof NoSuchElementException) {
                    return Mono.just(ResponseEntity.status(404)
                        .body(ApiResponse.<AnalysisResponse>error("Job not found: " + jobId)));
                }
                return Mono.just(ResponseEntity.status(500)
                    .body(ApiResponse.<AnalysisResponse>error("Failed to retrieve job: " + e.getMessage())));
            });
    }

    @DeleteMapping("/{jobId}")
    public Mono<ResponseEntity<ApiResponse<String>>> cancelJob(@PathVariable String jobId) {
        return projectAnalysisService.cancelJob(jobId)
            .map(result -> ResponseEntity.ok(ApiResponse.ok(result)))
            .onErrorResume(e -> {
                if (e instanceof NoSuchElementException) {
                    return Mono.just(ResponseEntity.status(404)
                        .body(ApiResponse.<String>error("Job not found: " + jobId)));
                }
                return Mono.just(ResponseEntity.status(500)
                    .body(ApiResponse.<String>error("Failed to cancel job: " + e.getMessage())));
            });
    }

    @GetMapping("/jobs")
    public Mono<ResponseEntity<ApiResponse<List<AnalysisJob>>>> getAllJobs() {
        return projectAnalysisService.getAllJobs()
            .collectList()
            .map(jobs -> ResponseEntity.ok(ApiResponse.ok(jobs)))
            .onErrorResume(e -> Mono.just(ResponseEntity.status(500)
                .body(ApiResponse.<List<AnalysisJob>>error("Failed to fetch jobs"))));
    }

    @GetMapping("/{jobId}/fixes")
    public Mono<ResponseEntity<ApiResponse<List<AnalysisFix>>>> getFixes(@PathVariable String jobId) {
        return fixSuggestionService.getFixesForJob(jobId)
            .collectList()
            .map(fixes -> ResponseEntity.ok(ApiResponse.ok(fixes)))
            .onErrorResume(e -> {
                log.error("[Analysis] Error fetching fixes for job {}: {}", jobId, e.getMessage());
                return Mono.just(ResponseEntity.status(500)
                    .body(ApiResponse.<List<AnalysisFix>>error("Failed to fetch fixes")));
            });
    }

    @PostMapping("/{jobId}/fixes/{fixId}/apply")
    public Mono<ResponseEntity<ApiResponse<AnalysisFix>>> applyFix(
            @PathVariable String jobId, @PathVariable String fixId) {
        return fixSuggestionService.applyFix(jobId, fixId)
            .map(fix -> ResponseEntity.ok(ApiResponse.ok(fix)))
            .onErrorResume(e -> {
                log.error("[Analysis] Error applying fix {} for job {}: {}", fixId, jobId, e.getMessage());
                return Mono.just(ResponseEntity.status(500)
                    .body(ApiResponse.<AnalysisFix>error("Failed to apply fix: " + e.getMessage())));
            });
    }

    @GetMapping("/{jobId}/context")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> getContext(
            @PathVariable String jobId,
            @RequestParam(required = false, defaultValue = "security") String query) {
        return projectAnalysisService.getJobStatus(jobId)
            .map(status -> {
                Map<String, Object> context = new HashMap<>();
                context.put("jobId", jobId);
                context.put("query", query);
                context.put("ragEnabled", status.isRagUsed());
                context.put("filesAnalyzed", status.getFilesAnalyzed());
                context.put("totalFiles", status.getTotalFiles());
                return ResponseEntity.ok(ApiResponse.ok(context));
            })
            .onErrorResume(e -> {
                if (e instanceof NoSuchElementException) {
                    return Mono.just(ResponseEntity.status(404)
                        .body(ApiResponse.<Map<String, Object>>error("Job not found: " + jobId)));
                }
                return Mono.just(ResponseEntity.status(500)
                    .body(ApiResponse.<Map<String, Object>>error("Failed to retrieve context")));
            });
    }

    @DeleteMapping("/cache/{projectId}")
    public Mono<ResponseEntity<ApiResponse<String>>> clearCache(@PathVariable String projectId) {
        return projectAnalysisService.clearProjectCache(projectId)
            .then(Mono.just(ResponseEntity.ok(ApiResponse.ok("Cache cleared for project: " + projectId))))
            .onErrorResume(e -> {
                log.error("[Analysis] Error clearing cache for project {}: {}", projectId, e.getMessage());
                return Mono.just(ResponseEntity.status(500)
                    .body(ApiResponse.<String>error("Failed to clear cache: " + e.getMessage())));
            });
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "code-analysis");
        health.put("timestamp", System.currentTimeMillis());
        health.put("agents", Map.of("SECURITY", true, "QUALITY", true, "DEPENDENCIES", true, "ARCHITECTURE", true));
        health.put("phase", "3");
        health.put("features", List.of("RAG", "Incremental Analysis", "LLM Fix Suggestions"));
        return Mono.just(ResponseEntity.ok(ApiResponse.ok(health)));
    }
}
