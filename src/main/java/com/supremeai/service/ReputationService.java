package com.supremeai.service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Reputation Service - Plan 18 Phase 4 Manages contributor reputation and trust scoring for
 * crowdsourced API patterns. Based on submission history, validation results, and community
 * feedback.
 */
@Service
public class ReputationService {

  private static final Logger logger = LoggerFactory.getLogger(ReputationService.class);

  // Reputation tiers
  public enum ReputationTier {
    NOVICE(0, 500),
    CONTRIBUTOR(500, 2000),
    TRUSTED(2000, 5000),
    EXPERT(5000, 15000),
    MASTER(15000, Integer.MAX_VALUE);

    final int minScore;
    final int maxScore;

    ReputationTier(int min, int max) {
      this.minScore = min;
      this.maxScore = max;
    }

    public static ReputationTier fromScore(int score) {
      for (ReputationTier tier : values()) {
        if (score >= tier.minScore && score < tier.maxScore) return tier;
      }
      return NOVICE;
    }
  }

  // Contributor profile
  public static class ContributorProfile {
    public String contributorId;
    public int totalScore;
    public ReputationTier tier;
    public int submissions;
    public int acceptedSubmissions;
    public int rejectedSubmissions;
    public double acceptanceRate;
    public long lastActivity;
    public List<String> badges;
    public Map<String, Integer> categoryScores;

    public ContributorProfile(String id) {
      this.contributorId = id;
      this.totalScore = 0;
      this.tier = ReputationTier.NOVICE;
      this.submissions = 0;
      this.acceptedSubmissions = 0;
      this.rejectedSubmissions = 0;
      this.acceptanceRate = 0.0;
      this.lastActivity = System.currentTimeMillis();
      this.badges = new ArrayList<>();
      this.categoryScores = new ConcurrentHashMap<>();
    }
  }

  // Quality score breakdown
  public static class QualityScore {
    public int overallScore;
    public int correctnessScore;
    public int completenessScore;
    public int securityScore;
    public int documentationScore;
    public int testCoverageScore;
    public List<String> issues;
    public List<String> recommendations;

    public QualityScore() {
      this.issues = new ArrayList<>();
      this.recommendations = new ArrayList<>();
    }
  }

  // In-memory storage (replace with DB in production)
  private final Map<String, ContributorProfile> contributorProfiles = new ConcurrentHashMap<>();
  private final Map<String, List<QualityScore>> apiQualityScores = new ConcurrentHashMap<>();

  // Scoring weights
  private static final double CORRECTNESS_WEIGHT = 0.35;
  private static final double COMPLETENESS_WEIGHT = 0.25;
  private static final double SECURITY_WEIGHT = 0.20;
  private static final double DOCUMENTATION_WEIGHT = 0.10;
  private static final double TEST_COVERAGE_WEIGHT = 0.10;

  // Score actions
  private static final int SCORE_SUBMISSION_ACCEPTED = 50;
  private static final int SCORE_SUBMISSION_REJECTED = -20;
  private static final int SCORE_VOTE_AGREEMENT = 10;
  private static final int SCORE_VOTE_DISAGREEMENT = -5;
  private static final int SCORE_BONUS_FIRST = 100;
  private static final int SCORE_BONUS_CONSECUTIVE = 25;

  /** Get or create contributor profile */
  public ContributorProfile getOrCreateProfile(String contributorId) {
    return contributorProfiles.computeIfAbsent(contributorId, id -> new ContributorProfile(id));
  }

  /** Record submission result and update reputation */
  public ContributorProfile recordSubmission(
      String contributorId, boolean accepted, String category) {
    ContributorProfile profile = getOrCreateProfile(contributorId);

    profile.submissions++;
    profile.lastActivity = System.currentTimeMillis();

    if (accepted) {
      profile.acceptedSubmissions++;
      profile.totalScore += SCORE_SUBMISSION_ACCEPTED;
      profile.categoryScores.merge(category, SCORE_SUBMISSION_ACCEPTED, Integer::sum);

      // Bonus for first accepted submission
      if (profile.acceptedSubmissions == 1) {
        profile.totalScore += SCORE_BONUS_FIRST;
      }
    } else {
      profile.rejectedSubmissions++;
      profile.totalScore = Math.max(0, profile.totalScore + SCORE_SUBMISSION_REJECTED);
    }

    // Update acceptance rate
    if (profile.submissions > 0) {
      profile.acceptanceRate = (double) profile.acceptedSubmissions / profile.submissions;
    }

    // Update tier
    profile.tier = ReputationTier.fromScore(profile.totalScore);

    logger.info(
        "Updated reputation for {}: score={}, tier={}, submissions={}",
        contributorId,
        profile.totalScore,
        profile.tier,
        profile.submissions);

    return profile;
  }

