package com.supremeai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Plan 5: Plan Compatibility & Impact Analysis.
 *
 * Compares a new proposed plan against the existing system plan and:
 * - Calculates a compatibility score (0-100%)
 * - Identifies conflicting areas
 * - Provides an impact simulation summary
 * - Warns users of risk level without blocking execution (user autonomy preserved)
 */
@Service
public class PlanCompatibilityService {

    private static final Logger logger = LoggerFactory.getLogger(PlanCompatibilityService.class);

    // Keywords that indicate destructive or breaking changes
    private static final List<String> BREAKING_KEYWORDS = List.of(
        "delete", "drop", "remove", "deprecate", "replace", "migrate", "reset", "override"
    );

    // Keywords that indicate additive/compatible changes
    private static final List<String> COMPATIBLE_KEYWORDS = List.of(
        "add", "extend", "enhance", "improve", "support", "integrate", "optimize", "expand"
    );

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Analyze compatibility between existing plan and proposed new plan.
     *
     * @param existingPlan Text description / key features of the current plan
     * @param proposedPlan Text description / key features of the proposed change
     * @return CompatibilityReport with score, conflicts, and impact summary
     */
    public CompatibilityReport analyze(String existingPlan, String proposedPlan) {
        logger.info("[PLAN_COMPAT] Analyzing compatibility...");

        String existing = existingPlan.toLowerCase();
        String proposed = proposedPlan.toLowerCase();

        // Score base: 100, deduct for conflicts
        int score = 100;
        List<String> conflicts = new ArrayList<>();
        List<String> compatibleAreas = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Check for breaking keywords in proposed plan
        for (String keyword : BREAKING_KEYWORDS) {
            if (proposed.contains(keyword)) {
                score -= 10;
                conflicts.add("Proposed plan contains breaking keyword: '" + keyword + "'");
            }
        }

        // Bonus for additive keywords
        for (String keyword : COMPATIBLE_KEYWORDS) {
            if (proposed.contains(keyword)) {
                compatibleAreas.add("Additive change detected: '" + keyword + "'");
            }
        }

        // Check if proposed overwrites core existing features
        String[] existingTokens = existing.split("[\\s,;.]+");
        for (String token : existingTokens) {
            if (token.length() > 4 && proposed.contains("replace " + token)) {
                score -= 15;
                conflicts.add("Proposed plan replaces existing feature: '" + token + "'");
            }
        }

        // Clamp score 0-100
        score = Math.max(0, Math.min(100, score));

        // Determine risk level
        RiskLevel risk;
        if (score >= 80) {
            risk = RiskLevel.LOW;
        } else if (score >= 50) {
            risk = RiskLevel.MEDIUM;
            warnings.add("Moderate compatibility issues detected. Review conflicts before proceeding.");
        } else {
            risk = RiskLevel.HIGH;
            warnings.add("High risk: significant conflicts with existing plan. Recommend manual review.");
        }

        // Impact simulation
        String impactSummary = buildImpactSummary(score, conflicts, compatibleAreas);

        logger.info("[PLAN_COMPAT] Score={} Risk={} Conflicts={}", score, risk, conflicts.size());

        return new CompatibilityReport(score, risk, conflicts, compatibleAreas, warnings, impactSummary);
    }

    /**
     * Quick compatibility check — returns true if score >= threshold (default 70).
     */
    public boolean isCompatible(String existingPlan, String proposedPlan) {
        return isCompatible(existingPlan, proposedPlan, 70);
    }

    public boolean isCompatible(String existingPlan, String proposedPlan, int threshold) {
        CompatibilityReport report = analyze(existingPlan, proposedPlan);
        return report.getScore() >= threshold;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String buildImpactSummary(int score, List<String> conflicts, List<String> compatible) {
        StringBuilder sb = new StringBuilder();
        sb.append("Compatibility Score: ").append(score).append("/100. ");

        if (conflicts.isEmpty() && compatible.isEmpty()) {
            sb.append("Plans appear unrelated — minimal impact expected.");
        } else {
            if (!compatible.isEmpty()) {
                sb.append("Compatible additions: ").append(compatible.size()).append(". ");
            }
            if (!conflicts.isEmpty()) {
                sb.append("Potential conflicts: ").append(conflicts.size()).append(". ");
                sb.append("Like 'renovating a room while the house is occupied' — proceed carefully.");
            } else {
                sb.append("No conflicts detected. Like 'adding a new room to a house' — safe to proceed.");
            }
        }
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inner Models
    // ─────────────────────────────────────────────────────────────────────────

    public enum RiskLevel { LOW, MEDIUM, HIGH }

    public static class CompatibilityReport {
        private final int score;
        private final RiskLevel riskLevel;
        private final List<String> conflicts;
        private final List<String> compatibleAreas;
        private final List<String> warnings;
        private final String impactSummary;

        public CompatibilityReport(int score, RiskLevel riskLevel, List<String> conflicts,
                                   List<String> compatibleAreas, List<String> warnings,
                                   String impactSummary) {
            this.score = score;
            this.riskLevel = riskLevel;
            this.conflicts = conflicts;
            this.compatibleAreas = compatibleAreas;
            this.warnings = warnings;
            this.impactSummary = impactSummary;
        }

        public int getScore() { return score; }
        public RiskLevel getRiskLevel() { return riskLevel; }
        public List<String> getConflicts() { return conflicts; }
        public List<String> getCompatibleAreas() { return compatibleAreas; }
        public List<String> getWarnings() { return warnings; }
        public String getImpactSummary() { return impactSummary; }

        public Map<String, Object> toMap() {
            return Map.of(
                "score", score,
                "riskLevel", riskLevel.name(),
                "conflicts", conflicts,
                "compatibleAreas", compatibleAreas,
                "warnings", warnings,
                "impactSummary", impactSummary
            );
        }
    }
}
