package com.supremeai.service;

import com.supremeai.model.SystemLearning;
import com.supremeai.repository.SystemLearningRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Self Improvement Loop Service
 *
 * <p>The single most valuable feature you can add. This service makes SupremeAI get better
 * AUTOMATICALLY every single hour.
 */
@Service
public class SelfImprovementService {

  private static final Logger logger = LoggerFactory.getLogger(SelfImprovementService.class);

  private final SystemLearningRepository learningRepository;
  private final MultiAIVotingService votingService;
  private final ConfigService configService;
  private final com.supremeai.admin.ProviderAdminService providerAdminService;

  private long totalLearningEntries = 0;
  private LocalDateTime lastImprovement =
      LocalDateTime.now().minusHours(1); // Set to allow first run immediately if needed
  private boolean isRunning = false;

  public SelfImprovementService(
      SystemLearningRepository learningRepository,
      MultiAIVotingService votingService,
      ConfigService configService,
      com.supremeai.admin.ProviderAdminService providerAdminService) {
    this.learningRepository = learningRepository;
    this.votingService = votingService;
    this.configService = configService;
    this.providerAdminService = providerAdminService;
  }

  /** Runs periodically to check if it's time for improvement based on admin config. */
  @Scheduled(fixedDelay = 60000) // check every minute
  public void hourlyImprovementLoop() {
    if (isRunning) {
      return;
    }

    long intervalMinutes = configService.getEffectiveSetting("learning_interval_minutes", 60L);
    if (Duration.between(lastImprovement, LocalDateTime.now()).toMinutes() < intervalMinutes) {
      return;
    }

    logger.info("🔄 Starting dynamic self-improvement cycle...");
    isRunning = true;

    LocalDateTime intervalAgo = LocalDateTime.now().minusMinutes(intervalMinutes);

    learningRepository
        .findAll()
        .filter(entry -> entry.getLearnedAt() != null && entry.getLearnedAt().isAfter(intervalAgo))
        .collectList()
        .doOnNext(
            entries -> {
              logger.info("📊 Analyzing {} new learning entries", entries.size());
              this.totalLearningEntries += entries.size();

              if (!entries.isEmpty()) {
                analyzeAndImprove(entries);
              }
            })
        .doOnSuccess(
            v -> {
              lastImprovement = LocalDateTime.now();
              isRunning = false;
              logger.info("✅ Self-improvement cycle completed successfully");
            })
        .doOnError(
            e -> {
              isRunning = false;
              logger.error("❌ Self-improvement cycle failed: {}", e.getMessage());
            })
        .subscribe();
  }

  /** Ingests scraped issues into the system learning repository. */
  public void ingestScrapedIssues(
      java.util.List<com.supremeai.learning.active.ActiveInternetScraper.ScrapedIssue> issues) {
    if (issues == null || issues.isEmpty()) {
      return;
    }

    logger.info("📥 Ingesting {} scraped issues into system learning", issues.size());

    for (var issue : issues) {
      SystemLearning learning = new SystemLearning();
      String id =
          "scraped_" + System.currentTimeMillis() + "_" + Math.abs(issue.getTitle().hashCode());
      learning.setId(id);
      learning.setTopic(issue.getTitle());
      learning.setContent(issue.getSolution());
      learning.setCategory("INTERNET_KNOWLEDGE");
      learning.setLearningType("ECOSYSTEM");
      learning.setSources(java.util.List.of(issue.getSource()));
      learning.setConfidenceScore(issue.getRawConfidence());
      learning.setLearnedAt(LocalDateTime.now());
      learning.setPermanent(false);
      learning.setMetadata(
          java.util.Map.of(
              "source", issue.getSource(),
              "authority", issue.getSourceAuthority()));

      learningRepository
          .save(learning)
          .subscribe(
              saved -> logger.debug("✅ Saved scraped knowledge: {}", saved.getTopic()),
              err -> logger.error("❌ Failed to save scraped knowledge: {}", err.getMessage()));
    }
  }

  private void analyzeAndImprove(java.util.List<SystemLearning> entries) {
    // Extract all prompts and responses
    var prompts = entries.stream().map(SystemLearning::getContent).toList();

    // Get active providers for voting
    providerAdminService
        .getAllProviders()
        .filter(p -> p.isActive() && p.isValidated())
        .map(com.supremeai.model.APIProvider::getId)
        .collectList()
        .flatMap(
            activeProviders -> {
              if (activeProviders.size() < 2) {
                logger.warn(
                    "⚠️ Not enough active providers for consensus voting. Need at least 2.");
                return Mono.empty();
              }

              logger.info("🗳️ Requesting consensus from providers: {}", activeProviders);
              return votingService.askConsensus(
                  "Analyze these user interactions and suggest 3 concrete improvements to the system: "
                      + prompts,
                  activeProviders,
                  30000);
            })
        .subscribe(
            result -> {
              if (result != null) {
                logger.info("🧠 AI Improvement Consensus Result: {}", result.getConsensusAnswer());
              }
            },
            error ->
                logger.error(
                    "❌ AI Consensus failed during self-improvement: {}", error.getMessage()));
  }

  /** Runs DAILY - Deep analysis and system optimization */
  @Scheduled(cron = "0 0 2 * * ?")
  public void dailyDeepAnalysis() {
    logger.info("🧠 Starting daily deep analysis cycle...");

    learningRepository
        .count()
        .doOnNext(count -> logger.info("📈 Total learning entries in system: {}", count))
        .subscribe();
  }

  public SystemStats getStats() {
    return new SystemStats(
        totalLearningEntries,
        lastImprovement,
        Duration.between(lastImprovement, LocalDateTime.now()).getSeconds());
  }

  public record SystemStats(
      long totalLearningEntries, LocalDateTime lastImprovement, long secondsSinceLastImprovement) {}
}