  /** Record community vote agreement */
  public void recordVoteAgreement(String contributorId, String category) {
    ContributorProfile profile = getOrCreateProfile(contributorId);
    profile.totalScore += SCORE_VOTE_AGREEMENT;
    profile.categoryScores.merge(category, SCORE_VOTE_AGREEMENT, Integer::sum);
    profile.tier = ReputationTier.fromScore(profile.totalScore);
  }

  /** Record community vote disagreement */
  public void recordVoteDisagreement(String contributorId, String category) {
    ContributorProfile profile = getOrCreateProfile(contributorId);
    profile.totalScore = Math.max(0, profile.totalScore + SCORE_VOTE_DISAGREEMENT);
    profile.tier = ReputationTier.fromScore(profile.totalScore);
  }

  /** Calculate quality score for an API submission */
  public QualityScore calculateQualityScore(String apiId, Map<String, Object> submissionData) {
    QualityScore score = new QualityScore();

    // Correctness (35%): Does it work correctly?
    score.correctnessScore = evaluateCorrectness(submissionData);

    // Completeness (25%): Does it cover all expected functionality?
    score.completenessScore = evaluateCompleteness(submissionData);

    // Security (20%): Is it secure?
    score.securityScore = evaluateSecurity(submissionData);

    // Documentation (10%): Is it well documented?
    score.documentationScore = evaluateDocumentation(submissionData);

    // Test Coverage (10%): Are there tests?
    score.testCoverageScore = evaluateTestCoverage(submissionData);

    // Weighted overall score
    score.overallScore =
        (int)
            (score.correctnessScore * CORRECTNESS_WEIGHT
                + score.completenessScore * COMPLETENESS_WEIGHT
                + score.securityScore * SECURITY_WEIGHT
                + score.documentationScore * DOCUMENTATION_WEIGHT
                + score.testCoverageScore * TEST_COVERAGE_WEIGHT);

    // Collect issues and recommendations
    if (score.correctnessScore < 60) {
      score.issues.add("Correctness below threshold");
      score.recommendations.add("Verify API response parsing and error handling");
    }
    if (score.securityScore < 70) {
      score.issues.add("Security concerns detected");
      score.recommendations.add("Review authentication and data handling");
    }
    if (score.testCoverageScore < 50) {
      score.issues.add("Low test coverage");
      score.recommendations.add("Add unit and integration tests");
    }

    // Store score
    apiQualityScores.computeIfAbsent(apiId, k -> new ArrayList<>()).add(score);

    return score;
  }

  /** Get average quality score for an API */
  public QualityScore getAverageQualityScore(String apiId) {
    List<QualityScore> scores = apiQualityScores.getOrDefault(apiId, List.of());
    if (scores.isEmpty()) return null;

    QualityScore avg = new QualityScore();
    int count = scores.size();

    avg.correctnessScore = scores.stream().mapToInt(s -> s.correctnessScore).sum() / count;
    avg.completenessScore = scores.stream().mapToInt(s -> s.completenessScore).sum() / count;
    avg.securityScore = scores.stream().mapToInt(s -> s.securityScore).sum() / count;
    avg.documentationScore = scores.stream().mapToInt(s -> s.documentationScore).sum() / count;
    avg.testCoverageScore = scores.stream().mapToInt(s -> s.testCoverageScore).sum() / count;
    avg.overallScore = scores.stream().mapToInt(s -> s.overallScore).sum() / count;

    return avg;
  }

