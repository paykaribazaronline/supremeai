package com.supremeai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * NaturalLanguageQueryService - S3 Enhancement
 * Transforms user queries into more natural, human-like prompts for AI providers.
 * Uses AI to rephrase technical/short queries into contextual, conversational prompts.
 */
@Service
public class NaturalLanguageQueryService {

    private static final Logger log = LoggerFactory.getLogger(NaturalLanguageQueryService.class);

    @Autowired
    private AIProviderFactory providerFactory;

    @Value("${supremeai.active.providers:groq,openai,anthropic,ollama}")
    private String activeProviders;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Map<String, String> COMMON_PHRASES = new HashMap<>();

    static {
        // Common technical terms → natural phrases
        COMMON_PHRASES.put("auth", "authentication and user login system");
        COMMON_PHRASES.put("crud", "create, read, update, and delete operations");
        COMMON_PHRASES.put("api", "application programming interface");
        COMMON_PHRASES.put("db", "database");
        COMMON_PHRASES.put("ui", "user interface");
        COMMON_PHRASES.put("ux", "user experience");
        COMMON_PHRASES.put("jwt", "JSON Web Token based authentication");
        COMMON_PHRASES.put("rest", "RESTful web service");
        COMMON_PHRASES.put("graphql", "GraphQL query language");
        COMMON_PHRASES.put("jpa", "Java Persistence API for database operations");
        COMMON_PHRASES.put("orm", "Object-Relational Mapping");
        COMMON_PHRASES.put("dto", "Data Transfer Object");
        COMMON_PHRASES.put("ioc", "Inversion of Control container");
        COMMON_PHRASES.put("di", "Dependency Injection");
    }

    /**
     * Make user query more natural and human-like.
     * @param originalQuery The user's original input
     * @return Natural language enhanced query
     */
    public String humanizeQuery(String originalQuery) {
        if (originalQuery == null || originalQuery.trim().isEmpty()) {
            return originalQuery;
        }

        log.debug("Humanizing query: {}", originalQuery);

        String query = originalQuery.trim();

        // Step 1: Expand common abbreviations
        query = expandAbbreviations(query);

        // Step 2: Check if query is too short (< 10 words) → use AI to expand
        int wordCount = query.split("\\s+").length;
        if (wordCount < 10) {
            return enhanceWithAI(query, wordCount);
        }

        // Step 3: For longer queries, just clean up grammar/punctuation
        return cleanUpQuery(query);
    }

    /**
     * Build natural language prompt for AI providers.
     * This creates a more conversational, context-aware prompt.
     */
    public String buildNaturalPrompt(String userQuery, String context, String taskType) {
        String humanized = humanizeQuery(userQuery);

        Map<String, Object> promptContext = new LinkedHashMap<>();
        promptContext.put("userRequest", humanized);
        promptContext.put("context", context != null ? context : "general software development");
        promptContext.put("taskType", taskType != null ? taskType : "code generation");
        promptContext.put("tone", "helpful, conversational, and detailed");
        promptContext.put("constraints", List.of(
            "Be specific and actionable",
            "Provide code examples where appropriate",
            "Explain your reasoning"
        ));

        // Build natural language prompt
        return String.format("""
            As an experienced software developer, I'd like your help with the following request:

            **What I need:** %s

            **Context:** This is for %s.

            **Task type:** %s

            Please provide a thorough response that:
            - Addresses my request directly
            - Includes practical code examples
            - Explains the "why" behind your recommendations
            - Uses clear, conversational language

            If you need any clarification, please ask specific questions.
            """,
            humanized,
            promptContext.get("context"),
            promptContext.get("taskType")
        );
    }

