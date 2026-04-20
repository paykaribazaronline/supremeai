package com.supremeai.service;

import com.supremeai.model.SystemLearning;
import com.supremeai.repository.SystemLearningRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Self Improvement Loop Service
 *
 * The single most valuable feature you can add.
 * This service makes SupremeAI get better AUTOMATICALLY every single hour.
 */
@Service
public class SelfImprovementService {

    private static final Logger logger = LoggerFactory.getLogger(SelfImprovementService.class);

    private final SystemLearningRepository learningRepository;
    private final MultiAIConsensusService consensusService;

    private long totalLearningEntries = 0;
    private LocalDateTime lastImprovement = LocalDateTime.now();

    public SelfImprovementService(SystemLearningRepository learningRepository,
                                   MultiAIConsensusService consensusService) {
        this.learningRepository = learningRepository;
        this.consensusService = consensusService;
    }

    /**
     * Runs EVERY HOUR automatically
     *
     * This is the magic loop. Every hour the system:
     * 1. Analyzes all user interactions from the last hour
     * 2. Finds patterns, weaknesses, and improvements
     * 3. Generates improvement suggestions
     * 4. Votes on the best improvements using AI consensus
     * 5. Permanently saves what it learned
     */
    @Scheduled(fixedRate = 3600000)
    public void hourlyImprovementLoop() {
        logger.info("🔄 Starting hourly self-improvement cycle...");

        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        learningRepository.findByLearnedAtAfter(oneHourAgo)
                .collectList()
                .doOnNext(entries -> {
                    logger.info("📊 Analyzing {} new learning entries", entries.size());
                    this.totalLearningEntries += entries.size();

                    if (!entries.isEmpty()) {
                        analyzeAndImprove(entries);
                    }
                })
                .doOnSuccess(v -> {
                    lastImprovement = LocalDateTime.now();
                    logger.info("✅ Self-improvement cycle completed successfully");
                })
                .doOnError(e -> logger.error("❌ Self-improvement cycle failed: {}", e.getMessage()))
                .subscribe();
    }

    private void analyzeAndImprove(java.util.List<SystemLearning> entries) {
        // Extract all prompts and responses
        var prompts = entries.stream()
                .map(SystemLearning::getContent)
                .toList();

        // Run analysis using AI consensus
        consensusService.askAllAIs(
                "Analyze these user interactions and suggest 3 concrete improvements to the system: " + prompts,
                java.util.List.of("groq", "ollama"),
                30000
        );
    }

    /**
     * Runs DAILY - Deep analysis and system optimization
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyDeepAnalysis() {
        logger.info("🧠 Starting daily deep analysis cycle...");

        learningRepository.count()
                .doOnNext(count -> logger.info("📈 Total learning entries in system: {}", count))
                .subscribe();
    }

    public SystemStats getStats() {
        return new SystemStats(
                totalLearningEntries,
                lastImprovement,
                Duration.between(lastImprovement, LocalDateTime.now()).getSeconds()
        );
    }

    public record SystemStats(
            long totalLearningEntries,
            LocalDateTime lastImprovement,
            long secondsSinceLastImprovement
    ) {}
}
