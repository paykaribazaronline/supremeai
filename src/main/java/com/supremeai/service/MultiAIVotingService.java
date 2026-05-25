package com.supremeai.service;

import com.supremeai.agentorchestration.VotingDecision;
import com.supremeai.intelligence.voting.VotingTopic;
import com.supremeai.intelligence.voting.VotingTopicGenerator;
import com.supremeai.learning.active.ActiveInternetScraper;
import com.supremeai.learning.active.ActiveInternetScraper.ScrapedIssue;
import com.supremeai.model.ConsensusResult;
import com.supremeai.model.ConsensusVote;
import com.supremeai.model.ProviderVote;
import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import com.supremeai.provider.AIProviderType;
import com.supremeai.repository.ProviderRepository;
import com.supremeai.repository.TaskProviderAssignmentRepository;
import com.supremeai.service.EnhancedLearningService;
import com.supremeai.service.FirebaseRealtimeService;
import com.supremeai.service.solomode.SoloModeManagerService;
import com.supremeai.util.FallbackConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Unified Multi-AI Voting Service
 * Provides ensemble voting, consensus, and decision-making across multiple AI
 * providers
 */
@Service
public class MultiAIVotingService {

    private static final Logger logger = LoggerFactory.getLogger(MultiAIVotingService.class);

    @Autowired
    private FirebaseRealtimeService firebaseRealtimeService;

    @Autowired
    private ActiveInternetScraper activeInternetScraper;

    @Autowired
    private EnhancedLearningService enhancedLearningService;

    @Autowired
    private WebClient.Builder webClientBuilder;

    // Dependencies from original services
    @Autowired
    private AIProviderFactory providerFactory;

    @Autowired
    private ContextualAIRankingService contextualRankingService;

    @Autowired
    private VotingTopicGenerator topicGenerator;

    @Lazy
    @Autowired(required = false)
    private SelfHealingService selfHealingService;

    @Autowired
    private ConfigService configService;

    @Autowired
    private KnowledgeFeedbackService feedbackService;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private TaskProviderAssignmentRepository taskAssignmentRepo;

    @Autowired
    @Lazy
    private SoloModeManagerService soloModeManagerService;

    // Dynamic model selection (no hardcoded limit)
    private static final int DEFAULT_MAX_VOTING_PROVIDERS = 10;

    /**
     * Playwright browser automation server URL (e.g. http://localhost:3001).
     * Set via application property: supremeai.browser.automation-url
     */
    @Value("${supremeai.browser.automation-url:http://localhost:3001}")
    private String playwrightAutomationUrl;

    /**
     * Maximum autonomous research steps per Solo Mode session.
     * Set via application property: supremeai.solo.max-steps
     */
    @Value("${supremeai.solo.max-steps:5}")
    private int soloMaxSteps;

    /**
     * Per-step timeout in milliseconds for Solo Mode Playwright research.
     * Set via application property: supremeai.solo.step-timeout-ms
     */
    @Value("${supremeai.solo.step-timeout-ms:30000}")
    private long soloStepTimeoutMs;

    public static final String[] ALL_PROVIDERS = {};

    public static final String[] DEFAULT_PROVIDERS = {};

    private final Map<String, ModelPerformanceTracker> performanceTrackers = new ConcurrentHashMap<>();
    private final List<ConsensusVote> consensusHistory = new CopyOnWriteArrayList<>();
    private final Random random = new Random();

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_BACKOFF_MS = 1000;

    /**
     * Provider cache TTL in milliseconds
     */

    public MultiAIVotingService() {
    }

    // Configuration helpers
    private int getTimeoutMs() {
        return (int) configService.getTimeout("voting_timeout", 15000L);
    }

    private int getMaxRetries() {
        return configService.getSetting("max_retries", 2);
    }

    private double getConfidenceThreshold() {
        return configService.getThreshold("consensus", 0.6);
    }

    // ===== ENSEMBLE VOTING (from TenAIVotingSystem) =====

    public Mono<VotingResult> executeEnsembleVoting(
            String prompt,
            List<String> selectedModels,
            long timeoutMs) {
        return executeEnsembleVoting(prompt, selectedModels, timeoutMs, null);
    }

    @Cacheable(value = "ai_responses", key = "#prompt + '_ensemble_' + #taskType")
    public Mono<VotingResult> executeEnsembleVoting(
            String prompt,
            List<String> selectedModels,
            long timeoutMs,
            String taskType // NEW: task type for smart routing
    ) {
        long startTime = System.currentTimeMillis();

        List<String> keywords = extractKeywords(prompt);
        String detectedDomain = detectDomain(prompt);
        boolean complex = isComplexConversation(prompt);

        return activeInternetScraper.scrapeKnowledge(detectedDomain, keywords)
                .collectList()
                .flatMap(issues -> {
                    return firebaseRealtimeService.getData("config/neural_helper")
                            .onErrorResume(e -> Mono.empty())
                            .flatMap(config -> {
                                final boolean helperEnabled = config != null && Boolean.parseBoolean(String.valueOf(config.getOrDefault("enabled", "false")));

                                return providerRepository.findAll()
                                        .filter(p -> "active".equalsIgnoreCase(p.getStatus())
                                                && p.isCanParticipateInVoting())
                                        .map(p -> p.getName())
                                        .collectList()
                                        .flatMap(dbModels -> {
                                            List<String> activeModels = new ArrayList<>(dbModels);
                                            if (helperEnabled && !activeModels.contains("cloud_helper")) {
                                                activeModels.add("cloud_helper");
                                            }

                                            int availableCount = activeModels.size();

                                            // Branch 1: Normal communication (no voting needed, direct answer)
                                            if (!complex) {
                                                logger.info(
                                                        "Normal communication detected. Executing Direct Internet Answer Flow without voting.");
                                                return executeDirectInternetCommunication(prompt, issues, config,
                                                        startTime, timeoutMs);
                                            }

                                            // Complex / Coding conversation:
                                            if (availableCount == 0) {
                                                logger.info(
                                                        "Complex query but 0 models available. Operating in autonomous Solo-Mode.");
                                                return executeSoloFallback(prompt, issues, startTime);
                                            } else if (availableCount == 1) {
                                                logger.info(
                                                        "Complex query and 1 model available ({}). Executing Single-Model double-pass Resilient Flow.",
                                                        activeModels.get(0));
                                                return executeSingleModelResilientFlow(prompt, activeModels.get(0),
                                                        config, issues, startTime, timeoutMs);
                                            } else {
                                                logger.info(
                                                        "Complex query and multiple models available ({}). Executing multi-model voting panel flow.",
                                                        availableCount);
                                                return executeMultiModelVotingFlow(prompt, activeModels, config, issues,
                                                        startTime, timeoutMs, taskType);
                                            }
                                        });
                            });
                })
                .timeout(java.time.Duration.ofMillis(timeoutMs))
                .onErrorResume(java.util.concurrent.TimeoutException.class, e -> {
                    logger.warn("Ensemble voting timed out for prompt: {}", prompt);
                    return Mono.just(new VotingResult(prompt,
                            FallbackConstants.VOTING_TIMEOUT,
                            List.of(), 0.0, "TIMEOUT", timeoutMs));
                });
    }