    /**
     * Use AI to enhance short queries into natural language.
     */
    private String enhanceWithAI(String query, int wordCount) {
        try {
            String[] providers = activeProviders.split(",");
            AIProvider provider = providerFactory.getProvider(providers[0].trim());

            String enhancementPrompt = String.format("""
                Rewrite the following user request into a natural, conversational, and detailed prompt
                that a human developer would ask. Expand any abbreviations, add context, and make it
                sound like something a person would actually say.

                Original: "%s"

                Requirements:
                - Keep the core meaning exactly the same
                - Expand technical abbreviations (e.g., "auth" → "authentication system")
                - Add natural language connectors (e.g., "I'd like to...", "Can you help me...")
                - Make it 5-10 sentences if possible
                - Sound like a real human developer asking for help

                Return ONLY the rewritten prompt, nothing else.
                """, query);

            String enhanced = provider.generate(enhancementPrompt);
            log.info("Query humanized: '{}' → '{}'", query, enhanced);
            return enhanced;
        } catch (Exception e) {
            log.warn("AI enhancement failed, using fallback: {}", e.getMessage());
            return fallbackHumanize(query);
        }
    }

    /**
     * Expand common abbreviations in text.
     */
    private String expandAbbreviations(String text) {
        String result = text;
        for (Map.Entry<String, String> entry : COMMON_PHRASES.entrySet()) {
            String regex = "\\b" + entry.getKey() + "\\b";
            result = result.replaceAll("(?i)" + regex, entry.getValue());
        }
        return result;
    }

    /**
     * Clean up query grammar and punctuation.
     */
    private String cleanUpQuery(String query) {
        // Ensure first letter is capitalized
        if (query.length() > 0 && Character.isLowerCase(query.charAt(0))) {
            query = Character.toUpperCase(query.charAt(0)) + query.substring(1);
        }

        // Ensure ends with appropriate punctuation
        if (!query.endsWith(".") && !query.endsWith("?") && !query.endsWith("!")) {
            query += ".";
        }

        return query;
    }

    /**
     * Fallback humanization when AI is unavailable.
     */
    private String fallbackHumanize(String query) {
        StringBuilder sb = new StringBuilder();
        sb.append("I'd like to ");
        
        // Convert imperative to conversational
        if (query.toLowerCase().matches("^(create|build|make|generate|develop|code).*")) {
            sb.append("request help to ");
        }

        sb.append(query.toLowerCase());
        
        // Add context if query is very short
        if (query.split("\\s+").length < 5) {
            sb.append(" for my project, including best practices and code examples");
        }

        sb.append(" Could you please provide a detailed solution?");
        
        return cleanUpQuery(sb.toString());
    }

    /**
     * Generate follow-up clarification questions in natural language.
     */
    public List<String> generateNaturalFollowUps(String originalQuery, String context) {
        List<String> questions = new ArrayList<>();
        
        // Template-based natural questions
        if (originalQuery.toLowerCase().contains("app") || originalQuery.toLowerCase().contains("application")) {
            questions.add("What platform should this app target? (Web, mobile, desktop, or all?)");
            questions.add("Who will be the primary users of this application?");
            questions.add("Are there any specific features you'd like to prioritize?");
        }

        if (originalQuery.toLowerCase().contains("api") || originalQuery.toLowerCase().contains("service")) {
            questions.add("What kind of data will your API handle?");
            questions.add("Do you need real-time updates, or is periodic refresh okay?");
            questions.add("Should the API be public, private, or have user-specific access?");
        }

        if (originalQuery.toLowerCase().contains("database") || originalQuery.toLowerCase().contains("db")) {
            questions.add("How much data do you expect to store initially?");
            questions.add("Do you need complex queries, or mostly simple lookups?");
            questions.add("Will you need to scale to handle lots of concurrent users?");
        }

        return questions;
    }

    /**
     * Log humanization metrics for learning.
     */
    public void logHumanizationMetrics(String original, String humanized) {
        int originalWords = original.split("\\s+").length;
        int newWords = humanized.split("\\s+").length;
        double expansionRatio = (double) newWords / originalWords;

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("originalLength", original.length());
        metrics.put("humanizedLength", humanized.length());
        metrics.put("originalWords", originalWords);
        metrics.put("humanizedWords", newWords);
        metrics.put("expansionRatio", expansionRatio);
        metrics.put("timestamp", new Date());

        log.info("Humanization metrics: {}", metrics);
    }
}
