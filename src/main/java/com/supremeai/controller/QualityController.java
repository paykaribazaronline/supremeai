package com.supremeai.controller;

import com.supremeai.service.QualityScoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Quality Control Controller - Phase 4
 * Exposes quality scoring and validation endpoints for admin dashboard.
 */
@RestController
@RequestMapping("/api/admin/quality")
public class QualityController {
    public QualityController(QualityScoringService qualityService) {
        this.qualityService = qualityService;
    }


    private static final Logger logger = LoggerFactory.getLogger(QualityController.class);


    /**
     * POST /api/admin/quality/assess
     * Run quality assessment on an API submission.
     */
    @PostMapping("/assess")
    public ResponseEntity<Map<String, Object>> assessQuality(
            @RequestBody Map<String, Object> submission) {

        String apiId = (String) submission.get("apiId");
        if (apiId == null || apiId.isEmpty()) {
            apiId = "api_" + System.currentTimeMillis();
        }

        QualityScoringService.QualityAssessment assessment =
            qualityService.assessQuality(apiId, submission);

        Map<String, Object> response = Map.of(
            "apiId", apiId,
            "overallScore", assessment.overallScore,
            "qualityLevel", assessment.qualityLevel,
            "autoApproved", assessment.autoApproved,
            "needsManualReview", assessment.needsManualReview,
            "checksPassed", assessment.checksPassed,
            "checksFailed", assessment.checksFailed,
            "recommendations", assessment.recommendations,
            "assessedAt", assessment.assessedAt
        );

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/admin/quality/run-tests
     * Execute automated tests against a connector.
     */
    @PostMapping("/run-tests")
    public ResponseEntity<Map<String, Object>> runTests(
            @RequestBody Map<String, Object> connectorConfig) {

        String apiId = (String) connectorConfig.get("apiId");
        if (apiId == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "apiId is required"));
        }

        QualityScoringService.AutoTestResult result =
            qualityService.runAutoTests(apiId, connectorConfig);

        Map<String, Object> response = Map.of(
            "testId", result.testId,
            "apiId", result.apiId,
            "passed", result.passed,
            "testsRun", result.testsRun,
            "testsPassed", result.testsPassed,
            "failures", result.failures,
            "durationMs", result.durationMs,
            "executedAt", result.executedAt
        );

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/admin/quality/assessments/pending
     * List all assessments requiring manual review.
     */
    @GetMapping("/assessments/pending")
    public ResponseEntity<List<Map<String, Object>>> getPendingAssessments() {
        List<QualityScoringService.QualityAssessment> pending =
            qualityService.getAssessmentsNeedingReview();

        // Convert to simplified DTOs
        List<Map<String, Object>> result = pending.stream()
            .map(a -> Map.of(
                "apiId", a.apiId,
                "overallScore", a.overallScore,
                "qualityLevel", a.qualityLevel,
                "checksFailed", a.checksFailed,
                "needsManualReview", a.needsManualReview
            ))
            .toList();

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/admin/quality/assessment/{apiId}
     * Get quality assessment for a specific API.
     */
    @GetMapping("/assessment/{apiId}")
    public ResponseEntity<Map<String, Object>> getAssessment(@PathVariable String apiId) {
        QualityScoringService.QualityAssessment assessment =
            qualityService.getAssessment(apiId);

        if (assessment == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of(
            "apiId", apiId,
            "overallScore", assessment.overallScore,
            "qualityLevel", assessment.qualityLevel,
            "checksPassed", assessment.checksPassed,
            "checksFailed", assessment.checksFailed,
            "warnings", assessment.warnings,
            "recommendations", assessment.recommendations
        ));
    }
}