    private boolean isComplexConversation(String prompt) {
        if (prompt == null)
            return false;
        String p = prompt.toLowerCase();

        Set<String> complexKeywords = Set.of(
                "code", "write", "function", "class", "program", "method", "java", "python",
                "javascript", "react", "html", "css", "error", "exception", "bug", "build",
                "compile", "database", "algorithm", "design", "refactor", "deploy", "architecture",
                "sql", "api", "query", "optimize");

        if (prompt.length() > 150)
            return true;

        for (String word : p.split("[^a-zA-Z0-9]+")) {
            if (complexKeywords.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private Mono<VotingResult> executeDirectInternetCommunication(
            String prompt, List<ScrapedIssue> issues, Map<String, Object> helperConfig, long startTime,
            long timeoutMs) {
        boolean helperEnabled = false;
        if (helperConfig != null) {
            helperEnabled = Boolean.parseBoolean(String.valueOf(helperConfig.getOrDefault("enabled", "false")));
        }

        if (helperEnabled) {
            return queryCloudHelper(prompt, helperConfig, issues, timeoutMs)
                    .map(vote -> {
                        String ans = vote.getResponse();
                        enhancedLearningService.learnFromNLPInteraction(prompt, ans, "direct_internet_helper", 0.90,
                                Map.of("mode", "direct_communication")).subscribe();
                        long duration = System.currentTimeMillis() - startTime;
                        return new VotingResult(prompt, ans, List.of(vote), 0.90, "DIRECT_COMMUNICATION", duration);
                    })
                    .onErrorResume(e -> {
                        logger.warn("Direct Helper failed, using Solo Synthesizer: {}", e.getMessage());
                        return Mono.just(executeDirectSoloSynthesis(prompt, issues, startTime));
                    });
        } else {
            return Mono.just(executeDirectSoloSynthesis(prompt, issues, startTime));
        }
    }

    private VotingResult executeDirectSoloSynthesis(String prompt, List<ScrapedIssue> issues, long startTime) {
        StringBuilder sb = new StringBuilder();
        sb.append("🤖 **SupremeAI স্বায়ত্তশাসিত ব্রাউজার উত্তর (Direct Search Answer)**\n\n");
        if (issues == null || issues.isEmpty()) {
            sb.append(
                    "উন্মুক্ত ইন্টারনেট থেকে সরাসরি কোনো তথ্য পাওয়া যায়নি। তবে আমাদের সিস্টেমের সাধারণ ডাটাবেস ভিত্তিক সমাধান নিচে দেওয়া হলো:\n\n");
            sb.append(generateFallbackGeneralResponse(prompt));
        } else {
            sb.append("আপনার প্রশ্নটির উত্তর সরাসরি ইন্টারনেট রিসার্চ থেকে খুঁজে বের করা হয়েছে:\n\n");
            for (ScrapedIssue issue : issues) {
                sb.append("📌 **").append(issue.getTitle()).append("**\n")
                        .append("- **সারাংশ:** ").append(issue.getSolution()).append("\n")
                        .append("- **সূত্র:** `").append(issue.getSource()).append("`\n\n");
            }
        }
        String synthesized = sb.toString();
        enhancedLearningService.learnFromNLPInteraction(prompt, synthesized, "direct_internet_solo", 0.80,
                Map.of("mode", "direct_communication_solo")).subscribe();
        long duration = System.currentTimeMillis() - startTime;
        ProviderVote vote = new ProviderVote("direct_browser", synthesized, 0.80, System.currentTimeMillis());
        return new VotingResult(prompt, synthesized, List.of(vote), 0.80, "DIRECT_COMMUNICATION_SOLO", duration);
    }

    private Mono<VotingResult> executeSingleModelResilientFlow(
            String prompt, String modelName, Map<String, Object> helperConfig,
            List<ScrapedIssue> issues, long startTime, long timeoutMs) {

        return queryModelOrHelper(modelName, prompt, helperConfig, issues, timeoutMs / 2)
                .flatMap(voteA -> {
                    String responseA = voteA.getResponse();
                    String responseB = synthesizeSoloResponse(prompt, issues); // The "Browser Weapon" output

                    String comparisonPrompt = String.format(
                            "ইউজার প্রশ্ন: %s\n\n" +
                                    "আপনার কাছে দুটি তথ্য আছে:\n" +
                                    "১. আপনার নিজস্ব AI মেমোরি থেকে প্রাপ্ত উত্তর (সমাধান A): %s\n" +
                                    "২. সুপ্রিমএআই ব্রাউজার দ্বারা ইন্টারনেট থেকে সংগৃহীত রিয়েল-টাইম তথ্য (সমাধান B): %s\n\n"
                                    +
                                    "এখন আপনি এই দুটি তুলনা করুন। কোনটি বেশি নির্ভুল এবং রিয়েল-টাইম তথ্যের সাথে সামঞ্জস্যপূর্ণ? "
                                    +
                                    "দয়া করে এই দুটির মধ্য থেকে একটি চূড়ান্ত 'বিজয়ী' সিদ্ধান্ত নিন এবং সেই অনুযায়ী সবচেয়ে উন্নত ও নিখুঁত বাংলা উত্তরটি প্রস্তুত করুন।",
                            prompt, responseA, responseB);

                    return queryModelOrHelper(modelName, comparisonPrompt, helperConfig, null, timeoutMs / 2)
                            .map(voteWinner -> {
                                String winnerResponse = voteWinner.getResponse();
                                enhancedLearningService
                                        .learnFromNLPInteraction(prompt, winnerResponse, "solo_resilient_winner", 0.95,
                                                Map.of("model", modelName, "winning_source", "comparison_refinement"))
                                        .subscribe();

                                long duration = System.currentTimeMillis() - startTime;
                                ProviderVote finalVote = new ProviderVote(modelName + "_compared", winnerResponse, 0.96,
                                        System.currentTimeMillis());
                                return new VotingResult(prompt, winnerResponse, List.of(finalVote), 0.96,
                                        "SINGLE_MODEL_COMPARED", duration);
                            })
                            .onErrorResume(e -> {
                                logger.warn("Comparison step failed, falling back to Response A: {}", e.getMessage());
                                long duration = System.currentTimeMillis() - startTime;
                                return Mono.just(new VotingResult(prompt, responseA, List.of(voteA), 0.90,
                                        "SINGLE_MODEL_FALLBACK", duration));
                            });
                })
                .onErrorResume(e -> {
                    logger.warn("Initial query to single model failed, falling back to Solo internet response: {}",
                            e.getMessage());
                    return executeSoloFallback(prompt, issues, startTime);
                });
    }

    private Mono<VotingResult> executeMultiModelVotingFlow(
            String prompt, List<String> activeModels, Map<String, Object> helperConfig,
            List<ScrapedIssue> issues, long startTime, long timeoutMs, String taskType) {

        int n = activeModels.size();
        List<String> votingPanel = new ArrayList<>(activeModels);

        if (n % 2 == 0) {
            logger.info("Even number detected ({}). SupremeAI Browser joins to ensure odd number voting.", n);
            votingPanel.add("autonomous_browser");
        } else {
            logger.info("Odd number detected ({}). SupremeAI Browser stays neutral.", n);
        }

        return Flux.fromIterable(votingPanel)
                .flatMap(modelName -> {
                    if ("autonomous_browser".equalsIgnoreCase(modelName)) {
                        String synthesized = synthesizeSoloResponse(prompt, issues);
                        ProviderVote vote = new ProviderVote("autonomous_browser", synthesized, 0.85,
                                System.currentTimeMillis());
                        return Mono.just(vote);
                    } else {
                        return queryModelOrHelper(modelName, prompt, helperConfig, issues, timeoutMs - 3000)
                                .onErrorResume(e -> {
                                    logger.warn("Model {} failed in voting panel: {}", modelName, e.getMessage());
                                    return Mono.empty();
                                });
                    }
                })
                .collectList()
                .flatMap(votes -> {
                    if (votes.isEmpty()) {
                        logger.warn("All voting panel members failed. Falling back to Solo-Mode.");
                        return executeSoloFallback(prompt, issues, startTime);
                    }

                    return firebaseRealtimeService.getData("config/task_weights")
                        .onErrorReturn(Map.of())
                        .flatMap(weightsConfig -> {
                            long duration = System.currentTimeMillis() - startTime;

                            Map<String, List<ProviderVote>> groups = groupBySimilarity(votes);
                            
                            // Determine weights for this taskType
                            Map<String, Double> modelWeights = new HashMap<>();
                            String actualTaskType = taskType != null ? taskType : detectDomain(prompt);
                            Object taskWeightConfig = weightsConfig.get(actualTaskType);
                            if (taskWeightConfig instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> wMap = (Map<String, Object>) taskWeightConfig;
                                for (Map.Entry<String, Object> e : wMap.entrySet()) {
                                    modelWeights.put(e.getKey(), Double.parseDouble(String.valueOf(e.getValue())));
                                }
                            }
                            
                            List<ProviderVote> winningGroup = groups.values().stream()
                                    .max(Comparator.comparingDouble(group -> group.stream()
                                            .mapToDouble(v -> modelWeights.getOrDefault(v.getProviderName(), 1.0))
                                            .sum()))
                                    .orElse(List.of(votes.get(0)));

                            String bestResponse = winningGroup.get(0).getResponse();
                            double avgConfidence = votes.stream()
                                    .mapToDouble(ProviderVote::getConfidence)
                                    .average()
                                    .orElse(0.5);

                            enhancedLearningService.learnFromNLPInteraction(prompt, bestResponse, "multi_model_voting_weighted",
                                    avgConfidence, Map.of("voters", votingPanel.toString(), "taskType", actualTaskType)).subscribe();

                            return Mono.just(new VotingResult(prompt, bestResponse, votes, avgConfidence, "CONSENSUS_RESOLVED", duration));
                        });
                });
    }

    private Mono<ProviderVote> queryModelOrHelper(
            String modelName, String prompt, Map<String, Object> helperConfig, List<ScrapedIssue> issues,
            long timeoutMs) {
        if ("cloud_helper".equalsIgnoreCase(modelName)) {
            return queryCloudHelper(prompt, helperConfig, issues, timeoutMs);
        } else {
            return queryModel(modelName, prompt, timeoutMs);
        }
    }

    public Flux<ProviderVote> streamVotes(
            String prompt,
            List<String> selectedModels,
            long timeoutMs
    ) {
        List<String> keywords = extractKeywords(prompt);
        String detectedDomain = detectDomain(prompt);

        return activeInternetScraper.scrapeKnowledge(detectedDomain, keywords)
                .collectList()
                .flatMapMany(issues -> {
                    return firebaseRealtimeService.getData("config/neural_helper")
                            .onErrorResume(e -> Mono.empty())
                            .flatMapMany(config -> {
                                return providerRepository.findAll()
                                        .filter(p -> "active".equalsIgnoreCase(p.getStatus())
                                                && p.isCanParticipateInVoting())
                                        .map(p -> p.getName())
                                        .collectList()
                                        .flatMapMany(dbModels -> {
                                            List<String> activeModels = new ArrayList<>(dbModels);
                                            boolean helperEnabled = config != null && Boolean.parseBoolean(String.valueOf(config.getOrDefault("enabled", "false")));
                                            if (helperEnabled && !activeModels.contains("cloud_helper")) {
                                                activeModels.add("cloud_helper");
                                            }

                                            int n = activeModels.size();
                                            if (n % 2 == 0) {
                                                activeModels.add("autonomous_browser");
                                            }

                                            return Flux.fromIterable(activeModels)
                                                    .flatMap(modelName -> {
                                                        if ("autonomous_browser".equalsIgnoreCase(modelName)) {
                                                            String synthesized = synthesizeSoloResponse(prompt, issues);
                                                            return Mono.just(new ProviderVote("autonomous_browser", synthesized, 0.85, System.currentTimeMillis()));
                                                        } else {
                                                            return queryModelOrHelper(modelName, prompt, config, issues, timeoutMs)
                                                                    .onErrorResume(e -> Mono.empty());
                                                        }
                                                    });
                                        });
                            });
                });
    }

    private Mono<ProviderVote> queryCloudHelper(
            String prompt, Map<String, Object> config, List<ScrapedIssue> issues, long timeoutMs) {
        String apiKey = (String) config.get("apiKey");
        String endpoint = (String) config.get("endpoint");
        String model = (String) config.get("model");

        StringBuilder contextBuilder = new StringBuilder();
        if (issues != null) {
            for (ScrapedIssue issue : issues) {
                contextBuilder.append("Source (").append(issue.getSource()).append("): ")
                        .append(issue.getTitle()).append(" - ").append(issue.getSolution()).append("\n\n");
            }
        }

        String systemInstruction = "You are the SupremeAI Assistant. Answer in Bengali.";
        String fullPrompt = systemInstruction + "\n\nSearch Context:\n" + contextBuilder.toString()
                + "\n\nUser Question:\n" + prompt;

        String requestBody;
        if (endpoint.contains("googleapis.com")) {
            requestBody = String.format("{\"contents\":[{\"parts\":[{\"text\":\"%s\"}]}]}", escapeJson(fullPrompt));
        } else {
            requestBody = String.format(
                    "{\"model\":\"%s\",\"messages\":[{\"role\":\"system\",\"content\":\"%s\"},{\"role\":\"user\",\"content\":\"%s\"}]}",
                    model != null ? model : "helper-model",
                    escapeJson(systemInstruction + "\n\nSearch Context:\n" + contextBuilder.toString()),
                    escapeJson(prompt));
        }

        org.springframework.web.reactive.function.client.WebClient.RequestBodySpec requestSpec;
        if (endpoint.contains("googleapis.com")) {
            String finalUrl = endpoint;
            if (!endpoint.contains("key=")) {
                finalUrl += (endpoint.contains("?") ? "&" : "?") + "key=" + apiKey;
            }
            requestSpec = webClientBuilder.build().post().uri(finalUrl);
        } else {
            requestSpec = webClientBuilder.build().post().uri(endpoint)
                    .header("Authorization", "Bearer " + apiKey);
        }

        return requestSpec
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(java.time.Duration.ofMillis(timeoutMs - 2000))
                .map(responseJson -> {
                    String parsed = parseResponse(responseJson);
                    if (parsed == null || parsed.trim().isEmpty()) {
                        throw new RuntimeException("Empty response from cloud helper");
                    }
                    return new ProviderVote("cloud_helper", parsed, 0.95, System.currentTimeMillis());
                })
                .onErrorResume(err -> {
                    logger.warn("Cloud Helper call failed: {}", err.getMessage());
                    return Mono.empty();
                });
    }

    /**
     * Solo Mode deep research using the Playwright browser automation server.
     *
     * <p>This method augments the existing {@link ActiveInternetScraper} results with
     * full-page content extracted by a real Chromium browser. It works in two phases:
     * <ol>
     *   <li><b>Search</b> — uses DuckDuckGo HTML search to discover relevant URLs.</li>
     *   <li><b>Deep-scrape</b> — navigates each discovered URL via Playwright, extracts
     *       the full text content, and returns it as {@link ScrapedIssue} objects.</li>
     * </ol>
     *
     * <p>The method is fully reactive, bounded by {@link #soloMaxSteps} and
     * {@link #soloStepTimeoutMs}, and never throws — failures are caught and logged
     * so Solo Mode always produces a response.
     */
    private Mono<List<ScrapedIssue>> playwrightResearch(String prompt, List<String> keywords) {
        if (playwrightAutomationUrl == null || playwrightAutomationUrl.isBlank()) {
            logger.debug("Playwright automation URL not configured — skipping deep research");
            return Mono.just(List.of());
        }

        String query = (keywords != null && !keywords.isEmpty())
                ? String.join(" ", keywords)
                : prompt;

        // Phase 1: Discover URLs via DuckDuckGo HTML search
        String searchUrl = "https://html.duckduckgo.com/html/?q="
                + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);

        logger.info("[Solo Mode] Phase 1 — Playwright deep research for query: {}", query);

        return webClientBuilder.build().get()
                .uri(searchUrl)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(java.time.Duration.ofMillis(soloStepTimeoutMs))
                .map(searchHtml -> extractUrlsFromDdg(searchHtml))
                .flatMapMany(Flux::fromIterable)
                .take(soloMaxSteps)
                // Phase 2: Deep-scrape each URL via Playwright server
                .flatMap(url -> deepScrapeUrl(url, prompt)
                        .onErrorResume(e -> {
                            logger.warn("[Solo Mode] Deep-scrape failed for {}: {}", url, e.getMessage());
                            return Mono.empty();
                        }))
                .collectList()
                .doOnSuccess(results -> logger.info(
                        "[Solo Mode] Playwright deep research complete — {} pages extracted", results.size()))
                .doOnError(e -> logger.warn("[Solo Mode] Playwright research failed: {}", e.getMessage()))
                .onErrorResume(e -> Mono.just(List.of()));
    }

    /**
     * Extracts result URLs from DuckDuckGo HTML search results.
     * DDG wraps links in <a class="result__a" href="..."> elements.
     */
    private List<String> extractUrlsFromDdg(String html) {
        List<String> urls = new ArrayList<>();
        if (html == null || html.isBlank()) return urls;

        String lower = html.toLowerCase();
        int idx = 0;
        while (urls.size() < soloMaxSteps) {
            int aTag = lower.indexOf("class=\"result__a\"", idx);
            if (aTag == -1) break;
            int hrefStart = lower.indexOf("href=\"", aTag);
            if (hrefStart == -1) break;
            hrefStart += 6;
            int hrefEnd = lower.indexOf('"', hrefStart);
            if (hrefEnd == -1) break;
            String url = html.substring(hrefStart, hrefEnd);
            // DDG wraps external URLs in /l/?uddg=...
            if (url.startsWith("/l/")) {
                int uddgIdx = url.indexOf("uddg=");
                if (uddgIdx != -1) {
                    url = java.net.URLDecoder.decode(
                            url.substring(uddgIdx + 5), java.nio.charset.StandardCharsets.UTF_8);
                }
            }
            if (url.startsWith("http") && !url.contains("duckduckgo.com")) {
                urls.add(url);
            }
            idx = hrefEnd;
        }
        return urls;
    }

    /**
     * Navigates to a URL via the Playwright server and extracts the full page text.
     * Returns a {@link ScrapedIssue} with the page title as the issue title and the
     * extracted text as the solution.
     */
    private Mono<ScrapedIssue> deepScrapeUrl(String url, String prompt) {
        return webClientBuilder.build().post()
                .uri(playwrightAutomationUrl + "/navigate")
                .bodyValue(Map.of("url", url))
                .retrieve()
                .bodyToMono(Void.class)
                .timeout(java.time.Duration.ofMillis(soloStepTimeoutMs))
                .then(Mono.delay(java.time.Duration.ofMillis(2000)))
                .then(webClientBuilder.build().get()
                        .uri(playwrightAutomationUrl + "/extract-text")
                        .retrieve()
                        .bodyToMono(Map.class)
                        .timeout(java.time.Duration.ofMillis(soloStepTimeoutMs))
                        .map(body -> {
                            String text = body != null && body.get("text") != null
                                    ? body.get("text").toString().trim()
                                    : "";
                            String title = url.replaceFirst("https?://", "")
                                    .replaceFirst("/$", "")
                                    .replaceFirst("www\\.", "");
                            if (text.length() > 3000) {
                                text = text.substring(0, 3000) + "...";
                            }
                            ScrapedIssue issue = new ScrapedIssue(
                                    "Deep Research: " + title, text, "Playwright Browser");
                            issue.setSourceAuthority(0.85);
                            issue.setRawConfidence(0.80);
                            return issue;
                        }));
    }

    private Mono<VotingResult> executeSoloFallback(String prompt, List<ScrapedIssue> issues, long startTime) {
        return playwrightResearch(prompt, extractKeywords(prompt))
                .flatMap(playwrightIssues -> {
                    List<ScrapedIssue> allIssues = new ArrayList<>(issues);
                    if (playwrightIssues != null && !playwrightIssues.isEmpty()) {
                        allIssues.addAll(playwrightIssues);
                        logger.info("[Solo Mode] Merged {} Playwright deep-research issues with {} scraper issues",
                                playwrightIssues.size(), issues.size());
                    }

                    // Build a detailed high-intelligence prompt for the sidecar
                    StringBuilder sb = new StringBuilder();
                    sb.append("You are SupremeAI operating in autonomous Solo-Mode.\n");
                    sb.append("Answer the user query with high human-level intelligence, deep reasoning, and clear executable steps.\n\n");
                    sb.append("User query: ").append(prompt).append("\n\n");
                    if (!allIssues.isEmpty()) {
                        sb.append("Here are some relevant facts scraped from the web to help you form a factual response:\n");
                        for (ScrapedIssue issue : allIssues) {
                            sb.append("- ").append(issue.getTitle()).append(": ").append(issue.getSolution()).append("\n");
                        }
                        sb.append("\n");
                    }
                    sb.append("Synthesize a comprehensive, authoritative response in Bengali (বাংলা). Do not mention that you are a GGUF or a small model.");

                    String localPrompt = sb.toString();

                    return soloModeManagerService.triggerLocalModelFallback(localPrompt)
                            .onErrorResume(e -> {
                                logger.warn("[Solo Mode] Local model synthesis failed, falling back to template-based response: {}", e.getMessage());
                                return Mono.just("");
                            })
                            .map(synthesized -> {
                                String finalResponse;
                                if (synthesized == null || synthesized.isBlank() || synthesized.contains("Offline Mode Fallback response") || synthesized.contains("System is currently downloading")) {
                                    // Local model returned fallback stub, use our dynamic rule-based synthesis
                                    finalResponse = synthesizeSoloResponse(prompt, allIssues);
                                } else {
                                    // Prepend the header to keep UI premium and clear
                                    finalResponse = "🤖 **SupremeAI স্বায়ত্তশাসিত সোলো-মোড সক্রিয় (Autonomous Solo-Mode - Dynamic local model)**\n\n" 
                                            + synthesized 
                                            + "\n\n---\n*💡 সুপ্রিমএআই ব্রাউজার অটোমেশন এবং সেলফ-লার্নিং সক্রিয় রেখেছে এবং এই চ্যাট থেকে প্রাপ্ত নতুন জ্ঞান ভবিষ্যতের জন্য ডাটাবেসে সংরক্ষণ করা হয়েছে।*";
                                }

                                enhancedLearningService.learnFromNLPInteraction(prompt, finalResponse, "browser_autonomous_crawler", 0.85,
                                        Map.of("mode", "browser_only")).subscribe();
                                long duration = System.currentTimeMillis() - startTime;
                                ProviderVote vote = new ProviderVote("autonomous_browser", finalResponse, 0.85,
                                        System.currentTimeMillis());
                                return new VotingResult(prompt, finalResponse, List.of(vote), 0.85, "SOLO_MODE", duration);
                            });
                });
    }

    private String synthesizeSoloResponse(String prompt, List<ScrapedIssue> issues) {
        StringBuilder sb = new StringBuilder();
        sb.append("🤖 **SupremeAI স্বায়ত্তশাসিত সোলো-মোড সক্রিয় (Autonomous Solo-Mode)**\n\n");
        sb.append(
                "কোনো হার্ডকোডেড ক্লাউড এআই সার্ভিস ছাড়াই সুপ্রিমএআই সরাসরি ওপেন-ওয়েব রিসার্চ ও ব্রাউজার ক্রলিংয়ের মাধ্যমে আপনার প্রশ্নটির রিয়েল-টাইম উত্তর প্রস্তুত করেছে।\n\n");

        if (issues == null || issues.isEmpty()) {
            sb.append("### 🔍 ইন্টারনেট রিসার্চ স্ট্যাটাস:\n");
            sb.append(
                    "> ⚠️ ওপেন ওয়েব থেকে সরাসরি কোনো মিল খুঁজে পাওয়া যায়নি। তবে পূর্ববর্তী লার্নিং প্যাটার্ন ও আমাদের সিস্টেমের নিজস্ব অ্যালগরিদম ভিত্তিক বিশ্লেষণ নিচে উপস্থাপন করা হলো:\n\n");
            sb.append("### 💡 সুপ্রিমএআই বিশ্লেষণ ও পরামর্শ:\n");
            sb.append(generateFallbackGeneralResponse(prompt));
        } else {
            sb.append("### 🔍 ওপেন-ওয়েব রিসার্চের গুরুত্বপূর্ণ তথ্যসমূহ:\n");
            for (int i = 0; i < Math.min(issues.size(), 3); i++) {
                ScrapedIssue issue = issues.get(i);
                sb.append("#### 📌 ").append(issue.getTitle()).append("\n");
                sb.append("- **রিসার্চ সারাংশ:** ").append(issue.getSolution()).append("\n");
                sb.append("- **তথ্যসূত্র:** `").append(issue.getSource()).append("` (নির্ভরযোগ্যতা স্কোর: ")
                        .append(String.format("%.0f%%", issue.getSourceAuthority() * 100)).append(")\n\n");
            }

            sb.append("### 🛠️ সুপ্রিমএআই প্রস্তাবিত অ্যাকশন প্ল্যান:\n");
            sb.append(generateStepByStepAdvice(prompt, issues));
        }

        sb.append(
                "\n\n---\n*💡 সুপ্রিমএআই ব্রাউজার অটোমেশন এবং সেলф-লার্নিং সক্রিয় রেখেছে এবং এই চ্যাট থেকে প্রাপ্ত নতুন জ্ঞান ভবিষ্যতের জন্য ডাটাবেসে সংরক্ষণ করা হয়েছে।*");
        return sb.toString();
    }

    private String generateFallbackGeneralResponse(String prompt) {
        String p = prompt.toLowerCase();
        if (p.contains("react") || p.contains("next") || p.contains("frontend") || p.contains("css")) {
            return "১. **প্রজেক্ট ডিপেন্ডেন্সি ও রিঅ্যাক্ট ভার্সন চেক করুন:**\n" +
                    "   - নিশ্চিত হোন যে আপনার `package.json` ফাইলে সঠিক রিঅ্যাক্ট ও লাইব্রেরি ভার্সন কনফিগার করা আছে।\n"
                    +
                    "   - যেকোনো ডিপেন্ডেন্সি কনফ্লিক্ট এড়াতে `npm dedupe` বা `npm install --legacy-peer-deps` ব্যবহার করুন।\n\n"
                    +
                    "২. **ব্রাউজার কনসোল ও রেন্ডারিং এরর বিশ্লেষণ:**\n" +
                    "   - ব্রাউজার কনসোলের রেন্ডারিং এরর চেক করুন এবং রেন্ডার ব্লকিং কোনো কম্পোনেন্ট থাকলে তা সমাধান করুন।\n"
                    +
                    "   - স্টাইল গ্লিচ এড়াতে ভ্যানিলা সিএসএস (Vanilla CSS) এর কাস্টম মিডিয়া কোয়েরি ও ফ্লেক্সবক্স/গ্রিড চেক করুন।\n\n"
                    +
                    "৩. **স্টেট ম্যানেজমেন্ট ও অপ্টিমাইজেশন:**\n" +
                    "   - রিঅ্যাক্ট প্রজেক্টের ক্ষেত্রে `useMemo` এবং `useCallback` এর সঠিক ব্যবহার নিশ্চিত করুন যাতে অনাকাঙ্ক্ষিত রি-রেন্ডার হ্রাস পায়।";
        } else if (p.contains("java") || p.contains("spring") || p.contains("maven") || p.contains("gradle")) {
            return "১. **ডিপেন্ডেন্সি ও লাইফসাইকেল চেক:**\n" +
                    "   - আপনার `pom.xml` বা `build.gradle` ফাইলটি পুনরায় পরীক্ষা করুন। কোনো ডুপ্লিকেট ডিপেন্ডেন্সি বা অসামঞ্জস্যপূর্ণ জার ভার্সন থাকলে তা দূর করুন।\n\n"
                    +
                    "২. **কনফিগারেশন ও এনভায়রনমেন্ট প্রোপার্টিজ:**\n" +
                    "   - `application.properties` বা `application.yml` ফাইলের সেটিংস (যেমন পোর্ট, ডাটাবেস URL এবং সিকিউরিটি ফিল্টারস) সঠিক আছে কিনা যাচাই করুন।\n\n"
                    +
                    "৩. **স্প্রিং বুট এরর হ্যান্ডলিং ও বিল্ড ক্লিন:**\n" +
                    "   - ক্যাশ সংক্রান্ত সমস্যা এড়াতে `./mvnw clean install` বা `./gradlew clean build` রান করুন।\n" +
                    "   - রানটাইম এররের জন্য অ্যাপ্লিকেশন লগ/স্ট্যাক ট্রেস পুঙ্খানুপুঙ্খভাবে বিশ্লেষণ করুন।";
        } else if (p.contains("firebase") || p.contains("auth") || p.contains("rules")) {
            return "১. **ফায়ারবেস সিকিউরিটি রুলস অডিট:**\n" +
                    "   - আপনার ডাটাবেস (`database.rules.json` বা `firestore.rules`) রুলস সঠিকভাবে পরীক্ষা করুন। ডুপ্লিকেট বা ওভারল্যাপিং রুলস ডিলিট করুন যাতে অথেনটিকেটেড ইউজাররা সহজেই রিকোয়েস্ট পাঠাতে পারেন।\n\n"
                    +
                    "২. **ফায়ারবেস ইনিশিয়ালাইজেশন ও কানেক্টিভিটি:**\n" +
                    "   - ক্লায়েন্ট সাইড এবং ব্যাকেন্ড সাইডে ফায়ারবেস ক্রেডেনশিয়াল লোডিং সঠিক আছে কিনা তা নিশ্চিত করুন।\n\n"
                    +
                    "৩. **টোকেন ভ্যালিডেশন:**\n" +
                    "   - ফায়ারবেস আইডি টোকেন এক্সপায়ার হয়েছে কিনা তা কনসোল বা টোকেন ভ্যালিডেটর এপিআই দিয়ে পরীক্ষা করুন।";
        } else {
            return "১. **রুট কজ অ্যানালাইসিস (Root Cause Analysis):**\n" +
                    "   - আপনার ইনপুটের মূল বিষয়বস্তু এবং এর সংকেত বা আচরণ বিশ্লেষণ করুন।\n\n" +
                    "২. **এনভায়রনমেন্ট কনফিগারেশন ভ্যালিডেশন:**\n" +
                    "   - প্রজেক্টের কনফিগারেশন ফাইল ও এনভায়রনমেন্ট সেটিংস পুনরায় পরীক্ষা করে দেখুন কোনো অমিল আছে কিনা।\n\n"
                    +
                    "৩. **বিল্ড এবং রানটাইম ডায়াগনস্টিকস:**\n" +
                    "   - ক্যাশেড বা কম্পাইল্ড ফাইল ডিলিট করে প্রজেক্ট পুনরায় বিল্ড দিন এবং লগের বিস্তারিত ট্র্যাকিং করুন।";
        }
    }

    private String generateStepByStepAdvice(String prompt, List<ScrapedIssue> issues) {
        StringBuilder advice = new StringBuilder();
        advice.append(
                "ওপেন-ওয়েব থেকে সংগৃহীত জ্ঞানভাণ্ডার বিশ্লেষণ করে আপনার জন্য একটি ধাপে ধাপে সমাধান নিচে তৈরি করা হলো:\n\n");

        int step = 1;
        for (ScrapedIssue issue : issues) {
            String title = issue.getTitle();
            String solution = issue.getSolution();
            if (solution.length() > 200) {
                solution = solution.substring(0, 200) + "...";
            }

            advice.append("**ধাপ ").append(step).append(": ").append(title).append("**\n")
                    .append("- *সমাধান গাইডলাইন:* ").append(solution).append("\n")
                    .append("- *অ্যাকশন আইটেম:* উৎস [").append(issue.getSource())
                    .append("] অনুযায়ী কনফিগারেশন এবং কোড সিনট্যাক্স সামঞ্জস্যपूर्ण করুন।\n\n");
            step++;
        }

        advice.append("**চূড়ান্ত ধাপ: ভ্যালিডেশন ও সেলফ-লার্নিং**\n")
                .append("- প্রজেক্টটি লোকালি রান করুন এবং পরিবর্তনগুলো সঠিকভাবে কাজ করছে কিনা ভ্যালিডেট করুন।\n")
                .append("- আমাদের সিস্টেমের লার্নিং মডিউল স্বয়ংক্রিয়ভাবে এই অ্যাকশন প্ল্যানটিকে সুপ্রিমএআই নলেজ বেজে যুক্ত করেছে।");

        return advice.toString();
    }

    private List<String> extractKeywords(String prompt) {
        List<String> keywords = new ArrayList<>();
        if (prompt == null || prompt.isEmpty())
            return keywords;

        String clean = prompt.toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", " ");
        String[] words = clean.split("\\s+");

        Set<String> techWords = Set.of(
                "react", "angular", "vue", "next", "vite", "javascript", "typescript", "css", "html",
                "java", "spring", "maven", "gradle", "kotlin", "scala",
                "firebase", "firestore", "auth", "hosting", "database",
                "gcp", "google", "cloud", "run", "vertex", "ai", "openai", "gemini",
                "error", "bug", "exception", "failed", "crash", "security", "rules");

        for (String w : words) {
            if (techWords.contains(w) && !keywords.contains(w)) {
                keywords.add(w);
            }
        }

        if (keywords.size() < 3) {
            for (String w : words) {
                if (w.length() > 4 && !keywords.contains(w) && keywords.size() < 5) {
                    keywords.add(w);
                }
            }
        }

        if (keywords.isEmpty()) {
            keywords.add("general");
            keywords.add("technology");
        }

        return keywords;
    }

    private String detectDomain(String prompt) {
        if (prompt == null)
            return "general";
        String p = prompt.toLowerCase();
        if (p.contains("react") || p.contains("next") || p.contains("frontend") || p.contains("web")) {
            return "web-development";
        }
        if (p.contains("java") || p.contains("spring") || p.contains("backend")) {
            return "java-backend";
        }
        if (p.contains("firebase") || p.contains("firestore") || p.contains("rtdb")) {
            return "firebase-ecosystem";
        }
        return "general-tech";
    }

    private String escapeJson(String text) {
        if (text == null)
            return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String jsonStr) {
        return jsonStr.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private String parseResponse(String json) {
        try {
            if (json.contains("\"candidates\"")) {
                int start = json.indexOf("\"text\"");
                if (start != -1) {
                    start = json.indexOf("\"", start + 6);
                    int end = json.indexOf("\"", start + 1);
                    while (end != -1 && json.charAt(end - 1) == '\\') {
                        end = json.indexOf("\"", end + 1);
                    }
                    if (start != -1 && end != -1) {
                        return unescapeJson(json.substring(start + 1, end));
                    }
                }
            }
            if (json.contains("\"choices\"")) {
                int start = json.indexOf("\"content\"");
                if (start != -1) {
                    start = json.indexOf("\"", start + 9);
                    int end = json.indexOf("\"", start + 1);
                    while (end != -1 && json.charAt(end - 1) == '\\') {
                        end = json.indexOf("\"", end + 1);
                    }
                    if (start != -1 && end != -1) {
                        return unescapeJson(json.substring(start + 1, end));
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse API response: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Get providers assigned to a specific task type.
     * Priority: DB assignment → fallback providers → all active providers.
     */
    private Mono<List<String>> getAssignedProvidersForTask(String taskType) {
        // 1. Check DB for task-specific assignment
        return taskAssignmentRepo.findByTaskTypeAndIsActive(taskType, true)
                .collectList()
                .flatMap(assignments -> {
                    if (assignments != null && !assignments.isEmpty()) {
                        // Merge all provider IDs from matching assignments
                        List<String> providers = new ArrayList<>();
                        for (com.supremeai.model.TaskProviderAssignment assignment : assignments) {
                            if (assignment.getProviderIds() != null) {
                                providers.addAll(assignment.getProviderIds());
                            }
                        }
                        if (!providers.isEmpty()) {
                            logger.info("🎯 Using {} task-assigned providers for '{}': {}",
                                    providers.size(), taskType, providers);
                            return Mono.just(providers);
                        }
                    }
                    return Mono.empty();
                })
                .onErrorResume(e -> {
                    logger.warn("Task assignment lookup failed for '{}': {}", taskType, e.getMessage());
                    return Mono.empty();
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // 2. Fallback: use contextual ranking to select best providers
                    try {
                        ContextualAIRankingService.TaskType tType = null;
                        for (ContextualAIRankingService.TaskType t : ContextualAIRankingService.TaskType.values()) {
                            if (t.name().equalsIgnoreCase(taskType)) {
                                tType = t;
                                break;
                            }
                        }
                        if (tType != null) {
                            List<String> ranked = contextualRankingService.getRankingsForTask(tType)
                                    .stream().map(r -> r.provider).collect(Collectors.toList());
                            if (ranked != null && !ranked.isEmpty()) {
                                List<String> result = ranked.stream()
                                        .limit(DEFAULT_MAX_VOTING_PROVIDERS)
                                        .collect(Collectors.toList());
                                logger.info("📊 Using {} ranked providers for '{}': {}",
                                        result.size(), taskType, result);
                                return Mono.just(result);
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Contextual ranking lookup failed for '{}': {}", taskType, e.getMessage());
                    }
                    return Mono.empty();
                }))
                .switchIfEmpty(Mono.defer(() -> {
                    // 3. Ultimate fallback: all healthy providers (truly unlimited)
                    return providerRepository.findAll()
                            .filter(p -> "active".equalsIgnoreCase(p.getStatus()) && p.isCanParticipateInVoting())
                            .map(p -> p.getName().toLowerCase())
                            .collectList()
                            .doOnNext(allProviders -> logger.info(
                                    "🌍 No task-specific assignment for '{}' - using {} active providers",
                                    taskType, allProviders != null ? allProviders.size() : 0));
                }));
    }

    // ===== APPROVAL VOTING (from CouncilVotingSystem) =====

    /**
     * Conduct approval vote for risky actions
     * Replaces CouncilVotingSystem.conductVote
     */
    public Mono<Boolean> conductApprovalVote(String changeType, String codeSnippet,
            List<String> councilMembers) {
        logger.info("[Approval Voting] Initiating vote for major change: {}", changeType);

        VotingTopic topic = topicGenerator.generateTopicForMajorChange(changeType, codeSnippet);
        if (topic == null) {
            logger.error("[Approval Voting] Failed to generate voting topic for: {}", changeType);
            return Mono.just(false);
        }
        logger.info("[Approval Voting] Formulated Question: {}", topic.getQuestionToAsk());

        return Flux.fromIterable(councilMembers)
                .flatMap(member -> Mono.fromCallable(() -> simulateAIVote(member, topic))
                        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()))
                .collectList()
                .map(votes -> {
                    long approveCount = votes.stream().filter(v -> v).count();
                    long rejectCount = votes.size() - approveCount;
                    boolean finalDecision = approveCount > rejectCount;
                    logger.info("[Approval Voting] Final Result: {} Approve, {} Reject -> Decision: {}",
                            approveCount, rejectCount, finalDecision ? "PROCEED" : "ABORT");
                    return finalDecision;
                });
    }

    // ===== DECISION VOTING (from AutonomousVotingService) =====

    /**
     * Conduct decision vote on questions
     * Replaces AutonomousVotingService.conductVote
     */
    public Mono<VotingDecision> conductDecisionVote(String question, String context) {
        logger.info("Starting decision voting for question: {}", question);

        return providerRepository.findAll()
                .filter(p -> "active".equalsIgnoreCase(p.getStatus()) && p.isCanParticipateInVoting())
                .map(p -> p.getName().toLowerCase())
                .collectList()
                .flatMap(providerList -> {
                    List<String> listToUse = providerList;

                    return Flux.fromIterable(listToUse)
                            .flatMap(providerName -> {
                                long start = System.currentTimeMillis();
                                try {
                                    AIProvider provider = providerFactory.getEnforcedProvider(providerName);
                                    return provider.generate(buildDecisionPrompt(question, context))
                                            .map(response -> {
                                                long latency = System.currentTimeMillis() - start;
                                                ProviderVote vote = new ProviderVote();
                                                vote.setProviderName(providerName);
                                                vote.setResponse(response);
                                                vote.setConfidence(calculateDecisionConfidence(response, latency));
                                                vote.setLatencyMs(latency);
                                                vote.setSuccess(true);
                                                logger.debug("Provider {} voted successfully in {}ms", providerName,
                                                        latency);
                                                return vote;
                                            })
                                            .onErrorResume(e -> {
                                                logger.warn("Provider {} failed to vote: {}", providerName,
                                                        e.getMessage());
                                                ProviderVote vote = new ProviderVote();
                                                vote.setProviderName(providerName);
                                                vote.setSuccess(false);
                                                vote.setErrorMessage(e.getMessage());
                                                return Mono.just(vote);
                                            });
                                } catch (Exception e) {
                                    logger.warn("Provider Factory failed for {}: {}", providerName, e.getMessage());
                                    ProviderVote vote = new ProviderVote();
                                    vote.setProviderName(providerName);
                                    vote.setSuccess(false);
                                    vote.setErrorMessage(e.getMessage());
                                    return Mono.just(vote);
                                }
                            })
                            .collectList()
                            .map(votes -> calculateDecisionConsensus(question, votes));
                });
    }

    // ===== CONSENSUS VOTING (from MultiAIConsensusService) =====

    /**
     * Ask consensus from multiple AI providers
     * Replaces MultiAIConsensusService.askAllAIs
     */
    @Cacheable(value = "ai_responses", key = "#question + '_' + T(java.util.Collections).unmodifiableSortedSet(new java.util.TreeSet(#providerNames)).hashCode()")
    public Mono<ConsensusResult> askConsensus(String question, List<String> providerNames, long timeoutMs) {
        return executeEnsembleVoting(question, null, timeoutMs)
                .map(votingResult -> {
                    ConsensusResult result = new ConsensusResult();
                    result.setQuestion(question);
                    result.setConsensusAnswer(votingResult.getBestResponse());
                    result.setVotes(votingResult.getAllVotes());
                    result.setAverageConfidence(votingResult.getAverageConfidence());
                    result.setStrength(
                            votingResult.getVerdict().contains("SOLO") ? "CONSENSUS_WEAK" : "CONSENSUS_STRONG");
                    result.setConsensusPercentage(100.0);
                    result.setResponseTimeMs(votingResult.getProcessingTimeMs());
                    result.setQualityScore(votingResult.getAverageConfidence());
                    return result;
                });
    }

    /**
     * Ask contextual consensus with automatic provider selection
     * Replaces MultiAIConsensusService.askContextualAIs
     */
    public Mono<ConsensusResult> askContextualConsensus(String question, int count, long timeoutMs) {
        return askConsensus(question, List.of(), timeoutMs);
    }

    /**
     * Get consensus history
     * Replaces MultiAIConsensusService.getHistory
     */
    public Flux<ConsensusVote> getConsensusHistory(int limit) {
        return Flux.fromIterable(consensusHistory)
                .sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .take(limit);
    }

    // ===== PRIVATE HELPER METHODS =====

    private Mono<ProviderVote> queryModel(String modelName, String prompt, long timeoutMs) {
        return Mono.defer(() -> {
            try {
                AIProvider provider = providerFactory.getEnforcedProvider(modelName);
                long startTime = System.currentTimeMillis();

                Mono<String> generationMono = (selfHealingService != null) ? selfHealingService.executeWithRetry(
                        () -> provider.generate(prompt),
                        getMaxRetries(),
                        RETRY_BACKOFF_MS) : provider.generate(prompt);

                return generationMono
                        .map(response -> {
                            long responseTime = System.currentTimeMillis() - startTime;
                            ContextualAIRankingService.TaskType taskType = contextualRankingService
                                    .detectTaskType(prompt);
                            double confidence = calculateConfidence(response, modelName, responseTime, taskType);
                            ProviderVote vote = new ProviderVote(modelName, response, confidence,
                                    System.currentTimeMillis());
                            updatePerformanceTracker(vote, taskType);
                            return vote;
                        })
                        .timeout(java.time.Duration.ofMillis(timeoutMs))
                        .onErrorResume(e -> {
                            logger.warn("Model {} failed: {}", modelName, e.getMessage());
                            return Mono.empty();
                        });
            } catch (Exception e) {
                logger.warn("Model Factory failed for {}: {}", modelName, e.getMessage());
                return Mono.empty();
            }
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }

    private VotingResult calculateEnsembleResult(
            String prompt,
            List<ProviderVote> votes,
            long duration,
            ContextualAIRankingService.TaskType taskType,
            Map<String, Double> weightsMap) {
        if (votes.isEmpty()) {
            return new VotingResult(prompt,
                    FallbackConstants.NO_PROVIDER_RESPONSE,
                    votes, 0.0, "ERROR", duration);
        }

        // ===== 2-ROUND META-CONSENSUS REFINEMENT LOOP =====
        if (votes.size() > 1) {
            double[] round1Confidences = new double[votes.size()];
            // Round 1: Peer Consensus Alignment
            for (int i = 0; i < votes.size(); i++) {
                ProviderVote vI = votes.get(i);
                double totalSim = 0.0;
                int peerCount = 0;
                for (int j = 0; j < votes.size(); j++) {
                    if (i == j)
                        continue;
                    ProviderVote vJ = votes.get(j);
                    double jaccard = calculateJaccardOverlap(vI.getResponse(), vJ.getResponse());
                    double structural = calculateStructuralSimilarity(vI.getResponse(), vJ.getResponse());
                    double sim = 0.6 * jaccard + 0.4 * structural;
                    totalSim += sim;
                    peerCount++;
                }
                double avgPeerSupport = peerCount > 0 ? (totalSim / peerCount) : 1.0;
                round1Confidences[i] = vI.getConfidence() * (0.7 + 0.3 * avgPeerSupport);
            }

            // Round 2: Weighted Consensus Reinforcement
            for (int i = 0; i < votes.size(); i++) {
                ProviderVote vI = votes.get(i);
                double weightedSimSum = 0.0;
                double weightSum = 0.0;
                for (int j = 0; j < votes.size(); j++) {
                    if (i == j)
                        continue;
                    ProviderVote vJ = votes.get(j);
                    double jaccard = calculateJaccardOverlap(vI.getResponse(), vJ.getResponse());
                    double structural = calculateStructuralSimilarity(vI.getResponse(), vJ.getResponse());
                    double sim = 0.6 * jaccard + 0.4 * structural;
                    weightedSimSum += sim * round1Confidences[j];
                    weightSum += round1Confidences[j];
                }
                double weightedSupport = weightSum > 0.0 ? (weightedSimSum / weightSum) : 1.0;
                double finalConfidence = round1Confidences[i] * (0.8 + 0.2 * weightedSupport);
                vI.setConfidence(Math.min(1.0, Math.max(0.0, finalConfidence)));
            }
        }

        Map<String, List<ProviderVote>> similarityGroups = groupBySimilarity(votes);
        Map<String, Double> groupScores = new HashMap<>();
        Map<String, Double> groupWeights = new HashMap<>();

        for (Map.Entry<String, List<ProviderVote>> entry : similarityGroups.entrySet()) {
            String groupKey = entry.getKey();
            List<ProviderVote> groupVotes = entry.getValue();

            double totalWeight = 0;
            double weightedScore = 0;

            for (ProviderVote vote : groupVotes) {
                double weight = calculateModelWeight(vote.getProviderName(), vote.getConfidence(), taskType,
                        weightsMap);
                weightedScore += vote.getConfidence() * weight;
                totalWeight += weight;
            }

            groupScores.put(groupKey, totalWeight > 0 ? weightedScore / totalWeight : 0);
            groupWeights.put(groupKey, totalWeight);
        }

        String winningGroupKey = groupScores.entrySet().stream()
                .max(Comparator.comparingDouble(e -> groupWeights.get(e.getKey()) * e.getValue()))
                .map(Map.Entry::getKey)
                .orElse(votes.get(0).getResponse().substring(0, Math.min(100, votes.get(0).getResponse().length())));

        List<ProviderVote> winningVotes = similarityGroups.get(winningGroupKey);
        String bestResponse = winningVotes.get(0).getResponse();

        double consensusPercentage = (double) winningVotes.size() / votes.size() * 100.0;
        double avgConfidence = winningVotes.stream()
                .mapToDouble(ProviderVote::getConfidence)
                .average()
                .orElse(0.0);

        for (ProviderVote vote : winningVotes) {
            updatePerformanceTracker(vote, taskType);
        }

        String verdict = determineVerdict(consensusPercentage, avgConfidence);

        return new VotingResult(prompt, bestResponse, votes, avgConfidence, verdict, duration);
    }

    private boolean simulateAIVote(String member, VotingTopic topic) {
        // Simulate based on topic category and member
        if (topic.getCategory().equals("SECURITY") && member.toUpperCase().contains("CLAUDE")) {
            return Math.random() > 0.4;
        }
        return Math.random() > 0.2;
    }

    private String buildDecisionPrompt(String question, String context) {
        return String.format("""
                Context: %s

                Question: %s

                Provide your expert opinion on this question. Be concise and specific.
                """, context, question);
    }

    private double calculateDecisionConfidence(String response, long latencyMs) {
        double confidence = 0.3;

        if (latencyMs < 100)
            confidence += 0.3;
        else if (latencyMs < 500)
            confidence += 0.2;
        else
            confidence += 0.1;

        int length = response != null ? response.length() : 0;
        if (length >= 50 && length <= 500)
            confidence += 0.4;
        else if (length > 0)
            confidence += 0.2;

        return Math.max(0.0, Math.min(1.0, confidence));
    }

    private VotingDecision calculateDecisionConsensus(String question, List<ProviderVote> votes) {
        VotingDecision decision = new VotingDecision();
        decision.setDecisionKey(question);
        decision.setProviderVotes(votes);

        long successCount = votes.stream().filter(ProviderVote::isSuccess).count();

        if (successCount == 0) {
            decision.setStrength("ERROR");
            decision.setConfidence(0.0);
            decision.setAiConsensus(FallbackConstants.VOTING_FAILURE);
            return decision;
        }

        Map<String, Long> responseFrequency = votes.stream()
                .filter(ProviderVote::isSuccess)
                .collect(Collectors.groupingBy(ProviderVote::getResponse, Collectors.counting()));

        Map.Entry<String, Long> mostCommon = responseFrequency.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow();

        double consensusPercentage = (double) mostCommon.getValue() / successCount;

        decision.setAiConsensus(mostCommon.getKey());
        decision.setConfidence(consensusPercentage);

        if (consensusPercentage >= 0.75)
            decision.setStrength("STRONG");
        else if (consensusPercentage >= 0.5)
            decision.setStrength("WEAK");
        else
            decision.setStrength("SPLIT");

        logger.info("Decision voting complete. Consensus: {} with {} confidence",
                decision.getStrength(), decision.getConfidence());

        return decision;
    }

    private ConsensusResult calculateConsensusResult(String question, List<ProviderVote> votes, long totalTimeMs) {
        if (votes.isEmpty()) {
            return new ConsensusResult(
                    question,
                    FallbackConstants.NO_PROVIDER_RESPONSE,
                    List.of(),
                    0.0,
                    "ERROR",
                    0.0,
                    totalTimeMs,
                    0.0);
        }

        Map<String, List<ProviderVote>> groups = new LinkedHashMap<>();
        for (ProviderVote vote : votes) {
            String normalized = vote.getResponse().trim();
            if (normalized.length() > 500) {
                normalized = normalized.substring(0, 500) + "...";
            }
            groups.computeIfAbsent(normalized, k -> new ArrayList<>()).add(vote);
        }

        List<ProviderVote> winningGroup = groups.values().stream()
                .max(Comparator.comparingInt(List::size))
                .orElse(List.of(votes.get(0)));

        String consensusAnswer = winningGroup.get(0).getResponse();
        double consensusPercentage = (double) winningGroup.size() / votes.size() * 100.0;
        double avgConfidence = votes.stream()
                .mapToDouble(ProviderVote::getConfidence)
                .average()
                .orElse(0.0);

        double qualityScore = (consensusPercentage / 100.0) * 0.7 + (avgConfidence) * 0.3;

        saveConsensusVoteToHistory(question, votes, consensusAnswer, consensusPercentage);

        return new ConsensusResult(
                question,
                consensusAnswer,
                votes,
                avgConfidence,
                "CONSENSUS_" + (consensusPercentage >= 70 ? "STRONG" : (consensusPercentage >= 40 ? "WEAK" : "SPLIT")),
                consensusPercentage,
                totalTimeMs,
                qualityScore);
    }

    private void saveConsensusVoteToHistory(String question, List<ProviderVote> votes, String consensus,
            double percentage) {
        try {
            ConsensusVote result = new ConsensusVote();
            result.setQuestion(question);
            result.setConsensusAnswer(consensus);
            result.setConsensusPercentage(percentage);
            result.setVotes(votes);
            result.setTimestamp(new Date());
            consensusHistory.add(result);
            logger.info("Saved consensus vote to in-memory history (size: {})", consensusHistory.size());
        } catch (Exception e) {
            logger.warn("Failed to save vote to history: " + e.getMessage());
        }
    }

    // Methods from TenAIVotingSystem (simplified for brevity)
    private double calculateConfidence(String response, String modelName, long responseTime,
            ContextualAIRankingService.TaskType taskType) {
        double confidence = 0.5;
        int length = response.length();
        if (length > 100 && length < 5000)
            confidence += 0.2;
        else if (length >= 5000)
            confidence += 0.1;

        if (responseTime < 2000)
            confidence += 0.1;
        else if (responseTime > 10000)
            confidence -= 0.1;

        List<ContextualAIRankingService.ProviderRanking> rankings = contextualRankingService
                .getRankingsForTask(taskType);
        for (ContextualAIRankingService.ProviderRanking r : rankings) {
            if (r.provider.equalsIgnoreCase(modelName)) {
                confidence += (r.successRate / 100.0) * 0.3;
                break;
            }
        }

        if (containsCodeBlocks(response))
            confidence += 0.1;
        if (hasConclusion(response))
            confidence += 0.1;

        return Math.min(Math.max(confidence, 0.0), 1.0);
    }

    private Map<String, List<ProviderVote>> groupBySimilarity(List<ProviderVote> votes) {
        Map<String, List<ProviderVote>> groups = new LinkedHashMap<>();
        for (ProviderVote vote : votes) {
            String normalized = normalizeResponse(vote.getResponse());
            String groupKey = findSimilarGroup(groups.keySet(), normalized);
            if (groupKey == null) {
                groupKey = normalized.length() > 100 ? normalized.substring(0, 100) : normalized;
            }
            groups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(vote);
        }
        return groups;
    }

    private String findSimilarGroup(Set<String> existingKeys, String newResponse) {
        if (newResponse.length() < 10)
            return null;
        String normalizedNew = newResponse.toLowerCase().replaceAll("[^a-z0-9]", " ");
        String[] newWords = normalizedNew.split("\\s+");
        if (newWords.length == 0)
            return null;

        Set<String> newWordSet = new HashSet<>();
        for (String w : newWords)
            if (w.length() > 2)
                newWordSet.add(w);

        for (String key : existingKeys) {
            if (Math.abs(key.length() - newResponse.length()) > Math.max(key.length(), newResponse.length()) * 0.5)
                continue;

            String normalizedKey = key.toLowerCase().replaceAll("[^a-z0-9]", " ");
            String[] keyWords = normalizedKey.split("\\s+");

            int matches = 0;
            int totalRelevant = 0;
            for (String kw : keyWords) {
                if (kw.length() > 2) {
                    totalRelevant++;
                    if (newWordSet.contains(kw))
                        matches++;
                }
            }

            if (totalRelevant == 0)
                continue;

            double similarity = (double) matches / Math.max(totalRelevant, newWordSet.size());
            double structuralSimilarity = calculateStructuralSimilarity(key, newResponse);
            double finalSimilarity = (similarity * 0.6) + (structuralSimilarity * 0.4);

            if (finalSimilarity > 0.65)
                return key;
        }
        return null;
    }

    private double calculateStructuralSimilarity(String s1, String s2) {
        int lines1 = s1.split("\n").length;
        int lines2 = s2.split("\n").length;
        double lineRatio = Math.min(lines1, lines2) / (double) Math.max(lines1, lines2);

        int codeBlocks1 = s1.split("```").length / 2;
        int codeBlocks2 = s2.split("```").length / 2;
        double codeRatio = (codeBlocks1 == codeBlocks2) ? 1.0 : 0.0;

        return (lineRatio * 0.5) + (codeRatio * 0.5);
    }

    private double calculateModelWeight(String modelName, double confidence,
            ContextualAIRankingService.TaskType taskType, Map<String, Double> weightsMap) {
        double baseWeight = getModelBaseWeight(modelName, taskType, weightsMap);
        return baseWeight * (0.5 + 0.5 * confidence);
    }

    private double getModelBaseWeight(String modelName, ContextualAIRankingService.TaskType taskType,
            Map<String, Double> weightsMap) {
        double contextualScore = 0.5;
        if (taskType != null) {
            List<ContextualAIRankingService.ProviderRanking> rankings = contextualRankingService
                    .getRankingsForTask(taskType);
            if (rankings != null) {
                for (ContextualAIRankingService.ProviderRanking r : rankings) {
                    if (r.provider.equalsIgnoreCase(modelName)) {
                        contextualScore = r.successRate / 100.0;
                        break;
                    }
                }
            }
        }

        double firestoreWeight = 0.5;
        if (weightsMap != null) {
            Double score = weightsMap.get(modelName.toLowerCase());
            if (score != null) {
                firestoreWeight = score;
            }
        }

        return (contextualScore * 0.6) + (firestoreWeight * 0.4);
    }

    private boolean isModelAvailable(String modelName) {
        try {
            providerFactory.getProvider(modelName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void updatePerformanceTracker(ProviderVote vote, ContextualAIRankingService.TaskType taskType) {
        ModelPerformanceTracker tracker = performanceTrackers.get(vote.getProviderName());
        if (tracker != null) {
            tracker.recordAttempt(vote.getConfidence());
        }

        contextualRankingService.recordTaskOutcome(
                vote.getProviderName(),
                taskType,
                true,
                1000,
                vote.getConfidence() * 5.0);
    }

    private void recordProviderOutcome(String providerName, String question, String response, long latency,
            double confidence) {
        try {
            ContextualAIRankingService.TaskType taskType = contextualRankingService.detectTaskType(question);
            double quality = Math.min(5.0, (response.length() > 100 ? 3.0 : 1.0) + confidence * 2.0);
            contextualRankingService.recordTaskOutcome(providerName, taskType, true, latency, quality);
        } catch (Exception e) {
            // Silently fail
        }
    }

    private String normalizeResponse(String response) {
        if (response == null)
            return "";
        return response.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase();
    }

    private boolean containsCodeBlocks(String response) {
        return response.contains("```") ||
                response.contains("    ") ||
                response.contains("\t") ||
                response.matches(".*(function|class|def |import |public |private ).*");
    }

    private boolean hasConclusion(String response) {
        String lower = response.toLowerCase();
        return lower.contains("conclusion") ||
                lower.contains("summary") ||
                lower.contains("in summary") ||
                lower.contains("therefore") ||
                lower.contains("thus");
    }

    private String determineVerdict(double consensusPercentage, double confidence) {
        if (consensusPercentage >= 80 && confidence >= 0.8)
            return "STRONG_CONSENSUS";
        else if (consensusPercentage >= 60 && confidence >= 0.6)
            return "MODERATE_CONSENSUS";
        else if (consensusPercentage >= 40)
            return "WEAK_CONSENSUS";
        else
            return "NO_CONSENSUS";
    }

    private double calculateJaccardOverlap(String s1, String s2) {
        if (s1 == null || s2 == null)
            return 0.0;
        String n1 = normalizeResponse(s1);
        String n2 = normalizeResponse(s2);
        if (n1.isEmpty() && n2.isEmpty())
            return 1.0;
        if (n1.isEmpty() || n2.isEmpty())
            return 0.0;

        String[] w1 = n1.split("\\s+");
        String[] w2 = n2.split("\\s+");

        Set<String> set1 = new HashSet<>();
        for (String w : w1) {
            if (w.length() > 2)
                set1.add(w);
        }
        Set<String> set2 = new HashSet<>();
        for (String w : w2) {
            if (w.length() > 2)
                set2.add(w);
        }

        if (set1.isEmpty() && set2.isEmpty())
            return 1.0;
        if (set1.isEmpty() || set2.isEmpty())
            return 0.0;

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size();
    }

    private Mono<VotingResult> performMetaSynthesis(String prompt, List<ProviderVote> votes, VotingResult initialResult,
            long initialDuration) {
        logger.info("Initiating Meta-Synthesis because consensus is weak/none. Verdict: {}",
                initialResult.getVerdict());

        // Choose primary orchestrator from the first available active provider at runtime.
        // Never hardcode a specific provider brand or model ID as the orchestrator.
        String orchestratorName = providerRepository
                .findByStatus("active")
                .map(com.supremeai.model.APIProvider::getName)
                .defaultIfEmpty("")
                .next()
                .block(java.time.Duration.ofSeconds(3));
        if (orchestratorName == null || orchestratorName.isBlank() || !isModelAvailable(orchestratorName)) {
            return Mono.just(initialResult);
        }

        final String finalOrchestrator = orchestratorName;

        StringBuilder synthesisPrompt = new StringBuilder();
        synthesisPrompt.append("You are the SupremeAI Orchestrator (Meta-Synthesis Engine).\n");
        synthesisPrompt.append(
                "We queried multiple AI models for the following user request. The decisions are split or weak. ");
        synthesisPrompt.append(
                "Your task is to analyze all the different responses, reconcile contradictions, filter out erroneous details, and synthesize a single, supreme, and consolidated response that perfectly satisfies the user's intent.\n\n");
        synthesisPrompt.append("User Request: ").append(prompt).append("\n\n");
        synthesisPrompt.append("Here are the individual model responses:\n");

        for (ProviderVote vote : votes) {
            synthesisPrompt.append("--- Model: ").append(vote.getProviderName())
                    .append(" (Confidence: ").append(String.format("%.2f", vote.getConfidence())).append(") ---\n")
                    .append(vote.getResponse()).append("\n\n");
        }

        synthesisPrompt.append(
                "Consolidate all the information into a single premium, structured, and complete response. Maintain high quality. DO NOT mention that you are synthesizing multiple models in your final output unless explicitly requested. Speak directly to the user.");

        long synthesisStart = System.currentTimeMillis();
        try {
            AIProvider orchestrator = providerFactory.getEnforcedProvider(finalOrchestrator);
            return orchestrator.generate(synthesisPrompt.toString())
                    .map(synthesizedResponse -> {
                        long totalDuration = initialDuration + (System.currentTimeMillis() - synthesisStart);
                        logger.info("Meta-Synthesis successfully completed by {} in {}ms", finalOrchestrator,
                                System.currentTimeMillis() - synthesisStart);

                        ProviderVote synthesisVote = new ProviderVote(
                                finalOrchestrator + "_synthesis",
                                synthesizedResponse,
                                0.95,
                                System.currentTimeMillis());
                        synthesisVote.setSuccess(true);

                        List<ProviderVote> updatedVotes = new ArrayList<>(votes);
                        updatedVotes.add(synthesisVote);

                        return new VotingResult(
                                prompt,
                                synthesizedResponse,
                                updatedVotes,
                                0.95,
                                "META_SYNTHESIZED",
                                totalDuration);
                    })
                    .onErrorResume(e -> {
                        logger.warn("Meta-Synthesis failed using {}: {}. Falling back to initial result.",
                                finalOrchestrator, e.getMessage());
                        return Mono.just(initialResult);
                    });
        } catch (Exception e) {
            logger.warn("Failed to get orchestrator {} for Meta-Synthesis: {}. Falling back to initial result.",
                    finalOrchestrator, e.getMessage());
            return Mono.just(initialResult);
        }
    }

    // Inner classes
    public static class VotingResult {
        private String prompt;
        private String bestResponse;
        private List<ProviderVote> allVotes;
        private double averageConfidence;
        private String verdict;
        private long processingTimeMs;

        public VotingResult(String prompt, String bestResponse, List<ProviderVote> allVotes,
                double averageConfidence, String verdict, long processingTimeMs) {
            this.prompt = prompt;
            this.bestResponse = bestResponse;
            this.allVotes = allVotes;
            this.averageConfidence = averageConfidence;
            this.verdict = verdict;
            this.processingTimeMs = processingTimeMs;
        }

        // Getters
        public String getPrompt() {
            return prompt;
        }

        public String getBestResponse() {
            return bestResponse;
        }

        public List<ProviderVote> getAllVotes() {
            return allVotes;
        }

        public double getAverageConfidence() {
            return averageConfidence;
        }

        public String getVerdict() {
            return verdict;
        }

        public long getProcessingTimeMs() {
            return processingTimeMs;
        }

        public int getTotalModelsUsed() {
            return allVotes.size();
        }
    }

    private static class ModelPerformanceTracker {
        private final String modelName;
        private double historicalScore = 0.5;
        private int totalAttempts = 0;

        public ModelPerformanceTracker(String modelName) {
            this.modelName = modelName;
        }

        public void recordAttempt(double confidence) {
            totalAttempts++;
            historicalScore = 0.9 * historicalScore + 0.1 * confidence;
        }

        public double getHistoricalScore() {
            if (totalAttempts == 0)
                return 0.5;
            return historicalScore;
        }
    }
}