package com.supremeai.service.analysis;

import com.supremeai.controller.WebSocketController;
import com.supremeai.model.analysis.*;
import com.supremeai.repository.analysis.AnalysisFindingRepository;
import com.supremeai.repository.analysis.AnalysisJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.NoSuchElementException;

@Service
public class ProjectAnalysisService {
    private static final Logger log = LoggerFactory.getLogger(ProjectAnalysisService.class);

    private final List<AnalysisAgentInterface> agents;
    private final FileExtractionService fileExtractionService;
    private final AnalysisJobRepository jobRepository;
    private final AnalysisFindingRepository findingRepository;
    private final FixSuggestionService fixSuggestionService;
    private final IncrementalAnalysisService incrementalAnalysisService;
    private final DependencyGraphService dependencyGraphService;
    private final CodeChunkerService codeChunkerService;
    private final VectorSearchService vectorSearchService;
    private final RAGContextBuilder ragContextBuilder;
    private final WebSocketController webSocketController;
    private final Random random = new Random();

    private final Map<String, AnalysisJob> jobStore = new ConcurrentHashMap<>();
    private final Map<String, List<AnalysisFinding>> findingsStore = new ConcurrentHashMap<>();
public ProjectAnalysisService(List<AnalysisAgentInterface> agents,
                                  FileExtractionService fileExtractionService,
                                  AnalysisJobRepository jobRepository,
                                  AnalysisFindingRepository findingRepository,
                                  FixSuggestionService fixSuggestionService,
                                  IncrementalAnalysisService incrementalAnalysisService,
                                  DependencyGraphService dependencyGraphService,
                                  CodeChunkerService codeChunkerService,
                                  VectorSearchService vectorSearchService,
                                  RAGContextBuilder ragContextBuilder,
                                  WebSocketController webSocketController) {
        this.agents = agents;
        this.fileExtractionService = fileExtractionService;
        this.jobRepository = jobRepository;
        this.findingRepository = findingRepository;
        this.fixSuggestionService = fixSuggestionService;
        this.incrementalAnalysisService = incrementalAnalysisService;
        this.dependencyGraphService = dependencyGraphService;
        this.codeChunkerService = codeChunkerService;
        this.vectorSearchService = vectorSearchService;
        this.ragContextBuilder = ragContextBuilder;
        this.webSocketController = webSocketController;
        log.info("Initialized with {} analysis agents: {}", agents.size(),
                agents.stream().map(AnalysisAgentInterface::getCategory).collect(Collectors.joining(", ")));
    }

