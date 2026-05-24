package com.supremeai.service;

import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Context Summarizer Service - Cost Optimization Agent.
 * Compresses long conversation histories into concise "Brain Summaries" to reduce token costs.
 */
@Service
public class ContextSummarizerService {

    private static final Logger log = LoggerFactory.getLogger(ContextSummarizerService.class);

    @Autowired
    private AIProviderFactory providerFactory;

    private static final int TOKEN_LIMIT_THRESHOLD = 4000;

    /**
     * Summarize a long conversation history.
     */
    public Mono<String> summarizeContext(String fullHistory) {
        // Only summarize if history is long enough
        if (fullHistory == null || fullHistory.length() < TOKEN_LIMIT_THRESHOLD) {
            return Mono.just(fullHistory);
        }

        log.info("💸 History exceeds token threshold. Triggering Brain Summary for cost optimization...");

        try {
            // Use the healthiest/cheapest default provider for summarization
            AIProvider provider = providerFactory.getDefaultProvider();

            String summarizationPrompt = String.format("""
                Summarize the following conversation history into a concise 'Brain Summary'.
                Maintain all technical decisions, architecture choices, and critical logic details.
                Discard conversational filler.

                History:
                %s

                Concise Brain Summary:
                """, fullHistory);

            return provider.generate(summarizationPrompt)
                .map(summary -> {
                    log.info("✅ Context compressed. Saving ~{}% in token costs.", 
                        (1.0 - (double)summary.length() / fullHistory.length()) * 100);
                    return "[BRAIN SUMMARY]: " + summary;
                });
        } catch (Exception e) {
            log.warn("Summarization failed, returning original history: {}", e.getMessage());
            return Mono.just(fullHistory);
        }
    }

    /**
     * Check if context needs summarization based on estimated token count (char count / 4).
     */
    public boolean needsSummarization(String content) {
        return content != null && (content.length() / 4) > TOKEN_LIMIT_THRESHOLD;
    }
}
