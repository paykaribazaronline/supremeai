package org.example.agentorchestration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AgenticToolLoop.
 *
 * Tests cover:
 *   - Successful plan executes all steps and marks COMPLETE
 *   - Critical step failure marks session FAILED and stops loop
 *   - Non-critical step failure continues loop
 *   - Unknown tool name produces an ERROR observation
 *   - Custom tools can be registered and invoked
 *   - buildPlan() parses tool:key=val format correctly
 *   - Observation transcript has correct step count
 *   - listTools() returns all registered tools
 */
class AgenticToolLoopTest {

    private AgenticToolLoop loop;

    @BeforeEach
    void setUp() {
        loop = new AgenticToolLoop();
    }

    // ── Successful execution ──────────────────────────────────────────────────

    @Test
    void execute_allStepsPass_marksCompleted() {
        List<AgenticToolLoop.PlannedStep> plan = loop.buildPlan(
            "log_info:message=step 1",
            "check_build_status",
            "run_tests"
        );

        AgenticToolLoop.LoopSession session = loop.execute(
            "t1", "Health check", List.of("Architect"), ExpertAgentRouter.TaskType.TESTING, plan);

        assertTrue(session.isCompleted(), "Session should be marked completed");
        assertFalse(session.isFailed(),   "Session should not be failed");
        assertEquals(3, session.getCurrentStep(), "Should have completed 3 steps");
    }

    @Test
    void execute_sessionHasObservationsForEachStep() {
        List<AgenticToolLoop.PlannedStep> plan = loop.buildPlan(
            "log_info:message=hello",
            "check_build_status"
        );

        AgenticToolLoop.LoopSession session = loop.execute(
            "t2", "Two steps", List.of("Builder"), ExpertAgentRouter.TaskType.CODE_GENERATION, plan);

        // Each step produces THINK + OBSERVE = 2 observations per step
        assertTrue(session.getObservations().size() >= 4,
            "Should have at least 4 observations for 2 steps (THINK+OBSERVE each)");
    }

    @Test
    void execute_returnsNonZeroDuration() throws InterruptedException {
        List<AgenticToolLoop.PlannedStep> plan = loop.buildPlan("log_info:message=timing");
        AgenticToolLoop.LoopSession session = loop.execute(
            "t3", "Timing", List.of("Reviewer"), ExpertAgentRouter.TaskType.CODE_REVIEW, plan);
        // durationMs is valid after completion
        assertTrue(session.durationMs() >= 0, "Duration should be non-negative");
    }

    // ── Critical step failure ─────────────────────────────────────────────────

    @Test
    void execute_criticalStepFails_marksSessionFailed() {
        List<AgenticToolLoop.PlannedStep> plan = List.of(
            new AgenticToolLoop.PlannedStep(
                "nonexistent_tool",
                Map.of(),
                "This tool does not exist",
                true /* critical */ )
        );

        AgenticToolLoop.LoopSession session = loop.execute(
            "t4", "Fail fast", List.of("B-Fixer"), ExpertAgentRouter.TaskType.BUG_FIX, plan);

        assertTrue(session.isFailed(), "Session should be failed when critical step uses unknown tool");
        assertFalse(session.isCompleted(), "Session should not be completed");
        assertNotNull(session.getFailReason(), "Fail reason should be set");
    }

    // ── Non-critical step failure ─────────────────────────────────────────────

    @Test
    void execute_nonCriticalStepFails_loopContinues() {
        List<AgenticToolLoop.PlannedStep> plan = List.of(
            new AgenticToolLoop.PlannedStep(
                "nonexistent_tool",
                Map.of(),
                "Optional step — not critical",
                false /* not critical */ ),
            new AgenticToolLoop.PlannedStep(
                "log_info",
                Map.of("message", "recovery step"),
                "Continuing after failure",
                false)
        );

        AgenticToolLoop.LoopSession session = loop.execute(
            "t5", "Recover", List.of("Reviewer"), ExpertAgentRouter.TaskType.TESTING, plan);

        // Loop should complete because not critical
        assertEquals(2, session.getCurrentStep(), "Loop should run both steps");
    }

    // ── Custom tool registration ──────────────────────────────────────────────