    public AnalysisResponse runAnalysis(AnalysisRequest request) throws Exception {
        String jobId = UUID.randomUUID().toString();
        String projectName = extractProjectName(request);
        String projectId = request.getProjectId() != null ? request.getProjectId() : projectName;

        log.info("[Analysis] Starting job {} for project: {} (rag={}, incremental={}, fixes={})",
                jobId, projectName, request.isRagEnabled(), request.isIncrementalEnabled(), request.isFixesEnabled());

        AnalysisJob job = AnalysisJob.builder()
            .id(jobId)
            .projectName(projectName)
            .projectType(request.getProjectType())
            .gitUrl(request.getGitUrl())
            .status("RUNNING")
            .startTime(Instant.now())
            .filesAnalyzed(0)
            .totalFindings(0)
            .findingsBySeverity(Map.of("CRITICAL", 0, "HIGH", 0, "MEDIUM", 0, "LOW", 0, "INFO", 0))
            .completed(false)
            .initiatedBy("admin")
            .build();

        jobStore.put(jobId, job);
        findingsStore.put(jobId, new ArrayList<>());
        saveJobToFirestore(job);

        // Broadcast job started
        webSocketController.broadcastAnalysisProgress(jobId, projectName, "STARTED",
            0, 0, "N/A", 0, "Analysis job started");

        long startTime = System.currentTimeMillis();
        List<File> filesToAnalyze;
        String tempDir = null;
        boolean incrementalUsed = false;
        boolean ragUsed = false;
        int changedFiles = 0;
        int cachedFindingsCount = 0;
        List<AnalysisFinding> cachedFindings = new ArrayList<>();

        try {
            if (request.getGitUrl() != null && !request.getGitUrl().isEmpty()) {
                filesToAnalyze = fileExtractionService.checkoutFromGit(request.getGitUrl(), request.getBranch());
                tempDir = extractTempDir(request.getGitUrl());
            } else if (request.getZipFile() != null && !request.getZipFile().isEmpty()) {
                filesToAnalyze = fileExtractionService.extractFromZip(request.getZipFile());
            } else {
                throw new IllegalArgumentException("Either gitUrl or zipFile must be provided");
            }

            if (filesToAnalyze.isEmpty()) {
                throw new IllegalStateException("No source files found for analysis");
            }

            int totalFiles = filesToAnalyze.size();
            log.info("[Analysis] Job {}: {} files collected for scanning", jobId, totalFiles);

            webSocketController.broadcastAnalysisProgress(jobId, projectName, "FILES_COLLECTED",
                totalFiles, totalFiles, "N/A", 0, "Collected " + totalFiles + " files");

            if (request.isIncrementalEnabled() && request.getGitUrl() != null) {
                try {
                    IncrementalAnalysisService.IncrementalAnalysisPlan plan =
                            incrementalAnalysisService.createPlan(projectId, tempDir);

                    if (!plan.isFullAnalysis()) {
                        incrementalUsed = true;
                        changedFiles = plan.getChangedFiles().size();
                        cachedFindings = plan.getCachedFindings();
                        cachedFindingsCount = cachedFindings.size();

                        filesToAnalyze = incrementalAnalysisService.filterFilesToAnalyze(filesToAnalyze, plan);
                        log.info("[Analysis] Job {}: Incremental mode - analyzing {} of {} files ({} cached)",
                                jobId, filesToAnalyze.size(), totalFiles, cachedFindingsCount);
                    }
                } catch (Exception e) {
                    log.warn("[Analysis] Job {}: Incremental analysis failed, falling back to full: {}", jobId, e.getMessage());
                }
            }

            if (request.isRagEnabled()) {
                try {
                    for (File file : filesToAnalyze) {
                        String relativePath = getRelativePath(file, request.getProjectType());
                        List<CodeChunkData> chunks = codeChunkerService.chunkFile(file, relativePath);
                        vectorSearchService.storeEmbeddings(projectId, chunks).block();
                    }
                    ragUsed = true;
                    log.info("[Analysis] Job {}: RAG embeddings stored for {} files", jobId, filesToAnalyze.size());
                    webSocketController.broadcastAnalysisProgress(jobId, projectName, "RAG_EMBEDDINGS",
                        filesToAnalyze.size(), totalFiles, "RAG", 0, "Stored embeddings for " + filesToAnalyze.size() + " files");
                } catch (Exception e) {
                    log.warn("[Analysis] Job {}: RAG embedding failed, continuing without: {}", jobId, e.getMessage());
                }
            }

            if (!incrementalUsed) {
                try {
                    dependencyGraphService.buildDependencyGraph(projectId, filesToAnalyze).block();
                } catch (Exception e) {
                    log.warn("[Analysis] Job {}: Dependency graph build failed: {}", jobId, e.getMessage());
                }
            }

            List<AnalysisFinding> newFindings = new ArrayList<>();
            AtomicInteger processedFiles = new AtomicInteger(0);
            AtomicInteger lastReportedPercent = new AtomicInteger(0);
            final int scanTotalFiles = filesToAnalyze.size();  // capture for lambda

            List<AnalysisAgentInterface> enabledAgents = agents.stream()
                .filter(agent -> isAgentEnabled(agent, request))
                .collect(Collectors.toList());

            log.info("[Analysis] Job {}: Running {} agents in parallel: {}", jobId, enabledAgents.size(),
                    enabledAgents.stream().map(AnalysisAgentInterface::getCategory).collect(Collectors.joining(", ")));

            Flux.fromIterable(filesToAnalyze)
                .doOnNext(file -> {
                    int processed = processedFiles.incrementAndGet();
                    int percent = (int) (((double) processed / scanTotalFiles) * 100);

                    // Emit progress every 5% or on first/last file
                    if (percent >= lastReportedPercent.get() + 5 || processed == 1 || processed == scanTotalFiles) {
                        lastReportedPercent.set(percent);
                        String agentName = enabledAgents.isEmpty() ? "N/A" : enabledAgents.get(0).getCategory();
                        webSocketController.broadcastAnalysisProgress(jobId, projectName, "SCANNING",
                            processed, scanTotalFiles, agentName, newFindings.size(),
                            String.format("Scanned %d/%d files (%d%%)", processed, scanTotalFiles, percent));
                    }
                })
                .flatMap(file -> {
                    String relativePath = getRelativePath(file, request.getProjectType());

                    List<Flux<AnalysisFinding>> agentFluxes = enabledAgents.stream()
                        .map(agent -> agent.scanFile(file, relativePath)
                            .doOnComplete(() -> {
                                // Optional: Could emit per-agent completion here if needed
                            })
                        )
                        .collect(Collectors.toList());

                    return Flux.merge(agentFluxes);
                })
                .doOnNext(finding -> {
                    finding.setJobId(jobId);
                    newFindings.add(finding);
                    synchronized (findingsStore) {
                        findingsStore.get(jobId).add(finding);
                    }
                })
                .then()
                .subscribeOn(Schedulers.parallel())
                .block();

            List<AnalysisFinding> allFindings;
            if (incrementalUsed && !cachedFindings.isEmpty()) {
                allFindings = incrementalAnalysisService.mergeFindings(newFindings, cachedFindings);
                log.info("[Analysis] Job {}: Merged {} new + {} cached = {} total findings",
                        jobId, newFindings.size(), cachedFindingsCount, allFindings.size());
            } else {
                allFindings = newFindings;
            }

            saveFindingsToFirestore(jobId, allFindings);

            if (incrementalUsed) {
                try {
                    String currentCommit = incrementalAnalysisService.getClass()
                        .getDeclaredField("gitDiffService") != null ? "HEAD" : "HEAD";
                    incrementalAnalysisService.updateBaseline(projectId, "HEAD", allFindings).block();
                } catch (Exception e) {
                    log.warn("[Analysis] Job {}: Failed to update baseline: {}", jobId, e.getMessage());
                }
            }

            List<AnalysisFix> fixes = new ArrayList<>();
            if (request.isFixesEnabled()) {
                try {
                    webSocketController.broadcastAnalysisProgress(jobId, projectName, "GENERATING_FIXES",
                        processedFiles.get(), totalFiles, "FixGenerator", newFindings.size(),
                        "Generating fix suggestions for " + newFindings.size() + " findings");
                    fixes = fixSuggestionService.generateFixes(jobId, allFindings).block();
                    log.info("[Analysis] Job {}: Generated {} fix suggestions", jobId, fixes.size());
                } catch (Exception e) {
                    log.warn("[Analysis] Job {}: Fix generation failed: {}", jobId, e.getMessage());
                }
            }

            long durationMs = System.currentTimeMillis() - startTime;
            job.setEndTime(Instant.now());
            job.setDurationMs(durationMs);
            job.setFilesAnalyzed(processedFiles.get());
            job.setTotalFindings(allFindings.size());
            job.setCompleted(true);
            job.setStatus("COMPLETED");

            Map<String, Integer> summary = allFindings.stream()
                .collect(Collectors.groupingBy(
                    AnalysisFinding::getSeverity,
                    Collectors.summingInt(f -> 1)
                ));

            for (String severity : List.of("CRITICAL", "HIGH", "MEDIUM", "LOW", "INFO")) {
                summary.putIfAbsent(severity, 0);
            }

            job.setFindingsBySeverity(summary);
            jobStore.put(jobId, job);
            saveJobToFirestore(job);

            // Broadcast completion
            webSocketController.broadcastAnalysisCompletion(jobId, projectName, allFindings.size(), summary, durationMs);

            log.info("[Analysis] Job {} completed in {}ms. Found {} issues. Fixes: {}",
                jobId, durationMs, allFindings.size(), fixes.size());

            return AnalysisResponse.builder()
                .jobId(jobId)
                .status(job.getStatus())
                .durationMs(durationMs)
                .project(job.getProjectName())
                .filesAnalyzed(processedFiles.get())
                .totalFiles(totalFiles)
                .totalFindings(allFindings.size())
                .summary(summary)
                .findings(allFindings)
                .fixes(fixes)
                .completed(true)
                .ragUsed(ragUsed)
                .incrementalUsed(incrementalUsed)
                .changedFiles(changedFiles)
                .cachedFindings(cachedFindingsCount)
                .build();

        } catch (Exception e) {
            log.error("[Analysis] Job {} failed: {}", jobId, e.getMessage(), e);
            webSocketController.broadcastAnalysisProgress(jobId, projectName, "FAILED",
                0, 0, "N/A", 0, e.getMessage());
            job.setStatus("FAILED");
            job.setErrorMessage(e.getMessage());
            job.setEndTime(Instant.now());
            job.setDurationMs(System.currentTimeMillis() - startTime);
            jobStore.put(jobId, job);
            saveJobToFirestore(job);
            throw e;
        }
    }

