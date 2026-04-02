package org.example.agentorchestration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ExpertAgentRouter.
 *
 * Tests cover:
 *   - Routing returns correct number of agents
 *   - Shared expert (Architect) is always included
 *   - Weight updates change routing order
 *   - Softmax probabilities sum to ~1.0
 *   - Routing summary covers all task types
 */
class ExpertAgentRouterTest {

    private ExpertAgentRouter router;

    @BeforeEach
    void setUp() {
        router = new ExpertAgentRouter();
    }

    // ── route() ───────────────────────────────────────────────────────────────

    @Test
    void route_returnsRequestedTopK() {
        List<ExpertAgentRouter.RoutingDecision> decisions =
            router.route(ExpertAgentRouter.TaskType.CODE_GENERATION, 4);

        // top-4 + shared expert Architect (may already be in top-4)
        assertTrue(decisions.size() >= 4, "Should return at least 4 agents");
    }

    @Test
    void route_alwaysIncludesSharedExpert() {
        for (ExpertAgentRouter.TaskType taskType : ExpertAgentRouter.TaskType.values()) {
            List<ExpertAgentRouter.RoutingDecision> decisions = router.route(taskType, 2);
            boolean hasArchitect = decisions.stream()
                .anyMatch(d -> d.getAgentName().equals("Architect"));
            assertTrue(hasArchitect,
                "Architect (shared expert) must always be included for taskType=" + taskType);
        }
    }

    @Test
    void route_defaultTopK_returnsThreeOrMore() {
        List<ExpertAgentRouter.RoutingDecision> decisions =
            router.route(ExpertAgentRouter.TaskType.SECURITY_AUDIT);
        assertTrue(decisions.size() >= 3, "Default top-K should return at least 3 agents");
    }

    @Test
    void route_noAgentRepeated() {
        List<ExpertAgentRouter.RoutingDecision> decisions =
            router.route(ExpertAgentRouter.TaskType.DEPLOYMENT, 5);
        long distinct = decisions.stream()
            .map(ExpertAgentRouter.RoutingDecision::getAgentName)
            .distinct().count();
        assertEquals(decisions.size(), distinct, "No agent should appear twice in routing");
    }

    @Test
    void route_allAgentsAreKnown() {
        List<ExpertAgentRouter.RoutingDecision> decisions =
            router.route(ExpertAgentRouter.TaskType.TESTING, 5);
        for (ExpertAgentRouter.RoutingDecision d : decisions) {
            assertTrue(ExpertAgentRouter.ALL_AGENTS.contains(d.getAgentName()),
                "Unknown agent in routing: " + d.getAgentName());
        }
    }

    @Test
    void route_softmaxProbabilitiesSumToApproximatelyOne() {
        List<ExpertAgentRouter.RoutingDecision> all =
            router.route(ExpertAgentRouter.TaskType.CODE_GENERATION, ExpertAgentRouter.ALL_AGENTS.size());
        double sumProb = all.stream().mapToDouble(ExpertAgentRouter.RoutingDecision::getProbability).sum();
        assertEquals(1.0, sumProb, 0.05, "Softmax probabilities should sum to ~1.0");
    }

    // ── updateWeight() ────────────────────────────────────────────────────────

    @Test
    void updateWeight_positiveRewardIncreasesAgentScore() {
        ExpertAgentRouter.TaskType type = ExpertAgentRouter.TaskType.BUG_FIX;
        String agent = "B-Fixer";

        // Get initial order
        List<ExpertAgentRouter.RoutingDecision> before = router.route(type, 5);
        int rankBefore = rankOf(before, agent);

        // Give B-Fixer a strong positive reward 10 times
        for (int i = 0; i < 10; i++) {
            router.updateWeight(agent, type, +1.0, 0.05);
        }

        List<ExpertAgentRouter.RoutingDecision> after = router.route(type, 5);
        int rankAfter = rankOf(after, agent);

        // Rank should improve (lower index = better)
        assertTrue(rankAfter <= rankBefore,
            "B-Fixer rank should improve after positive rewards: before=" + rankBefore + " after=" + rankAfter);
    }

    @Test
    void updateWeight_negativeRewardDoesNotCollapseWeightBelowMinimum() {
        String agent = "Architect";
        ExpertAgentRouter.TaskType type = ExpertAgentRouter.TaskType.COST_OPTIMIZATION;

        // Hammer with negative rewards
        for (int i = 0; i < 100; i++) {
            router.updateWeight(agent, type, -1.0, 0.1);
        }

        // Weight should clamp at minimum (0.01), never go to 0 or negative
        Map<String, Map<ExpertAgentRouter.TaskType, Double>> snapshot = router.getWeightSnapshot();
        double weight = snapshot.get(agent).getOrDefault(type, 0.01);
        assertTrue(weight >= 0.01, "Weight must not collapse below minimum 0.01, got: " + weight);
    }

    // ── getRoutingSummary() ───────────────────────────────────────────────────

    @Test
    void getRoutingSummary_coversAllTaskTypes() {
        Map<ExpertAgentRouter.TaskType, List<String>> summary = router.getRoutingSummary();
        for (ExpertAgentRouter.TaskType tt : ExpertAgentRouter.TaskType.values()) {
            assertTrue(summary.containsKey(tt), "Summary missing task type: " + tt);
            assertFalse(summary.get(tt).isEmpty(), "Summary has empty agents for: " + tt);
        }
    }

    @Test
    void getRoutingSummary_securityAuditPreferssSecurityAgents() {
        Map<ExpertAgentRouter.TaskType, List<String>> summary = router.getRoutingSummary();
        List<String> secAgents = summary.get(ExpertAgentRouter.TaskType.SECURITY_AUDIT);
        // At least one security specialist should appear
        boolean hasSecurityAgent = secAgents.stream()
            .anyMatch(a -> a.startsWith("Alpha") || a.startsWith("Beta") || a.startsWith("Gamma"));
        assertTrue(hasSecurityAgent,
            "Security audit routing should include at least one security agent, got: " + secAgents);
    }

    @Test
    void allAgentsList_has20Agents() {
        assertEquals(20, ExpertAgentRouter.ALL_AGENTS.size(), "SupremeAI should have exactly 20 agents");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int rankOf(List<ExpertAgentRouter.RoutingDecision> decisions, String agentName) {
        for (int i = 0; i < decisions.size(); i++) {
            if (decisions.get(i).getAgentName().equals(agentName)) return i;
        }
        return Integer.MAX_VALUE; // not found
    }
}
