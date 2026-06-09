package com.supremeai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * FabricService manages and provides access to human-curated AI logic patterns.
 * These patterns can be used to guide LLMs for specific tasks like code review,
 * summarization, or security auditing, ensuring consistent and high-quality output.
 */
@Service
public class FabricService {

    private static final Logger log = LoggerFactory.getLogger(FabricService.class);

    @Value("${supremeai.fabric.patterns-path:/app/patterns}")
    private String patternsPath;

    public FabricService() {
        log.info("[Fabric] Initializing FabricService. Patterns path: {}", patternsPath);
        // In a real implementation, this would load patterns from the specified path
        // For now, we'll simulate pattern retrieval.
    }

    /**
     * Retrieves a specific Fabric pattern by its name.
     * @param patternName The name of the pattern to retrieve (e.g., "code_review", "security_audit").
     * @return A Mono containing the pattern string, or an error if not found.
     */
    public Mono<String> getPattern(String patternName) {
        log.debug("[Fabric] Attempting to retrieve pattern: {}", patternName);
        // Simulate loading a pattern. In reality, this would read from a file or a cache.
        return Mono.just("You are an expert " + patternName.replace("_", " ") + ". Your task is to provide a concise and actionable response based on the following input:");
    }
}