    public Mono<AnalysisResponse> getJobStatus(String jobId) {
        AnalysisJob job = jobStore.get(jobId);
        if (job == null) {
            return Mono.error(new NoSuchElementException("Job not found: " + jobId));
        }

        List<AnalysisFinding> findings = findingsStore.getOrDefault(jobId, List.of());
        return Mono.just(buildResponse(jobId, job, findings, List.of()));
    }

    public Mono<String> cancelJob(String jobId) {
        AnalysisJob job = jobStore.remove(jobId);
        findingsStore.remove(jobId);
        if (job != null) {
            jobRepository.deleteById(jobId).subscribe();
            log.info("[Analysis] Job {} cancelled and deleted", jobId);
            return Mono.just("Job cancelled successfully");
        }
        return Mono.error(new NoSuchElementException("Job not found: " + jobId));
    }

    public Flux<AnalysisJob> getAllJobs() {
        return Flux.fromIterable(jobStore.values().stream()
            .sorted(Comparator.comparing(AnalysisJob::getStartTime).reversed())
            .collect(Collectors.toList()));
    }

    public RAGContextBuilder.RAGContext getRAGContext(String projectId, String query, List<String> files) {
        return ragContextBuilder.buildContext(projectId, query, files);
    }

    public Mono<Void> clearProjectCache(String projectId) {
        return vectorSearchService.clearProjectEmbeddings(projectId)
            .then(dependencyGraphService.clearProjectGraph(projectId))
            .then(incrementalAnalysisService.clearBaseline(projectId))
            .doOnSuccess(v -> log.info("Cleared all caches for project {}", projectId));
    }