  /** Get top contributors for a category */
  public List<ContributorProfile> getTopContributors(String category, int limit) {
    return contributorProfiles.values().stream()
        .filter(p -> p.categoryScores.containsKey(category))
        .sorted(
            (a, b) -> {
              int scoreCompare =
                  Integer.compare(
                      b.categoryScores.getOrDefault(category, 0),
                      a.categoryScores.getOrDefault(category, 0));
              if (scoreCompare != 0) return scoreCompare;
              return Integer.compare(b.totalScore, a.totalScore);
            })
        .limit(limit)
        .collect(Collectors.toList());
  }

  /** Get contributors requiring moderation (low quality or suspicious patterns) */
  public List<ContributorProfile> getContributorsNeedingReview(
      int minSubmissions, double maxAcceptanceRate) {
    return contributorProfiles.values().stream()
        .filter(p -> p.submissions >= minSubmissions)
        .filter(p -> p.acceptanceRate < maxAcceptanceRate)
        .sorted((a, b) -> Double.compare(a.acceptanceRate, b.acceptanceRate))
        .collect(Collectors.toList());
  }

  /** Evaluate correctness of a submission */
  private int evaluateCorrectness(Map<String, Object> data) {
    int score = 70; // Base score

    // Penalize missing required fields
    if (!data.containsKey("endpoints")
        || ((List<?>) data.getOrDefault("endpoints", List.of())).isEmpty()) {
      score -= 20;
    }
    if (!data.containsKey("authType")) {
      score -= 15;
    }
    if (!data.containsKey("requestFormat")) {
      score -= 10;
    }

    // Bonus for validation evidence
    if (Boolean.TRUE.equals(data.get("validated"))) {
      score += 15;
    }

    return Math.min(100, Math.max(0, score));
  }

  /** Evaluate completeness of a submission */
  private int evaluateCompleteness(Map<String, Object> data) {
    int score = 60;

    if (data.containsKey("description")
        && ((String) data.getOrDefault("description", "")).length() > 10) {
      score += 10;
    }
    if (data.containsKey("endpoints")
        && ((List<?>) data.getOrDefault("endpoints", List.of())).size() > 2) {
      score += 10;
    }
    if (Boolean.TRUE.equals(data.get("hasExamples"))) {
      score += 10;
    }
    if (data.containsKey("errorHandling")) {
      score += 10;
    }

    return Math.min(100, Math.max(0, score));
  }

  /** Evaluate security of a submission */
  private int evaluateSecurity(Map<String, Object> data) {
    int score = 70;

    if (Boolean.TRUE.equals(data.get("usesEncryption"))) {
      score += 10;
    }
    if (Boolean.TRUE.equals(data.get("validatesInput"))) {
      score += 10;
    }
    if (Boolean.TRUE.equals(data.get("rateLimited"))) {
      score += 10;
    }
    if (Boolean.TRUE.equals(data.get("authenticated"))) {
      score += 10;
    }

    return Math.min(100, Math.max(0, score));
  }

  /** Evaluate documentation quality */
  private int evaluateDocumentation(Map<String, Object> data) {
    int score = 50;

    if (data.containsKey("readme") && ((String) data.getOrDefault("readme", "")).length() > 100) {
      score += 25;
    }
    if (data.containsKey("apiDocs") && Boolean.TRUE.equals(data.get("apiDocs"))) {
      score += 15;
    }
    if (data.containsKey("changelog") && Boolean.TRUE.equals(data.get("changelog"))) {
      score += 10;
    }

    return Math.min(100, Math.max(0, score));
  }

  /** Evaluate test coverage */
  private int evaluateTestCoverage(Map<String, Object> data) {
    int score = 40;

    if (data.containsKey("testCount")) {
      int tests = (int) data.get("testCount");
      if (tests > 20) score += 30;
      else if (tests > 5) score += 15;
      else if (tests > 0) score += 5;
    }
    if (Boolean.TRUE.equals(data.get("hasIntegrationTests"))) {
      score += 15;
    }
    if (Boolean.TRUE.equals(data.get("hasE2ETests"))) {
      score += 10;
    }

    return Math.min(100, Math.max(0, score));
  }
}
