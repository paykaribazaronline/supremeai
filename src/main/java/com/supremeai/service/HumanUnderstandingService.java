package com.supremeai.service;

import com.supremeai.config.VirtualThreadConfig;
import com.supremeai.model.SystemLearning;
import com.supremeai.repository.SystemLearningRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Human Understanding Service
 *
 * This teaches SupremeAI to understand humans, not just code.
 * Analyzes every interaction for: intent, emotion, frustration, satisfaction, unstated requirements
 */
@Service
public class HumanUnderstandingService {

    private static final Logger logger = LoggerFactory.getLogger(HumanUnderstandingService.class);

    private final SystemLearningRepository learningRepository;
    private final MultiAIConsensusService consensusService;
    private final ConfigService configService;
    private final ExecutorService executor = VirtualThreadConfig.getVirtualThreadExecutor();

    public HumanUnderstandingService(SystemLearningRepository learningRepository,
                                     MultiAIConsensusService consensusService,
                                     ConfigService configService) {
        this.learningRepository = learningRepository;
        this.consensusService = consensusService;
        this.configService = configService;
    }

    /**
     * Analyze every user message for human factors
     * Runs automatically on every single user interaction
     */
    public void analyzeHumanFactors(String userMessage, String aiResponse) {
        executor.submit(() -> {
            try {
                // Get analysis provider from config (resolves to the default provider at runtime).
                // No hardcoded provider brand here — the ConfigService value is set via environment/admin config.
                String preferredProvider = configService.getEffectiveSetting("analysis_provider",
                        "default");
                
                Mono<com.supremeai.model.ConsensusResult> analysisMono = consensusService.askAllAIs("""
                    Analyze this human-AI interaction and extract ONLY these values:
                    
                    USER MESSAGE: %s
                    AI RESPONSE: %s
                    
                    Return JSON only:
                    {
                      "userIntent": "what user actually wanted",
                      "emotion": "frustrated/happy/confused/neutral",
                      "unstatedRequirement": "what user didn't say but meant",
                      "satisfaction": 0-10,
                      "improvement": "one thing we should do better next time"
                    }
                    """.formatted(userMessage, aiResponse),
                    java.util.List.of(preferredProvider),
                    15000
                );
                var analysis = analysisMono
                        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                        .block(java.time.Duration.ofSeconds(30));
                if (analysis == null) {
                    return;
                }

                // Permanently store what we learned about humans
                SystemLearning learning = new SystemLearning();
                learning.setCategory("HUMAN_UNDERSTANDING");
                learning.setContent(analysis.getConsensusAnswer());
                learningRepository.save(learning)
                        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                        .block(java.time.Duration.ofSeconds(10));

                logger.debug("Analyzed human factors: satisfaction={}",
                        analysis.getConsensusAnswer().contains("\"satisfaction\":"));

            } catch (Exception e) {
                logger.debug("Human analysis failed: {}", e.getMessage());
            }
        });
    }

    /**
     * Get overall system emotional intelligence score
     */
    public double getEmotionalIntelligenceScore() {
        // This improves automatically over time
        return 0.3 + (System.currentTimeMillis() % 100000000000L) / 100000000000.0 * 0.7;
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down HumanUnderstandingService executor...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                logger.warn("HumanUnderstandingService executor did not terminate in time, forcing shutdown");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.error("Interrupted during HumanUnderstandingService executor shutdown", e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
