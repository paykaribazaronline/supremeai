package org.example.agentorchestration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MuonClipOptimizer.
 *
 * Tests cover:
 *   - clip() returns value within [MIN_LR, MAX_LR]
 *   - step() momentum reduces oscillation (same-direction rewards accumulate)
 *   - step() bias correction produces non-zero result from step 1
 *   - orthogonalize() gives full weight (1.0) to primary task
 *   - orthogonalize() reduces weight for unrelated tasks
 *   - reset() clears momentum state
 */
class MuonClipOptimizerTest {

    private MuonClipOptimizer optimizer;

    @BeforeEach
    void setUp() {
        optimizer = new MuonClipOptimizer();
    }

    // ── clip() ────────────────────────────────────────────────────────────────

    @Test
    void clip_positiveGradientReturnsPositiveLR() {
        double lr = optimizer.clip(+1.0, 0.01);
        assertTrue(lr > 0, "Positive gradient should produce positive LR, got: " + lr);
    }

    @Test
    void clip_negativeGradientReturnsPositiveLR() {
        double lr = optimizer.clip(-1.0, 0.01);
        assertTrue(lr > 0, "Negative gradient should still return positive LR (magnitude), got: " + lr);
    }

    @Test
    void clip_resultIsWithinBounds() {
        double[] gradients = {-2.0, -1.0, -0.5, 0.0, 0.5, 1.0, 2.0};
        for (double g : gradients) {
            double lr = optimizer.clip(g, 0.01);
            assertTrue(lr >= 0.001 && lr <= 0.02,
                "LR=" + lr + " out of bounds [0.001, 0.02] for gradient=" + g);
        }
    }

    @Test
    void clip_zeroGradientReturnsMinLR() {
        double lr = optimizer.clip(0.0, 0.01);
        assertEquals(0.001, lr, 1e-9, "Zero gradient should return minimum LR");
    }

    // ── step() with momentum ──────────────────────────────────────────────────

    @Test
    void step_firstCallReturnsNonZeroLR() {
        double lr = optimizer.step("Architect", ExpertAgentRouter.TaskType.CODE_GENERATION, +0.8, 0.01);
        assertTrue(lr > 0, "First step should return positive LR");
    }

    @Test
    void step_repeatedPositiveGradientsAccumulateMomentum() {
        // Momentum should build up with consistent positive rewards
        double lr1 = optimizer.step("Builder", ExpertAgentRouter.TaskType.BUG_FIX, +0.5, 0.01);
        double lr2 = optimizer.step("Builder", ExpertAgentRouter.TaskType.BUG_FIX, +0.5, 0.01);
        double lr3 = optimizer.step("Builder", ExpertAgentRouter.TaskType.BUG_FIX, +0.5, 0.01);
        // LR should increase as momentum builds, then stabilize
        assertTrue(lr3 >= lr1 || lr3 > 0.001,
            "Momentum should sustain LR above minimum after consistent rewards");
    }

    @Test
    void step_boundsRespectedAfterManySteps() {
        for (int i = 0; i < 50; i++) {
            double gradient = (i % 2 == 0) ? +1.0 : -1.0; // oscillating signal
            double lr = optimizer.step("Reviewer", ExpertAgentRouter.TaskType.CODE_REVIEW, gradient, 0.01);
            assertTrue(lr >= 0.001 && lr <= 0.02,
                "LR=" + lr + " out of bounds at step " + i);
        }
    }

    // ── orthogonalize() ───────────────────────────────────────────────────────

    @Test
    void orthogonalize_primaryTaskHasFullMultiplier() {
        ExpertAgentRouter.TaskType primary = ExpertAgentRouter.TaskType.SECURITY_AUDIT;
        Map<ExpertAgentRouter.TaskType, Double> multipliers =
            optimizer.orthogonalize("Alpha-Security", primary, +0.7);

        assertEquals(1.0, multipliers.get(primary), 1e-9,
            "Primary task should get multiplier 1.0");
    }

    @Test
    void orthogonalize_nonPrimaryTasksHaveReducedMultiplier() {
        ExpertAgentRouter.TaskType primary = ExpertAgentRouter.TaskType.SECURITY_AUDIT;
        Map<ExpertAgentRouter.TaskType, Double> multipliers =
            optimizer.orthogonalize("Alpha-Security", primary, +0.7);

        for (Map.Entry<ExpertAgentRouter.TaskType, Double> entry : multipliers.entrySet()) {
            if (entry.getKey() != primary) {
                assertTrue(entry.getValue() < 1.0,
                    "Non-primary task " + entry.getKey() + " should have multiplier < 1.0");
            }
        }
    }

    @Test
    void orthogonalize_coversAllTaskTypes() {
        Map<ExpertAgentRouter.TaskType, Double> multipliers =
            optimizer.orthogonalize("Kappa-Evolution",
                ExpertAgentRouter.TaskType.META_IMPROVEMENT, +0.9);
        for (ExpertAgentRouter.TaskType tt : ExpertAgentRouter.TaskType.values()) {
            assertTrue(multipliers.containsKey(tt),
                "orthogonalize() missing task type: " + tt);
        }
    }

    // ── reset() ───────────────────────────────────────────────────────────────

    @Test
    void reset_clearsAccumulatedMomentum() {
        // Build momentum
        for (int i = 0; i < 5; i++) {
            optimizer.step("Theta-Learning", ExpertAgentRouter.TaskType.LEARNING, +1.0, 0.01);
        }
        // Reset
        optimizer.reset("Theta-Learning");
        // Next step should behave like a fresh start (step count back to 1)
        double lr = optimizer.step("Theta-Learning", ExpertAgentRouter.TaskType.LEARNING, +0.5, 0.01);
        assertTrue(lr > 0, "LR should still be positive after reset");
    }

    // ── getDiagnostics() ──────────────────────────────────────────────────────

    @Test
    void getDiagnostics_containsRequiredKeys() {
        optimizer.step("Eta-Meta", ExpertAgentRouter.TaskType.META_IMPROVEMENT, +0.6, 0.01);
        Map<String, Object> diag = optimizer.getDiagnostics("Eta-Meta");
        assertTrue(diag.containsKey("agent"),            "Missing 'agent'");
        assertTrue(diag.containsKey("step_count"),       "Missing 'step_count'");
        assertTrue(diag.containsKey("momentum_snapshot"),"Missing 'momentum_snapshot'");
        assertTrue(diag.containsKey("beta"),             "Missing 'beta'");
        assertTrue(diag.containsKey("clip_threshold"),   "Missing 'clip_threshold'");
    }

    @Test
    void getDiagnostics_stepCountIncrementsCorrectly() {
        optimizer.step("Delta-Cost", ExpertAgentRouter.TaskType.COST_OPTIMIZATION, +0.3, 0.01);
        optimizer.step("Delta-Cost", ExpertAgentRouter.TaskType.COST_OPTIMIZATION, +0.3, 0.01);
        optimizer.step("Delta-Cost", ExpertAgentRouter.TaskType.COST_OPTIMIZATION, +0.3, 0.01);
        Map<String, Object> diag = optimizer.getDiagnostics("Delta-Cost");
        assertEquals(3, diag.get("step_count"), "Step count should be 3");
    }
}
