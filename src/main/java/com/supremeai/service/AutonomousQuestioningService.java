package com.supremeai.service;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Analyzes user prompts and generates intelligent clarifying questions
 * to reduce ambiguity before code generation.
 */
@Service
public class AutonomousQuestioningService {

    // Keywords indicating potential ambiguity
    private static final Set<String> AMBIGUOUS_TERMS = Set.of(
        "something", "anything", "whatever", "some", "maybe", "perhaps",
        "like", "similar", "etc", "and so on", "you know"
    );

    // Missing info detectors
    private static final List<ClarityRule> CLARITY_RULES = List.of(
        new ClarityRule(
            "missing_scope",
            "Missing project scope",
            List.of("build", "create", "make", "develop"),
            "What is the scale/scope of this project? (small app, enterprise system, microservice?)"
        ),
        new ClarityRule(
            "missing_tech_stack",
            "No technology preferences specified",
            List.of("using", "with", "in", "on"),
            "Which tech stack do you prefer? (React, Vue, Node, Python, Java, etc.)"
        ),
        new ClarityRule(
            "missing_constraints",
            "No constraints mentioned",
            List.of("fast", "quick", "simple", "easy"),
            "Any constraints to consider? (budget, timeline, existing systems, performance?)"
        ),
        new ClarityRule(
            "missing_data",
            "Data requirements unclear",
            List.of("data", "store", "save", "track"),
            "What data will be stored/processed? User info, transactions, analytics?"
        ),
        new ClarityRule(
            "missing_users",
            "Target users undefined",
            List.of("users", "customers", "admins", "people"),
            "Who are the target users? (admin panel, customer-facing, internal?)"
        ),
        new ClarityRule(
            "missing_platform",
            "Platform not specified",
            List.of("web", "mobile", "app", "desktop", "api"),
            "Which platform? (web app, mobile app, API service, CLI tool?)"
        )
    );

    /**
     * Analyze a prompt and return clarifying questions.
     * Returns empty list if prompt appears clear.
     */
    public List<ClarifyingQuestion> analyzePrompt(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return List.of(new ClarifyingQuestion(
                "Empty prompt",
                "Please describe what you want to build.",
                1.0
            ));
        }

        String lowerPrompt = prompt.toLowerCase();
        List<ClarifyingQuestion> questions = new ArrayList<>();

        // Check for vague terms
        for (String term : AMBIGUOUS_TERMS) {
            // Use regex word boundaries to avoid matching substrings (e.g. "some" in "awesome")
            if (lowerPrompt.matches(".*\\b" + java.util.regex.Pattern.quote(term) + "\\b.*")) {
                questions.add(new ClarifyingQuestion(
                    "Vague language detected",
                    "You used '" + term + "'. Can you be more specific?",
                    0.7
                ));
                break; // Only flag once
            }
        }

        // Check clarity rules
        for (ClarityRule rule : CLARITY_RULES) {
            boolean triggered = false;
            for (String keyword : rule.keywords) {
                if (lowerPrompt.contains(keyword)) {
                    // Check if the topic is actually addressed
                    if (!isTopicAddressed(lowerPrompt, rule.topic)) {
                        questions.add(new ClarifyingQuestion(
                            rule.name,
                            rule.question,
                            0.8
                        ));
                        triggered = true;
                        break;
                    }
                }
            }
        }

        // Check for missing key details
        if (prompt.length() < 50) {
            questions.add(new ClarifyingQuestion(
                "Brief prompt",
                "Your prompt is quite short. Can you provide more details about what you need?",
                0.6
            ));
        }

        // Remove duplicates and sort by confidence
        return questions.stream()
                .distinct()
                .sorted(Comparator.comparingDouble(q -> -q.confidence))
                .limit(5) // Max 5 questions
                .toList();
    }

    private boolean isTopicAddressed(String prompt, String topic) {
        // Simple check if topic keywords appear near relevant terms
        String[] words = prompt.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            if (words[i].contains(topic.substring(0, Math.min(4, topic.length())))) {
                // Look for negation or confirmation nearby
                if (i > 0 && Set.of("no", "not", "without", "excluding").contains(words[i-1])) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Generate a concise summary of what's clear and what needs clarification.
     */
    public Map<String, Object> getPromptAnalysis(String prompt) {
        List<ClarifyingQuestion> questions = analyzePrompt(prompt);
        boolean isClear = questions.isEmpty();
        
        return Map.of(
            "promptLength", prompt != null ? prompt.length() : 0,
            "isClear", isClear,
            "confidence", isClear ? 0.9 : 0.5,
            "questions", questions,
            "suggestedQuestions", questions.stream()
                    .filter(q -> q.confidence >= 0.7)
                    .limit(3)
                    .toList(),
            "timestamp", System.currentTimeMillis()
        );
    }

    // ── Data Classes ──────────────────────────────────────────────────────────

    public static class ClarifyingQuestion {
        public String issue;
        public String question;
        public double confidence; // 0-1
        public List<String> followUps = List.of();

        public ClarifyingQuestion(String issue, String question, double confidence) {
            this.issue = issue;
            this.question = question;
            this.confidence = confidence;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ClarifyingQuestion)) return false;
            ClarifyingQuestion that = (ClarifyingQuestion) o;
            return issue.equals(that.issue);
        }

        @Override
        public int hashCode() {
            return Objects.hash(issue);
        }
    }

    private static class ClarityRule {
        String id;
        String name;
        List<String> keywords;
        String question;
        String topic;

        public ClarityRule(String id, String name, List<String> keywords, String question) {
            this.id = id;
            this.name = name;
            this.keywords = keywords;
            this.question = question;
            this.topic = id.substring(id.indexOf('_') + 1);
        }
    }
}