    private boolean isAgentEnabled(AnalysisAgentInterface agent, AnalysisRequest request) {
        Map<String, Boolean> agentConfig = request.getAgents();
        if (agentConfig == null || agentConfig.isEmpty()) {
            return agent.isEnabled();
        }
        Boolean enabled = agentConfig.get(agent.getCategory());
        return enabled != null ? enabled : agent.isEnabled();
    }

    private String getRelativePath(File file, String projectType) {
        return file.getName();
    }

    private String extractProjectName(AnalysisRequest request) {
        if (request.getGitUrl() != null) {
            String[] parts = request.getGitUrl().split("/");
            return parts[parts.length - 1].replace(".git", "");
        }
        return "uploaded_project";
    }

    private String extractTempDir(String gitUrl) {
        return System.getProperty("java.io.tmpdir");
    }

    private AnalysisResponse buildResponse(String jobId, AnalysisJob job, List<AnalysisFinding> findings, List<AnalysisFix> fixes) {
        return AnalysisResponse.builder()
            .jobId(jobId)
            .status(job.getStatus())
            .durationMs(job.getDurationMs())
            .project(job.getProjectName())
            .filesAnalyzed(job.getFilesAnalyzed())
            .totalFiles(job.getFilesAnalyzed())
            .totalFindings(job.getTotalFindings())
            .summary(job.getFindingsBySeverity())
            .findings(findings)
            .fixes(fixes)
            .completed(job.isCompleted())
            .errorMessage(job.getErrorMessage())
            .build();
    }

    private void saveJobToFirestore(AnalysisJob job) {
        try {
            jobRepository.save(job).subscribe();
            log.debug("[Firestore] Saved job: {}", job.getId());
        } catch (Exception e) {
            log.error("[Firestore] Failed to save job {}: {}", job.getId(), e.getMessage());
        }
    }

    private void saveFindingsToFirestore(String jobId, List<AnalysisFinding> findings) {
        try {
            findings.forEach(f -> f.setJobId(jobId));
            findingRepository.saveAll(findings).blockLast();
            log.debug("[Firestore] Saved {} findings for job {}", findings.size(), jobId);
        } catch (Exception e) {
            log.error("[Firestore] Failed to save findings for job {}: {}", jobId, e.getMessage());
        }
    }
}
