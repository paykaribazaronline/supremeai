package org.example.kimik2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for KimiMoERouter.
 *
 * Tests cover:
 *   - Routing returns correct number of agents
 *   - Shared expert (Architect) is always included
 *   - Weight updates change routing order
 *   - Softmax probabilities sum to ~1.0
 *   - Routing summary covers all task types
 */
class KimiMoERouterTest {

    private KimiMoERouter router;

    @BeforeEach
    void setUp() {
        router = new KimiMoERouter();
    }

    // ── route() ───────────────────────────────────────────────────────────────

    @Test
    void route_returnsRequestedTopK() {
        List<KimiMoERouter.RoutingDecision> decisions =
            router.route(KimiMoERouter.TaskType.CODE_GENERATION, 4);

        // top-4 + shared expert Architect (may already be in top-4)
        assertTrue(decisions.size() >= 4, "Should return at least 4 agents");
    }

    @Test
    void route_alwaysIncludesSharedExpert() {
        for (KimiMoERouter.TaskType taskType : KimiMoERouter.TaskType.values()) {
            List<KimiMoERouter.RoutingDecision> decisions = router.route(taskType, 2);
            boolean hasArchitect = decisions.stream()
                .anyMatch(d -> d.getAgentName().equals("Architect"));
            assertTrue(hasArchitect,
                "Architect (shared expert) must always be included for taskType=" + taskType);
        }
    }

    @Test
    void route_defaultTopK_returnsThreeOrMore() {
        List<KimiMoERouter.RoutingDecision> decisions =
            router.route(KimiMoERouter.TaskType.SECURITY_AUDIT);
        assertTrue(decisions.size() >= 3, "Default top-K should return at least 3 agents");
    }

    @Test
    void route_noAgentRepeated() {
        List<KimiMoERouter.RoutingDecision> decisions =
            router.route(KimiMoERouter.TaskType.DEPLOYMENT, 5);
        long distinct = decisions.stream()
            .map(KimiMoERouter.RoutingDecision::getAgentName)
            .distinct().count();
        assertEquals(decisions.size(), distinct, "No agent should appear twice in routing");
    }

    @Test
    void route_allAgentsAreKnown() {
        List<KimiMoERouter.RoutingDecision> decisions =
            router.route(KimiMoERouter.TaskType.TESTING, 5);
        for (KimiMoERouter.RoutingDecision d : decisions) {
            assertTrue(KimiMoERouter.ALL_AGENTS.contains(d.getAgentName()),
                "Unknown agent in routing: " + d.getAgentName());
        }
    }

    @Test
    void route_softmaxProbabilitiesSumToApproximatelyOne() {
        List<KimiMoERouter.RoutingDecision> all =
            router.route(KimiMoERouter.TaskType.CODE_GENERATION, KimiMoERouter.ALL_AGENTS.size());
        double sumProb = all.stream().mapToDouble(KimiMoERouter.RoutingDecision::getProbability).sum();
        assertEquals(1.0, sumProb, 0.05, "Softmax probabilities should sum to ~1.0");
    }

    // ── updateWeight() ────────────────────────────────────────────────────────

    @Test
    void updateWeight_positiveRewardIncreasesAgentScore() {
        KimiMoERouter.TaskType type = KimiMoERouter.TaskType.BUG_FIX;
        String agent = "B-Fixer";

        // Get initial order
        List<KimiMoERouter.RoutingDecision> before = router.route(type, 5);
        int rankBefore = rankOf(before, agent);

        // Give B-Fixer a strong positive reward 10 times
        for (int i = 0; i < 10; i++) {
            router.updateWeight(agent, type, +1.0, 0.05);
        }

        List<KimiMoERouter.RoutingDecision> after = router.route(type, 5);
        int rankAfter = rankOf(after, agent);

        // Rank should improve (lower index = better)
        assertTrue(rankAfter <= rankBefore,
            "B-Fixer rank should improve after positive rewards: before=" + rankBefore + " after=" + rankAfter);
    }

    @Test
    void updateWeight_negativeRewardDoesNotCollapseWeightBelowMinimum() {
        String agent = "Architect";
        KimiMoERouter.TaskType type = KimiMoERouter.TaskType.COST_OPTIMIZATION;

        // Hammer with negative rewards
        for (int i = 0; i < 100; i++) {
            router.updateWeight(agent, type, -1.0, 0.1);
        }

        // Weight should clamp at minimum (0.01), never go to 0 or negative
        Map<String, Map<KimiMoERouter.TaskType, Double>> snapshot = router.getWeightSnapshot();
        double weight = snapshot.get(agent).getOrDefault(type, 0.01);
        assertTrue(weight >= 0.01, "Weight must not collapse below minimum 0.01, got: " + weight);
    }

    // ── getRoutingSummary() ───────────────────────────────────────────────────

    @Test
    void getRoutingSummary_coversAllTaskTypes() {
        Map<KimiMoERouter.TaskType, List<String>> summary = router.getRoutingSummary();
        for (KimiMoERouter.TaskType tt : KimiMoERouter.TaskType.values()) {
            assertTrue(summary.containsKey(tt), "Summary missing task type: " + tt);
            assertFalse(summary.get(tt).isEmpty(), "Summary has empty agents for: " + tt);
        }
    }

    @Test
    void getRoutingSummary_securityAuditPreferssSecurityAgents() {
        Map<KimiMoERouter.TaskType, List<String>> summary = router.getRoutingSummary();
        List<String> secAgents = summary.get(KimiMoERouter.TaskType.SECURITY_AUDIT);
        // At least one security specialist should appear
        boolean hasSecurityAgent = secAgents.stream()
            .anyMatch(a -> a.startsWith("Alpha") || a.startsWith("Beta") || a.startsWith("Gamma"));
        assertTrue(hasSecurityAgent,
            "Security audit routing should include at least one security agent, got: " + secAgents);
    }

    @Test
    void allAgentsList_has20Agents() {
        assertEquals(20, KimiMoERouter.ALL_AGENTS.size(), "SupremeAI should have exactly 20 agents");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int rankOf(List<KimiMoERouter.RoutingDecision> decisions, String agentName) {
        for (int i = 0; i < decisions.size(); i++) {
            if (decisions.get(i).getAgentName().equals(agentName)) return i;
        }
        return Integer.MAX_VALUE; // not found
    }
}
