package com.supremeai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Plan 19: Brilliant Idea Detection.
 *
 * Monitors conversation text and detects "brilliant ideas" based on
 * heuristic scoring (novelty keywords + context signals).
 * Detected ideas are queued for admin review.
 *
 * Scoring approach:
 * - Innovation keywords: +10 each
 * - Monetization signals: +8 each
 * - Problem-solving framing: +6 each
 * - Question marks / uncertainty: -2 (reduces speculation)
 * - Threshold >= 20 to be flagged as "brilliant"
 */
@Service
public class IdeaDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(IdeaDetectionService.class);

    @Value("${idea.detection.threshold:20}")
    private int detectionThreshold;

    // Keywords that signal a novel, high-value idea
    private static final List<String> INNOVATION_KEYWORDS = List.of(
        "novel", "unique", "breakthrough", "revolutionary", "innovative", "disruptive",
        "first-of-its-kind", "never done before", "game changer", "paradigm shift"
    );

    // Keywords that signal business / monetization value
    private static final List<String> MONETIZATION_KEYWORDS = List.of(
        "revenue", "monetize", "profit", "market", "users will pay", "subscription",
        "freemium", "enterprise tier", "white-label", "api marketplace"
    );

    // Keywords that frame the idea as a concrete solution
    private static final List<String> SOLUTION_KEYWORDS = List.of(
        "solve", "fix", "automate", "eliminate", "reduce", "replace", "streamline",
        "integrate", "unify", "platform for"
    );

    // Admin review queue; in production, back with Firestore "idea_queue" collection
    private final List<DetectedIdea> reviewQueue = new CopyOnWriteArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Analyze a conversation message/text for brilliant idea content.
     *
     * @param text   The text to analyze (chat message, note, etc.)
     * @param source Source identifier (userId, sessionId, etc.)
     * @return IdeaAnalysisResult with score and whether it was queued
     */
    public IdeaAnalysisResult analyze(String text, String source) {
        if (text == null || text.trim().isEmpty()) {
            return IdeaAnalysisResult.notIdea(0, "Empty input");
        }

        String lower = text.toLowerCase();
        int score = 0;
        List<String> matchedSignals = new ArrayList<>();

        // Score innovation keywords
        for (String kw : INNOVATION_KEYWORDS) {
            if (lower.contains(kw)) {
                score += 10;
                matchedSignals.add("innovation: " + kw);
            }
        }

        // Score monetization keywords
        for (String kw : MONETIZATION_KEYWORDS) {
            if (lower.contains(kw)) {
                score += 8;
                matchedSignals.add("monetization: " + kw);
            }
        }

        // Score solution-framing keywords
        for (String kw : SOLUTION_KEYWORDS) {
            if (lower.contains(kw)) {
                score += 6;
                matchedSignals.add("solution: " + kw);
            }
        }

        // Penalty for pure questions (speculation, not concrete idea)
        long questionCount = text.chars().filter(c -> c == '?').count();
        score -= (int) questionCount * 2;
        score = Math.max(0, score);

        boolean isBrilliant = score >= detectionThreshold;

        if (isBrilliant) {
            DetectedIdea idea = new DetectedIdea(
                UUID.randomUUID().toString(),
                text.length() > 200 ? text.substring(0, 200) + "..." : text,
                source,
                score,
                matchedSignals
            );
            reviewQueue.add(idea);
            logger.info("[IDEA] Brilliant idea detected from source={} score={} signals={}",
                source, score, matchedSignals.size());
            return IdeaAnalysisResult.brilliant(score, matchedSignals, idea.getIdeaId());
        }

        logger.debug("[IDEA] Not brilliant: score={} threshold={}", score, detectionThreshold);
        return IdeaAnalysisResult.notIdea(score, "Score below threshold (" + detectionThreshold + ")");
    }

    /**
     * Get all ideas pending admin review.
     */
    public List<DetectedIdea> getPendingReviewQueue() {
        return new ArrayList<>(reviewQueue.stream()
            .filter(i -> i.getStatus() == IdeaStatus.PENDING)
            .toList());
    }

    /**
     * Admin: mark an idea as approved (saved to knowledge base) or rejected.
     */
    public boolean reviewIdea(String ideaId, boolean approved) {
        for (DetectedIdea idea : reviewQueue) {
            if (idea.getIdeaId().equals(ideaId)) {
                idea.setStatus(approved ? IdeaStatus.APPROVED : IdeaStatus.REJECTED);
                idea.setReviewedAt(LocalDateTime.now());
                logger.info("[IDEA] Reviewed ideaId={} approved={}", ideaId, approved);
                return true;
            }
        }
        logger.warn("[IDEA] reviewIdea: ideaId not found: {}", ideaId);
        return false;
    }

    /**
     * Get review queue stats for admin dashboard.
     */
    public java.util.Map<String, Object> getQueueStats() {
        long pending = reviewQueue.stream().filter(i -> i.getStatus() == IdeaStatus.PENDING).count();
        long approved = reviewQueue.stream().filter(i -> i.getStatus() == IdeaStatus.APPROVED).count();
        long rejected = reviewQueue.stream().filter(i -> i.getStatus() == IdeaStatus.REJECTED).count();
        return java.util.Map.of("pending", pending, "approved", approved, "rejected", rejected, "total", reviewQueue.size());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inner Models
    // ─────────────────────────────────────────────────────────────────────────

    public enum IdeaStatus { PENDING, APPROVED, REJECTED }

    public static class DetectedIdea {
        private final String ideaId;
        private final String textSnippet;
        private final String source;
        private final int score;
        private final List<String> signals;
        private final LocalDateTime detectedAt;
        private IdeaStatus status;
        private LocalDateTime reviewedAt;

        public DetectedIdea(String ideaId, String textSnippet, String source,
                            int score, List<String> signals) {
            this.ideaId = ideaId;
            this.textSnippet = textSnippet;
            this.source = source;
            this.score = score;
            this.signals = signals;
            this.detectedAt = LocalDateTime.now();
            this.status = IdeaStatus.PENDING;
        }

        public String getIdeaId() { return ideaId; }
        public String getTextSnippet() { return textSnippet; }
        public String getSource() { return source; }
        public int getScore() { return score; }
        public List<String> getSignals() { return signals; }
        public LocalDateTime getDetectedAt() { return detectedAt; }
        public IdeaStatus getStatus() { return status; }
        public void setStatus(IdeaStatus status) { this.status = status; }
        public LocalDateTime getReviewedAt() { return reviewedAt; }
        public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    }

    public static class IdeaAnalysisResult {
        private final boolean brilliant;
        private final int score;
        private final List<String> signals;
        private final String message;
        private final String queuedIdeaId;

        private IdeaAnalysisResult(boolean brilliant, int score, List<String> signals,
                                   String message, String queuedIdeaId) {
            this.brilliant = brilliant;
            this.score = score;
            this.signals = signals;
            this.message = message;
            this.queuedIdeaId = queuedIdeaId;
        }

        public static IdeaAnalysisResult brilliant(int score, List<String> signals, String ideaId) {
            return new IdeaAnalysisResult(true, score, signals, "Brilliant idea detected and queued for review", ideaId);
        }

        public static IdeaAnalysisResult notIdea(int score, String reason) {
            return new IdeaAnalysisResult(false, score, List.of(), reason, null);
        }

        public boolean isBrilliant() { return brilliant; }
        public int getScore() { return score; }
        public List<String> getSignals() { return signals; }
        public String getMessage() { return message; }
        public String getQueuedIdeaId() { return queuedIdeaId; }
    }
}
