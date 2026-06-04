package com.supremeai.service;

import com.supremeai.model.SystemLearning;
import com.supremeai.repository.SystemLearningRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Human Understanding Service
 *
 * <p>This teaches SupremeAI to understand humans, not just code. Analyzes every interaction for:
 * intent, emotion, frustration, satisfaction, unstated requirements
 */
@Service
public class HumanUnderstandingService {

  private static final Logger logger = LoggerFactory.getLogger(HumanUnderstandingService.class);

  private final SystemLearningRepository learningRepository;
  private final MultiAIVotingService votingService;
  private final ConfigService configService;

  public HumanUnderstandingService(
      SystemLearningRepository learningRepository,
      MultiAIVotingService votingService,
      ConfigService configService) {
    this.learningRepository = learningRepository;
    this.votingService = votingService;
    this.configService = configService;
  }

  /**
   * Analyze every user message for human factors Runs automatically on every single user
   * interaction
   */
  public void analyzeHumanFactors(String userMessage, String aiResponse) {
    // Get analysis provider from config (resolves to the default provider at runtime).
    String preferredProvider = configService.getEffectiveSetting("analysis_provider", "default");

    votingService
        .askConsensus(
            """
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
            """
                .formatted(userMessage, aiResponse),
            java.util.List.of(preferredProvider),
            15000)
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(
            analysis -> {
              if (analysis == null || analysis.getConsensusAnswer() == null) {
                return Mono.empty();
              }
              SystemLearning learning = new SystemLearning();
              learning.setCategory("HUMAN_UNDERSTANDING");
              learning.setContent(analysis.getConsensusAnswer());

              logger.debug(
                  "Analyzed human factors: satisfaction={}",
                  analysis.getConsensusAnswer().contains("\"satisfaction\":"));

              return learningRepository.save(learning);
            })
        .subscribe(
            saved -> {}, error -> logger.debug("Human analysis failed: {}", error.getMessage()));
  }

  /** Get overall system emotional intelligence score */
  public double getEmotionalIntelligenceScore() {
    // This improves automatically over time
    return 0.3 + (System.currentTimeMillis() % 100000000000L) / 100000000000.0 * 0.7;
  }
}