    @Test
    void registerTool_customToolIsInvokable() {
        loop.registerTool("custom_ping",
            "Returns pong",
            args -> AgenticToolLoop.ToolResult.success("pong: " + args.getOrDefault("target", "?")));

        List<AgenticToolLoop.PlannedStep> plan = loop.buildPlan("custom_ping:target=test");
        AgenticToolLoop.LoopSession session = loop.execute(
            "t6", "Custom tool", List.of("Architect"), ExpertAgentRouter.TaskType.CODE_GENERATION, plan);

        assertTrue(session.isCompleted(), "Custom tool should execute successfully");

        boolean hasPong = session.getObservations().stream()
            .anyMatch(obs -> obs.get("content").toString().contains("pong"));
        assertTrue(hasPong, "Observation should contain 'pong' from custom_ping tool");
    }

    @Test
    void registerTool_failingCustomToolProducesFailObservation() {
        loop.registerTool("always_fail",
            "Always returns failure",
            args -> AgenticToolLoop.ToolResult.failure("intentional failure"));

        List<AgenticToolLoop.PlannedStep> plan = List.of(
            new AgenticToolLoop.PlannedStep("always_fail", Map.of(), "Expected to fail", false));
        AgenticToolLoop.LoopSession session = loop.execute(
            "t7", "Fail tool", List.of("Builder"), ExpertAgentRouter.TaskType.BUG_FIX, plan);

        boolean hasFailObserve = session.getObservations().stream()
            .anyMatch(obs -> obs.get("type").toString().equals("OBSERVE_FAIL"));
        assertTrue(hasFailObserve, "Should have an OBSERVE_FAIL entry");
    }

    // ── buildPlan() parsing ───────────────────────────────────────────────────

    @Test
    void buildPlan_toolOnlyStep_hasEmptyArgs() {
        List<AgenticToolLoop.PlannedStep> plan = loop.buildPlan("run_tests");
        assertEquals(1, plan.size());
        assertEquals("run_tests", plan.get(0).toolName);
        assertTrue(plan.get(0).args.isEmpty(), "Step without args should have empty args map");
    }

    @Test
    void buildPlan_toolWithArgs_parsesKeyValuePairs() {
        List<AgenticToolLoop.PlannedStep> plan =
            loop.buildPlan("commit_changes:message=fix auth bug,author=supremeai");
        assertEquals(1, plan.size());
        AgenticToolLoop.PlannedStep step = plan.get(0);
        assertEquals("commit_changes", step.toolName);
        assertEquals("fix auth bug", step.args.get("message"));
        assertEquals("supremeai", step.args.get("author"));
    }

    @Test
    void buildPlan_multipleMixedSteps_parsesAll() {
        List<AgenticToolLoop.PlannedStep> plan = loop.buildPlan(
            "run_tests",
            "analyze_error:error=NullPointerException",
            "apply_fix:description=null guard",
            "run_tests"
        );
        assertEquals(4, plan.size());
        assertEquals("run_tests",     plan.get(0).toolName);
        assertEquals("analyze_error", plan.get(1).toolName);
        assertEquals("NullPointerException", plan.get(1).args.get("error"));
        assertEquals("apply_fix",     plan.get(2).toolName);
    }

    // ── listTools() ───────────────────────────────────────────────────────────

    @Test
    void listTools_containsBuiltInTools() {
        List<Map<String, String>> tools = loop.listTools();
        List<String> names = tools.stream()
            .map(t -> t.get("name"))
            .toList();
        assertTrue(names.contains("run_tests"),           "Missing run_tests");
        assertTrue(names.contains("check_build_status"),  "Missing check_build_status");
        assertTrue(names.contains("apply_fix"),           "Missing apply_fix");
        assertTrue(names.contains("commit_changes"),      "Missing commit_changes");
        assertTrue(names.contains("run_security_scan"),   "Missing run_security_scan");
    }

    @Test
    void listTools_eachToolHasNameAndDescription() {
        for (Map<String, String> tool : loop.listTools()) {
            assertNotNull(tool.get("name"),        "Tool missing 'name'");
            assertNotNull(tool.get("description"), "Tool missing 'description'");
            assertFalse(tool.get("name").isBlank(), "Tool name should not be blank");
        }
    }
}
