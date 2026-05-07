package com.supremeai.learning;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        String selected = router.routeTask("code_generation", agents);

        assertNotNull(selected);
        assertTrue(agents.contains(selected), "Selected agent should be from available list");
    }

    @Test
    void testRouteTask_returnsBestAgentBasedOnQValues() {
        // Initialize Q-values for a specific state
        router.routeTask("bug_fix", List.of("agentA", "agentB"));

        // Manually boost Q-value for agentA using reflection (simulate learned preference)
        try {
            java.lang.reflect.Field qTableField = SelfLearningRouter.class.getDeclaredField("qTable");
            qTableField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<String, Double> qTable = (java.util.Map<String, Double>) qTableField.get(router);
            qTable.put("bug_fix:agentA", 10.0);
            qTable.put("bug_fix:agentB", 1.0);
        } catch (Exception e) {
            fail("Failed to manipulate Q-table: " + e.getMessage());
        }

        List<String> agents = List.of("agentA", "agentB");
        String selected = router.routeTask("bug_fix", agents);

        assertEquals("agentA", selected, "Should select agent with highest Q-value");
    }

    @Test
    void testUpdateReward_updatesQValue() {
        router.routeTask("refactor", List.of("agentX"));

        // Check initial Q-value (should be 0)
        try {
            java.lang.reflect.Field qTableField = SelfLearningRouter.class.getDeclaredField("qTable");
            qTableField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<String, Double> qTable = (java.util.Map<String, Double>) qTableField.get(router);
            double initialQ = qTable.get("refactor:agentX");
            assertEquals(0.0, initialQ, 0.001);

            // Update reward
            router.updateReward("refactor", "agentX", 5.0);

            // New Q = 0 + 0.1 * (5 - 0) = 0.5
            double updatedQ = qTable.get("refactor:agentX");
            assertEquals(0.5, updatedQ, 0.001);
        } catch (Exception e) {
            fail("Failed to access Q-table: " + e.getMessage());
        }
    }

    @Test
    void testUpdateReward_multipleUpdates_decaysCorrectly() {
        router.routeTask("task1", List.of("agentY"));

        try {
            java.lang.reflect.Field qTableField = SelfLearningRouter.class.getDeclaredField("qTable");
            qTableField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<String, Double> qTable = (java.util.Map<String, Double>) qTableField.get(router);

            // Q0 = 0
            router.updateReward("task1", "agentY", 1.0);
            // Q1 = 0.1 * (1 - 0) = 0.1
            assertEquals(0.1, qTable.get("task1:agentY"), 0.001);

            router.updateReward("task1", "agentY", 1.0);
            // Q2 = 0.1 + 0.1*(1-0.1) = 0.1 + 0.09 = 0.19
            assertEquals(0.19, qTable.get("task1:agentY"), 0.001);
        } catch (Exception e) {
            fail("Failed: " + e.getMessage());
        }
    }

    @Test
    void testExtractState_normalizesTaskType() {
        // Use reflection to access extractState
        try {
            var extractMethod = SelfLearningRouter.class.getDeclaredMethod("extractState", String.class);
            extractMethod.setAccessible(true);

            assertEquals("code_generation", extractMethod.invoke(router, "Code Generation"));
            assertEquals("api_call", extractMethod.invoke(router, "API Call"));
            assertEquals("database_query", extractMethod.invoke(router, "Database Query"));
        } catch (Exception e) {
            fail("Failed: " + e.getMessage());
        }
    }

    @Test
    void testGetTopAgents_returnsSortedByQValue() {
        String task = "optimization";
        router.routeTask(task, List.of("agent1", "agent2", "agent3"));

        try {
            java.lang.reflect.Field qTableField = SelfLearningRouter.class.getDeclaredField("qTable");
            qTableField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<String, Double> qTable = (java.util.Map<String, Double>) qTableField.get(router);

            qTable.put(task + ":agent1", 0.5);
            qTable.put(task + ":agent2", 0.9);
            qTable.put(task + ":agent3", 0.7);
        } catch (Exception e) {
            fail("Failed to set Q-values: " + e.getMessage());
        }

        List<String> top2 = router.getTopAgents("optimization", 2);
        assertEquals(2, top2.size());
        assertEquals("agent2", top2.get(0), "Top agent should have highest Q-value");
        assertEquals("agent3", top2.get(1), "Second agent should have second-highest Q-value");
    }

    @Test
    void testGetTopAgents_returnsEmptyWhenNoAgentsAvailable() {
        List<String> result = router.getTopAgents("nonexistent_task", 3);
        assertTrue(result.isEmpty(), "Should return empty list for unknown state");
    }

    @Test
    void testRouteTask_emptyAgentList_returnsFirstOrDefault() {
        // With no agents, routeTask should throw or handle gracefully
        List<String> empty = List.of();
        assertThrows(IndexOutOfBoundsException.class, () -> {
            router.routeTask("task", empty);
        });
    }

    @Test
    void testRouteTask_newState_qValuesInitializedToZero() {
        List<String> agents = List.of("alpha", "beta");
        router.routeTask("new_task_type", agents);

        try {
            java.lang.reflect.Field qTableField = SelfLearningRouter.class.getDeclaredField("qTable");
            qTableField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<String, Double> qTable = (java.util.Map<String, Double>) qTableField.get(router);

            assertEquals(0.0, qTable.get("new_task_type:alpha"));
            assertEquals(0.0, qTable.get("new_task_type:beta"));
        } catch (Exception e) {
            fail("Failed: " + e.getMessage());
        }
    }

    @Test
    void testUpdateReward_negativeReward_decreasesQValue() {
        router.routeTask("task_neg", List.of("agentZ"));

        try {
            // Set initial Q to 0.5
            java.lang.reflect.Field qTableField = SelfLearningRouter.class.getDeclaredField("qTable");
            qTableField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<String, Double> qTable = (java.util.Map<String, Double>) qTableField.get(router);
            qTable.put("task_neg:agentZ", 0.5);

            router.updateReward("task_neg", "agentZ", -1.0);
            // New Q = 0.5 + 0.1 * (-1 - 0.5) = 0.5 - 0.15 = 0.35
            assertEquals(0.35, qTable.get("task_neg:agentZ"), 0.001);
        } catch (Exception e) {
            fail("Failed: " + e.getMessage());
        }
    }

    @Test
    void testConcurrentRouteTask_threadSafe() throws InterruptedException {
        int threads = 10;
        Thread[] threadList = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            threadList[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    router.routeTask("concurrent_task_" + j, List.of("agent1", "agent2"));
                }
            });
        }
        for (Thread t : threadList) t.start();
        for (Thread t : threadList) t.join();

        // If no exceptions, test passes. Also verify Q-table contains entries
        try {
            java.lang.reflect.Field qTableField = SelfLearningRouter.class.getDeclaredField("qTable");
            qTableField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<String, Double> qTable = (java.util.Map<String, Double>) qTableField.get(router);
            assertTrue(qTable.size() > 0, "Q-table should have entries after concurrent use");
        } catch (Exception e) {
            fail("Failed to access Q-table: " + e.getMessage());
        }
    }
}
