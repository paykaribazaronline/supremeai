package com.supremeai.controller;

import com.supremeai.service.ReputationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reputation Controller - Phase 4 (Plan 18)
 * Exposes contributor reputation and trust scoring endpoints.
 */
@RestController
@RequestMapping("/api/admin/reputation")
public class ReputationController {
    public ReputationController(ReputationService reputationService) {
        this.reputationService = reputationService;
    }


    private static final Logger logger = LoggerFactory.getLogger(ReputationController.class);


    /**
     * GET /api/admin/reputation/contributor/{contributorId}
     * Get contributor profile with reputation details.
     */
    @GetMapping("/contributor/{contributorId}")
    public ResponseEntity<Map<String, Object>> getContributor(@PathVariable String contributorId) {
        ReputationService.ContributorProfile profile =
            reputationService.getOrCreateProfile(contributorId);

        Map<String, Object> response = new HashMap<>();
        response.put("contributorId", profile.contributorId);
        response.put("totalScore", profile.totalScore);
        response.put("tier", profile.tier.name());
        response.put("submissions", profile.submissions);
        response.put("acceptedSubmissions", profile.acceptedSubmissions);
        response.put("rejectedSubmissions", profile.rejectedSubmissions);
        response.put("acceptanceRate", profile.acceptanceRate);
        response.put("lastActivity", profile.lastActivity);
        response.put("badges", profile.badges);
        response.put("categoryScores", profile.categoryScores);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/admin/reputation/top-contributors
     * Get top contributors by category.
     */
    @GetMapping("/top-contributors")
    public ResponseEntity<List<Map<String, Object>>> getTopContributors(
            @RequestParam(defaultValue = "general") String category,
            @RequestParam(defaultValue = "10") int limit) {

        List<ReputationService.ContributorProfile> top =
            reputationService.getTopContributors(category, limit);

        List<Map<String, Object>> result = top.stream()
            .map(p -> {
                Map<String, Object> m = new HashMap<>();
                m.put("contributorId", p.contributorId);
                m.put("totalScore", p.totalScore);
                m.put("tier", p.tier.name());
                m.put("submissions", p.submissions);
                m.put("acceptanceRate", p.acceptanceRate);
                return m;
            })
            .toList();

        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/admin/reputation/record-submission
     * Record a submission result (accept/reject).
     */
    @PostMapping("/record-submission")
    public ResponseEntity<Map<String, Object>> recordSubmission(
            @RequestBody Map<String, Object> request) {

        String contributorId = (String) request.get("contributorId");
        boolean accepted = Boolean.TRUE.equals(request.get("accepted"));
        String category = (String) request.getOrDefault("category", "general");

        if (contributorId == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "contributorId is required"));
        }

        ReputationService.ContributorProfile profile =
            reputationService.recordSubmission(contributorId, accepted, category);

        Map<String, Object> response = Map.of(
            "contributorId", profile.contributorId,
            "newScore", profile.totalScore,
            "tier", profile.tier.name(),
            "submissions", profile.submissions,
            "acceptanceRate", profile.acceptanceRate
        );

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/admin/reputation/needs-review
     * Get contributors requiring moderation.
     */
    @GetMapping("/needs-review")
    public ResponseEntity<List<Map<String, Object>>> getContributorsNeedingReview(
            @RequestParam(defaultValue = "5") int minSubmissions,
            @RequestParam(defaultValue = "0.6") double maxAcceptanceRate) {

        List<ReputationService.ContributorProfile> flagged =
            reputationService.getContributorsNeedingReview(minSubmissions, maxAcceptanceRate);

        List<Map<String, Object>> result = flagged.stream()
            .map(p -> {
                Map<String, Object> m = new HashMap<>();
                m.put("contributorId", p.contributorId);
                m.put("totalScore", p.totalScore);
                m.put("tier", p.tier.name());
                m.put("submissions", p.submissions);
                m.put("acceptanceRate", p.acceptanceRate);
                m.put("lastActivity", p.lastActivity);
                return m;
            })
            .toList();

        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/admin/reputation/calculate-quality
     * Calculate quality score for an API submission.
     */
    @PostMapping("/calculate-quality")
    public ResponseEntity<Map<String, Object>> calculateQuality(
            @RequestBody Map<String, Object> submission) {

        String apiId = (String) submission.get("apiId");
        if (apiId == null) {
            apiId = "api_" + System.currentTimeMillis();
        }

        ReputationService.QualityScore score =
            reputationService.calculateQualityScore(apiId, submission);

        Map<String, Object> response = Map.of(
            "apiId", apiId,
            "overallScore", score.overallScore,
            "correctnessScore", score.correctnessScore,
            "completenessScore", score.completenessScore,
            "securityScore", score.securityScore,
            "documentationScore", score.documentationScore,
            "testCoverageScore", score.testCoverageScore,
            "issues", score.issues,
            "recommendations", score.recommendations
        );

        return ResponseEntity.ok(response);
    }
}
