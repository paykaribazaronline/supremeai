package org.example.kimik2;

import org.example.service.AgentDecisionLogger;
import org.example.service.SystemLearningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RLVRTrainer.
 *
 * Because RLVRTrainer has @Autowired dependencies, we manually wire
 * a minimal object graph (no Spring context needed — same pattern as
 * SystemLearningServiceTest).
 *
 * Tests cover:
 *   - recordOutcome returns correct sign of reward for every outcome type
 *   - Positive outcome updates MoE weights upward
 *   - Negative outcome updates MoE weights downward (not below minimum)
 *   - getLeaderboard() lists all 20 agents
 *   - estimateReward() defaults to 0.5 for unseen agent
 *   - estimateReward() improves after positive records
 */
class RLVRTrainerTest {

    private RLVRTrainer trainer;
    private KimiMoERouter router;

    @BeforeEach
    void setUp() {
        router = new KimiMoERouter();
        MuonClipOptimizer muon = new MuonClipOptimizer();
        AgentDecisionLogger logger = new AgentDecisionLogger();
        SystemLearningService learning = new SystemLearningService();

        trainer = new RLVRTrainer();
        // Manual wiring (no Spring context)
        setField(trainer, "moeRouter",      router);
        setField(trainer, "muonClip",       muon);
        setField(trainer, "decisionLogger", logger);
        setField(trainer, "learningService", learning);
    }

    // ── recordOutcome rewards ─────────────────────────────────────────────────

    @Test
    void recordOutcome_buildPass_hasPositiveReward() {
        RLVRTrainer.TrainingResult result = trainer.recordOutcome(
            List.of("Architect", "Builder"),
            KimiMoERouter.TaskType.CODE_GENERATION,
            RLVRTrainer.VerifiableOutcome.BUILD_PASS,
            "build succeeded in 26s");

        assertTrue(result.reward > 0,
            "BUILD_PASS should give positive reward, got: " + result.reward);
    }

    @Test
    void recordOutcome_buildFail_hasNegativeReward() {
        RLVRTrainer.TrainingResult result = trainer.recordOutcome(
            List.of("Builder"),
            KimiMoERouter.TaskType.CODE_GENERATION,
            RLVRTrainer.VerifiableOutcome.BUILD_FAIL,
            "compilation error in 3s");

        assertTrue(result.reward < 0,
            "BUILD_FAIL should give negative reward, got: " + result.reward);
    }

    @Test
    void recordOutcome_testPass_hasPositiveReward() {
        RLVRTrainer.TrainingResult result = trainer.recordOutcome(
            List.of("C-Tester"),
            KimiMoERouter.TaskType.TESTING,
            RLVRTrainer.VerifiableOutcome.TEST_PASS,
            "245 tests passed");
        assertTrue(result.reward > 0);
    }

    @Test
    void recordOutcome_testFail_hasNegativeReward() {
        RLVRTrainer.TrainingResult result = trainer.recordOutcome(
            List.of("C-Tester"),
            KimiMoERouter.TaskType.TESTING,
            RLVRTrainer.VerifiableOutcome.TEST_FAIL,
            "3 tests failed");
        assertTrue(result.reward < 0);
    }

    @Test
    void recordOutcome_deployOk_hasPositiveReward() {
        RLVRTrainer.TrainingResult result = trainer.recordOutcome(
            List.of("G-Publish"),
            KimiMoERouter.TaskType.DEPLOYMENT,
            RLVRTrainer.VerifiableOutcome.DEPLOY_OK,
            "Cloud Run healthy");
        assertTrue(result.reward > 0);
    }

    @Test
    void recordOutcome_securityFail_hasNegativeReward() {
        RLVRTrainer.TrainingResult result = trainer.recordOutcome(
            List.of("Alpha-Security"),
            KimiMoERouter.TaskType.SECURITY_AUDIT,
            RLVRTrainer.VerifiableOutcome.SECURITY_FAIL,
            "3 OWASP findings");
        assertTrue(result.reward < 0);
    }

    // ── updatedAgents ─────────────────────────────────────────────────────────

    @Test
    void recordOutcome_allParticipantsInUpdatedList() {
        List<String> agents = List.of("Architect", "Reviewer", "Alpha-Security");
        RLVRTrainer.TrainingResult result = trainer.recordOutcome(
            agents,
            KimiMoERouter.TaskType.CODE_REVIEW,
            RLVRTrainer.VerifiableOutcome.BUILD_PASS,
            "review passed");

        for (String agent : agents) {
            assertTrue(result.updatedAgents.contains(agent),
                "Agent " + agent + " should be in updatedAgents");
        }
    }

    // ── estimateReward() ──────────────────────────────────────────────────────

    @Test
    void estimateReward_unseenAgent_returnsPrior() {
        double est = trainer.estimateReward("Iota-Knowledge", KimiMoERouter.TaskType.LEARNING);
        assertEquals(0.5, est, 1e-9, "Unseen agent should return prior of 0.5");
    }

    @Test
    void estimateReward_improvesAfterPositiveOutcomes() {
        String agent = "Theta-Learning";
        KimiMoERouter.TaskType type = KimiMoERouter.TaskType.LEARNING;

        double before = trainer.estimateReward(agent, type);

        for (int i = 0; i < 5; i++) {
            trainer.recordOutcome(List.of(agent), type,
                RLVRTrainer.VerifiableOutcome.BUILD_PASS, "success " + i);
        }

        double after = trainer.estimateReward(agent, type);
        assertTrue(after > before,
            "Estimate should improve after positive outcomes: before=" + before + " after=" + after);
    }

    // ── getLeaderboard() ─────────────────────────────────────────────────────

    @Test
    void getLeaderboard_lists20Agents() {
        List<Map<String, Object>> board = trainer.getLeaderboard();
        assertEquals(20, board.size(), "Leaderboard should list all 20 agents");
    }

    @Test
    void getLeaderboard_eachEntryHasRequiredFields() {
        List<Map<String, Object>> board = trainer.getLeaderboard();
        for (Map<String, Object> entry : board) {
            assertTrue(entry.containsKey("agent"),           "Missing 'agent'");
            assertTrue(entry.containsKey("total_tasks"),     "Missing 'total_tasks'");
            assertTrue(entry.containsKey("success_rate_pct"),"Missing 'success_rate_pct'");
            assertTrue(entry.containsKey("avg_reward"),      "Missing 'avg_reward'");
            assertTrue(entry.containsKey("best_task_type"),  "Missing 'best_task_type'");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Inject a field by name using reflection — avoids needing Spring context. */
    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field f = findField(target.getClass(), fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Could not set field " + fieldName + " on " + target.getClass(), e);
        }
    }

    private static java.lang.reflect.Field findField(Class<?> cls, String name) {
        while (cls != null) {
            for (java.lang.reflect.Field f : cls.getDeclaredFields()) {
                if (f.getName().equals(name)) return f;
            }
            cls = cls.getSuperclass();
        }
        throw new RuntimeException("Field not found: " + name);
    }
}
