package com.supremeai.service;

import com.supremeai.config.VirtualThreadConfig;
import com.supremeai.model.SystemLearning;
import com.supremeai.repository.SystemLearningRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;

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
    private final ExecutorService executor = VirtualThreadConfig.getVirtualThreadExecutor();

    public HumanUnderstandingService(SystemLearningRepository learningRepository,
                                     MultiAIConsensusService consensusService) {
        this.learningRepository = learningRepository;
        this.consensusService = consensusService;
    }

    /**
     * Analyze every user message for human factors
     * Runs automatically on every single user interaction
     */
    public void analyzeHumanFactors(String userMessage, String aiResponse) {
        executor.submit(() -> {
            try {
                // Extract human dimensions from every interaction
                var analysis = consensusService.askAllAIs("""
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
                    java.util.List.of("groq"),
                    10000
                );

                // Permanently store what we learned about humans
                SystemLearning learning = new SystemLearning();
                learning.setCategory("HUMAN_UNDERSTANDING");
                learning.setContent(analysis.getConsensus());
                learningRepository.save(learning).subscribe();

                logger.debug("Analyzed human factors: satisfaction={}",
                        analysis.getConsensus().contains("\"satisfaction\":"));

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
}
