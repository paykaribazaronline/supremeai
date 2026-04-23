package com.supremeai.service;

import com.supremeai.learning.knowledge.GlobalKnowledgeBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Enhanced Autonomous Questioning Service with ML-based ambiguity detection
 * and adaptive learning from past clarifications.
 */
@Service
public class EnhancedAutonomousQuestioningService {

    private static final Logger log = LoggerFactory.getLogger(EnhancedAutonomousQuestioningService.class);

    @Autowired
    private GlobalKnowledgeBase globalKnowledgeBase;

    // Store clarification history for learning
    private final Map<String, List<ClarificationRecord>> userClarificationHistory = new ConcurrentHashMap<>();
    private final Map<String, Double> questionEffectiveness = new ConcurrentHashMap<>();

    // NLP-based ambiguity features
    private static final Set<String> VAGUE_PRONOUNS = Set.of("it", "that", "this", "they", "them", "something", "anything");
    private static final Set<String> UNCLEAR_QUANTIFIERS = Set.of("some", "many", "few", "several", "various");
    private static final Set<String> HEDGING_WORDS = Set.of("maybe", "perhaps", "possibly", "might", "could", "would", "should");

    // Clarity rules with ML-based confidence scoring
    private static final List<EnhancedClarityRule> ENHANCED_RULES = List.of(
        new EnhancedClarityRule("scope", "project scope",
            List.of("build", "create", "make", "develop", "implement"),
            "What is the scale/scope? (small app, enterprise system, microservice?)",
            0.85),
        new EnhancedClarityRule("tech_stack", "technology stack",
            List.of("using", "with", "in", "on", "framework"),
            "Which tech stack? (React, Vue, Node, Python, Java, etc.)",
            0.80),
        new EnhancedClarityRule("constraints", "project constraints",
            List.of("fast", "quick", "simple", "easy", "cheap"),
            "Any constraints? (budget, timeline, existing systems, performance?)",
            0.75),
        new EnhancedClarityRule("data_requirements", "data requirements",
            List.of("data", "store", "save", "track", "manage"),
            "What data will be stored/processed? (user info, transactions, analytics?)",
            0.82),
        new EnhancedClarityRule("target_users", "target users",
            List.of("users", "customers", "admins", "people"),
            "Who are the target users? (admin panel, customer-facing, internal?)",
            0.78),
        new EnhancedClarityRule("platform", "deployment platform",
            List.of("web", "mobile", "app", "desktop", "api"),
            "Which platform? (web app, mobile app, API service, CLI tool?)",
            0.81),
        new EnhancedClarityRule("integration", "integration requirements",
            List.of("integrate", "connect", "sync", "api", "webhook"),
            "Any integration requirements? (third-party APIs, payment gateways, etc.?)",
            0.76),
        new EnhancedClarityRule("security", "security requirements",
            List.of("secure", "authentication", "authorization", "login", "permission"),
            "Any security requirements? (user auth, data encryption, compliance?)",
            0.79)
    );

