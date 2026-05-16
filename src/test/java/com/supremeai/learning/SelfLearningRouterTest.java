package com.supremeai.learning;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SelfLearningRouter (Q-Learning based router).
 * Tests route selection, reward updates, and top-N retrieval.
 */
class SelfLearningRouterTest {

    private SelfLearningRouter router;

    @BeforeEach
    void setUp() {
        router = new SelfLearningRouter();
    }

    @Test
    void testRouteTask_initializesQValues() {
        List<String> agents = List.of("agent1", "agent2", "agent3");
        SelfLearningRouter.RoutingDecision decision = router.routeTask("code_generation", "sig1", agents);

        assertNotNull(decision.agentId);
        assertTrue(agents.contains(decision.agentId), "Selected agent should be from available list");
    }

    @Test
    void testRouteTask_returnsBestAgentBasedOnQValues() {
        // Initialize Q-values for a specific state
        router.routeTask("bug_fix", "sig2", List.of("agentA", "agentB"));

        // Manually boost Q-value for agentA using reflection (simulate learned preference)
        try {
            java.lang.reflect.Field qTableField = SelfLearningRouter.class.getDeclaredField("qTable");
            qTableField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<String, java.util.Map<String, Double>> qTable = (java.util.Map<String, java.util.Map<String, Double>>) qTableField.get(router);
            String state = "bug_fix:" + "sig2".hashCode();
            qTable.put(state, new java.util.concurrent.ConcurrentHashMap<>(Map.of("agentA", 10.0, "agentB", 1.0)));
        } catch (Exception e) {
            fail("Failed to manipulate Q-table: " + e.getMessage());
        }

        List<String> agents = List.of("agentA", "agentB");
        SelfLearningRouter.RoutingDecision decision = router.routeTask("bug_fix", "sig2", agents);

        assertEquals("agentA", decision.agentId, "Should select agent with highest Q-value");
    }

    @Test
    void testUpdateFromOutcome_updatesQValue() {
        router.routeTask("refactor", "sig3", List.of("agentX"));

        try {
            java.lang.reflect.Field qTableField = SelfLearningRouter.class.getDeclaredField("qTable");
            qTableField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<String, java.util.Map<String, Double>> qTable = (java.util.Map<String, java.util.Map<String, Double>>) qTableField.get(router);
            String state = "refactor:" + "sig3".hashCode();
            
            double initialQ = qTable.get(state).getOrDefault("agentX", 0.0);
            assertEquals(0.0, initialQ, 0.001);

            // Update reward
            router.updateFromOutcome("refactor", "sig3", "agentX", true, 100);

            double updatedQ = qTable.get(state).get("agentX");
            assertTrue(updatedQ > 0.0);
        } catch (Exception e) {
            fail("Failed to access Q-table: " + e.getMessage());
        }
    }

    @Test
    void testGetAgentStats() {
        router.routeTask("opt", "sig4", List.of("agentY"));
        router.updateFromOutcome("opt", "sig4", "agentY", true, 500);

        Map<String, Object> stats = router.getAgentStats("agentY");
        assertNotNull(stats);
        assertEquals(1, stats.get("totalRoutes"));
        assertEquals(1, stats.get("successfulRoutes"));
    }

    @Test
    void testConcurrentRouteTask_threadSafe() throws InterruptedException {
        int threads = 10;
        Thread[] threadList = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            final int index = i;
            threadList[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    router.routeTask("concurrent_task_" + j, "sig" + index, List.of("agent1", "agent2"));
                }
            });
        }
        for (Thread t : threadList) t.start();
        for (Thread t : threadList) t.join();

        try {
            java.lang.reflect.Field qTableField = SelfLearningRouter.class.getDeclaredField("qTable");
            qTableField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<String, java.util.Map<String, Double>> qTable = (java.util.Map<String, java.util.Map<String, Double>>) qTableField.get(router);
            assertTrue(qTable.size() > 0, "Q-table should have entries after concurrent use");
        } catch (Exception e) {
            fail("Failed to access Q-table: " + e.getMessage());
        }
    }
}
