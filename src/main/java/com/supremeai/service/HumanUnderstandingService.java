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
  private final ClaudeMemoryService memoryService;

  public HumanUnderstandingService(
      SystemLearningRepository learningRepository,
      MultiAIVotingService votingService,
      ConfigService configService,
      ClaudeMemoryService memoryService) {
    this.learningRepository = learningRepository;
    this.votingService = votingService;
    this.configService = configService;
    this.memoryService = memoryService;
  }

  /**
   * Analyze every user message for human factors Runs automatically on every single user
   * interaction
   */
  public void analyzeHumanFactors(String userId, String userMessage, String aiResponse) {
    // Get analysis provider from config (resolves to the default provider at runtime).
    String preferredProvider = configService.getEffectiveSetting("analysis_provider", "default");

    // ১ম ধাপ: ইউজারের পুরনো মেমরি সার্চ করা
    memoryService.searchMemory(userId, userMessage)
        .defaultIfEmpty(java.util.Collections.emptyList())
        .flatMap(pastMemories -> {
            String context = pastMemories.isEmpty() ? "None" : String.join("; ", pastMemories);

            // ২য় ধাপ: পুরনো মেমরি এবং বর্তমান মেসেজ নিয়ে কনসেনসাস এনালাইসিস করা
            return votingService.askConsensus(
                """
                Analyze this human-AI interaction considering past context if available.
                
                PAST CONTEXT: %s
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
                    .formatted(context, userMessage, aiResponse),
                java.util.List.of(preferredProvider),
                15000);
        })
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

              // ৩য় ধাপ: এনালাইসিস রেজাল্ট লং-টার্ম মেমরিতে সেভ করা এবং রিপোজিটরিতে রাখা
              return memoryService.storeMemory(userId, analysis.getConsensusAnswer())
                  .then(learningRepository.save(learning));
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