    /**
     * Analyze prompt with ML-based ambiguity detection and adaptive questioning.
     */
    public AnalysisResult analyzePromptWithML(String prompt, String userId) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return new AnalysisResult(prompt, 0.0,
                List.of(new ClarifyingQuestion("empty_prompt",
                    "Please describe what you want to build.", 1.0, List.of())),
                Map.of("isEmpty", true));
        }

        String lowerPrompt = prompt.toLowerCase(Locale.ROOT);
        List<ClarifyingQuestion> questions = new ArrayList<>();
        Map<String, Object> analysisMetadata = new HashMap<>();

        // 1. ML-based ambiguity scoring
        double ambiguityScore = calculateAmbiguityScore(lowerPrompt, prompt);
        analysisMetadata.put("ambiguityScore", ambiguityScore);

        // 2. Check for vague terms with confidence scoring
        checkVagueTerms(lowerPrompt, questions);

        // 3. Apply enhanced clarity rules
        applyClarityRules(lowerPrompt, questions, userId);

        // 4. Check user's past clarification patterns
        adaptQuestionsToUser(questions, userId, prompt);

        // 5. Add follow-up questions based on initial answers
        addFollowUpQuestions(questions, prompt);

        // 6. Sort by effectiveness and confidence
        List<ClarifyingQuestion> finalQuestions = questions.stream()
            .distinct()
            .sorted((q1, q2) -> Double.compare(
                q2.confidence * getQuestionEffectiveness(q2.issue),
                q1.confidence * getQuestionEffectiveness(q1.issue)))
            .limit(5)
            .collect(Collectors.toList());

        // Calculate overall clarity
        double clarityScore = calculateClarityScore(finalQuestions, ambiguityScore);
        analysisMetadata.put("clarityScore", clarityScore);
        analysisMetadata.put("questionCount", finalQuestions.size());

        return new AnalysisResult(prompt, clarityScore, finalQuestions, analysisMetadata);
    }

    /**
     * Calculate ML-based ambiguity score using multiple features.
     */
    private double calculateAmbiguityScore(String lowerPrompt, String originalPrompt) {
        double score = 0.0;

        // Feature 1: Vocabulary diversity (lower = more ambiguous)
        String[] words = originalPrompt.split("\\s+");
        Set<String> uniqueWords = new HashSet<>(Arrays.asList(words));
        double diversityRatio = words.length > 0 ? (double) uniqueWords.size() / words.length : 0;
        score += (1.0 - diversityRatio) * 0.3;

        // Feature 2: Sentence structure complexity
        long sentenceCount = originalPrompt.split("[.!?]+").length;
        double avgWordsPerSentence = sentenceCount > 0 ? words.length / (double) sentenceCount : words.length;
        if (avgWordsPerSentence < 5) score += 0.2; // Very short sentences = ambiguous

        // Feature 3: Presence of vague terms
        long vagueCount = VAGUE_PRONOUNS.stream().filter(lowerPrompt::contains).count();
        score += Math.min(vagueCount * 0.15, 0.45);

        // Feature 4: Quantifier ambiguity
        long quantifierCount = UNCLEAR_QUANTIFIERS.stream().filter(lowerPrompt::contains).count();
        score += Math.min(quantifierCount * 0.1, 0.3);

        // Feature 5: Hedging words (indicates uncertainty)
        long hedgeCount = HEDGING_WORDS.stream().filter(lowerPrompt::contains).count();
        score += Math.min(hedgeCount * 0.08, 0.24);

        // Feature 6: Prompt length (too short = ambiguous)
        if (originalPrompt.length() < 30) score += 0.3;
        else if (originalPrompt.length() < 50) score += 0.15;

        return Math.min(score, 1.0);
    }

    /**
     * Check for vague terms and add questions.
     */
    private void checkVagueTerms(String lowerPrompt, List<ClarifyingQuestion> questions) {
        for (String term : VAGUE_PRONOUNS) {
            if (lowerPrompt.contains(term)) {
                questions.add(new ClarifyingQuestion(
                    "vague_term_" + term,
                    "You used '" + term + "'. Can you be more specific?",
                    0.75,
                    List.of("What specifically are you referring to?")
                ));
            }
        }
    }

    /**
     * Apply clarity rules and check if topics are addressed.
     */
    private void applyClarityRules(String lowerPrompt, List<ClarifyingQuestion> questions, String userId) {
        for (EnhancedClarityRule rule : ENHANCED_RULES) {
            boolean keywordMatch = rule.keywords.stream().anyMatch(lowerPrompt::contains);
            if (keywordMatch && !isTopicAddressed(lowerPrompt, rule.topic)) {
                // Boost confidence based on user's past behavior
                double adjustedConfidence = rule.baseConfidence;
                List<ClarificationRecord> history = userClarificationHistory.get(userId);
                if (history != null) {
                    long similarPast = history.stream()
                        .filter(r -> r.issueType.equals(rule.id))
                        .count();
                    if (similarPast > 0) {
                        adjustedConfidence = Math.min(adjustedConfidence + 0.1, 1.0);
                    }
                }

                questions.add(new ClarifyingQuestion(
                    rule.id,
                    rule.question,
                    adjustedConfidence,
                    rule.suggestedFollowUps
                ));
            }
        }
    }

    /**
     * Adapt questions based on user's past behavior.
     */
    private void adaptQuestionsToUser(List<ClarifyingQuestion> questions, String userId, String prompt) {
        List<ClarificationRecord> history = userClarificationHistory.get(userId);
        if (history == null || history.isEmpty()) return;

        // Find user's expertise level
        long totalClarifications = history.size();
        long recentClarifications = history.stream()
            .filter(r -> System.currentTimeMillis() - r.timestamp < 7 * 24 * 60 * 60 * 1000)
            .count();

        // If user frequently needs clarification, add more guiding questions
        if (recentClarifications > 5) {
            questions.add(new ClarifyingQuestion(
                "user_guidance",
                "I notice you're working on multiple tasks. Would you like to see examples or templates?",
                0.65,
                List.of("Check out our documentation", "See example projects")
            ));
        }
    }

    /**
     * Add follow-up questions based on the prompt context.
     */
    private void addFollowUpQuestions(List<ClarifyingQuestion> questions, String prompt) {
        String lower = prompt.toLowerCase(Locale.ROOT);

        // If they mention "app", ask about platform specifics
        if (lower.contains("app") && !lower.contains("web app") && !lower.contains("mobile app")) {
            questions.add(new ClarifyingQuestion(
                "app_platform",
                "When you say 'app', do you mean web app, mobile app, or both?",
                0.70,
                List.of("Web application", "Mobile application", "Both")
            ));
        }

        // If they mention "database", ask which type
        if (lower.contains("database") || lower.contains("db")) {
            questions.add(new ClarifyingQuestion(
                "database_type",
                "Which database type do you prefer? (SQL, NoSQL, in-memory?)",
                0.72,
                List.of("PostgreSQL", "MySQL", "MongoDB", "Redis")
            ));
        }
    }

    /**
     * Check if a topic is already addressed in the prompt.
     */
    private boolean isTopicAddressed(String prompt, String topic) {
        String[] topicKeywords = topic.split("_");
        for (String keyword : topicKeywords) {
            if (prompt.contains(keyword)) return true;
        }
        return false;
    }

    /**
     * Get question effectiveness score from past interactions.
     */
    private double getQuestionEffectiveness(String issue) {
        return questionEffectiveness.getOrDefault(issue, 0.8);
    }

    /**
     * Record clarification effectiveness for learning.
     */
    public void recordClarificationResult(String userId, String issue, boolean wasHelpful) {
        userClarificationHistory.computeIfAbsent(userId, k -> new ArrayList<>())
            .add(new ClarificationRecord(issue, wasHelpful, System.currentTimeMillis()));

        // Update effectiveness score with exponential moving average
        double current = questionEffectiveness.getOrDefault(issue, 0.8);
        double alpha = 0.3; // Learning rate
        double newValue = wasHelpful ? current + alpha * (1.0 - current) : current - alpha * current;
        questionEffectiveness.put(issue, Math.max(0.1, Math.min(1.0, newValue)));

        log.info("Updated question effectiveness for {}: {}", issue, questionEffectiveness.get(issue));
    }

    /**
     * Calculate overall clarity score.
     */
    private double calculateClarityScore(List<ClarifyingQuestion> questions, double ambiguityScore) {
        if (questions.isEmpty()) return 1.0 - ambiguityScore;
        double avgConfidence = questions.stream()
            .mapToDouble(q -> q.confidence)
            .average()
            .orElse(0.5);
        return (1.0 - ambiguityScore) * (1.0 - avgConfidence);
    }

    /**
     * Get analysis summary for API response.
     */
    public Map<String, Object> getAnalysisSummary(String prompt, String userId) {
        AnalysisResult result = analyzePromptWithML(prompt, userId);
        Map<String, Object> summary = new HashMap<>(result.metadata);
        summary.put("promptLength", prompt != null ? prompt.length() : 0);
        summary.put("isClear", result.clarityScore > 0.7);
        summary.put("clarityScore", result.clarityScore);
        summary.put("questions", result.questions);
        summary.put("suggestedQuestions", result.questions.stream()
            .filter(q -> q.confidence >= 0.75)
            .limit(3)
            .collect(Collectors.toList()));
        summary.put("timestamp", System.currentTimeMillis());
        return summary;
    }

    // ── Data Classes ──────────────────────────────────────────────────────────

    public static class AnalysisResult {
        public final String prompt;
        public final double clarityScore;
        public final List<ClarifyingQuestion> questions;
        public final Map<String, Object> metadata;

        public AnalysisResult(String prompt, double clarityScore, List<ClarifyingQuestion> questions, Map<String, Object> metadata) {
            this.prompt = prompt;
            this.clarityScore = clarityScore;
            this.questions = questions;
            this.metadata = metadata;
        }
    }

    public static class ClarifyingQuestion {
        public String issue;
        public String question;
        public double confidence;
        public List<String> followUps;

        public ClarifyingQuestion(String issue, String question, double confidence, List<String> followUps) {
            this.issue = issue;
            this.question = question;
            this.confidence = confidence;
            this.followUps = followUps != null ? followUps : List.of();
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
            return issue.hashCode();
        }
    }

    private static class EnhancedClarityRule {
        String id;
        String topic;
        List<String> keywords;
        String question;
        double baseConfidence;
        List<String> suggestedFollowUps;

        EnhancedClarityRule(String id, String topic, List<String> keywords, String question, double baseConfidence) {
            this.id = id;
            this.topic = topic;
            this.keywords = keywords;
            this.question = question;
            this.baseConfidence = baseConfidence;
            this.suggestedFollowUps = List.of();
        }
    }

    private static class ClarificationRecord {
        String issueType;
        boolean wasHelpful;
        long timestamp;

        ClarificationRecord(String issueType, boolean wasHelpful, long timestamp) {
            this.issueType = issueType;
            this.wasHelpful = wasHelpful;
            this.timestamp = timestamp;
        }
    }
}
