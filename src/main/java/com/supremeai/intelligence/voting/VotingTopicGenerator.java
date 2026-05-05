package com.supremeai.intelligence.voting;

import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Intelligent Engine that decides WHAT to vote on and HOW to ask the question.
 */
@Service
public class VotingTopicGenerator {

    /**
     * Analyzes a proposed major change and formulates a precise question
     * to ask the "Council of AI Models" for a vote.
     */
    public VotingTopic generateTopicForMajorChange(String changeType, String codeSnippet) {
        
        String topicId = UUID.randomUUID().toString();

        if (changeType.equals("DATABASE_MIGRATION")) {
            return new VotingTopic(
                topicId,
                "ARCHITECTURE",
                "Code involves altering existing database tables: \n" + codeSnippet,
                "Does this database migration script look safe for production? Will it lock the tables or drop existing data?"
            );
        }
        
        if (changeType.equals("AUTH_LOGIC_CHANGE")) {
            return new VotingTopic(
                topicId,
                "SECURITY",
                "Code changes authentication or token validation: \n" + codeSnippet,
                "Does this authentication logic introduce any OWASP Top 10 vulnerabilities, specifically JWT signature bypass or timing attacks?"
            );
        }
        
        if (changeType.equals("NEW_THIRD_PARTY_LIB")) {
             return new VotingTopic(
                topicId,
                "MAINTAINABILITY",
                "Code introduces a new heavy dependency: \n" + codeSnippet,
                "Is this third-party library actively maintained, and does it bloat the application size unnecessarily compared to native solutions?"
            );
        }

        // Generic catch-all for complex code
        return new VotingTopic(
            topicId,
            "GENERAL_REVIEW",
            "Complex algorithm proposed: \n" + codeSnippet,
            "Review this logic for edge cases (nulls, empty lists, infinite loops) and O(n) performance issues."
        );
    }
}