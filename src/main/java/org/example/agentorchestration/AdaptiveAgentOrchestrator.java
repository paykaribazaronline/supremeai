package org.example.agentorchestration;

import org.example.agentorchestration.learning.AgentPatternProfiler;
import org.example.agentorchestration.learning.ReasoningChainCopier;
import org.example.agentorchestration.learning.ReasoningGenerator;
import org.example.service.AgentDecisionLogger;
import org.example.service.SystemLearningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ADAPTIVE AGENT ORCHESTRATOR
 *
 * Central coordinator that wires all 4 orchestration techniques together:
 *
 *   MoE Router    → selects best agents for the task (sparse activation)
 *   RLVR Trainer  → records outcomes, updates weights via verifiable rewards
 *   MuonClip      → stabilizes weight updates (momentum + gradient clipping)
 *   Agentic Loop  → executes multi-step tool chains until goal is complete
 *
 * Full execution flow:
 *
 *   submit(task) ─┐
 *                 ├─ 1. MoE: route(taskType) → top-3 agents
 *                 ├─ 2. Agentic Loop: THINK→ACT→OBSERVE × N steps
 *                 ├─ 3. Verify outcome (build/test/deploy result)
 *                 ├─ 4. RLVR: recordOutcome → compute reward
 *                 ├─ 5. MuonClip: clip gradient → update routing weights
 *                 └─ 6. SystemLearning: store pattern (long-term memory)
 *
 * This pipeline combines sparse routing, verifiable rewards, tool execution,
 * and stabilization into one adaptive orchestration flow.
 */
@Service
public class AdaptiveAgentOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(AdaptiveAgentOrchestrator.class);

    @Autowired
    private ExpertAgentRouter moeRouter;

    @Autowired
    private RLVRTrainer rlvrTrainer;

    @Autowired
    private MuonClipOptimizer muonClip;

    @Autowired
    private AgenticToolLoop toolLoop;

    @Autowired
    private AgentDecisionLogger decisionLogger;

    @Autowired
    private SystemLearningService learningService;

    // ── Level 2/3/4 deep learning ──────────────────────────────────────────────
    @Autowired
    private AgentPatternProfiler patternProfiler;

    @Autowired
    private ReasoningChainCopier chainCopier;

    @Autowired
    private ReasoningGenerator reasoningGenerator;

    // History of all orchestrated tasks (for monitoring)
    private final List<TaskExecution> history = Collections.synchronizedList(new ArrayList<>());

    /**
    * Submit a task to SupremeAI's adaptive orchestration pipeline.
     *
     * @param request  task request with goal, type, and optional custom plan
     * @return         complete execution result with routing, loop, and RLVR data
     */
    public TaskExecution submit(TaskRequest request) {
        long start = System.currentTimeMillis();
        String taskId = "task-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6);

        logger.info("═══════════════════════════════════════════════════════");
        logger.info("🤖 Adaptive orchestration pipeline: taskId={} type={}", taskId, request.taskType);
        logger.info("   Goal: {}", request.goal);
        logger.info("═══════════════════════════════════════════════════════");

        TaskExecution exec = new TaskExecution(taskId, request);

        try {
            // ─── STEP 1: MoE ROUTING ────────────────────────────────────────
            logger.info("⟶ Step 1: MoE routing (top-{} agents)", request.topK);
            List<ExpertAgentRouter.RoutingDecision> routingDecisions =
                moeRouter.route(request.taskType, request.topK);

            List<String> selectedAgents = routingDecisions.stream()
                .map(ExpertAgentRouter.RoutingDecision::getAgentName)
                .collect(Collectors.toList());

            exec.setRoutingDecisions(routingDecisions);
            exec.setSelectedAgents(selectedAgents);

            logger.info("   Selected: {} (probabilities: {})",
                selectedAgents,
                routingDecisions.stream()
                    .map(d -> String.format("%.2f", d.getProbability()))
                    .collect(Collectors.joining(", ")));

            // ─── STEP 2: BUILD EXECUTION PLAN ──────────────────────────────
            logger.info("⟶ Step 2: Building execution plan");
            List<AgenticToolLoop.PlannedStep> plan = buildPlan(request, selectedAgents);
            exec.setPlan(plan);

            // ─── STEP 3: AGENTIC TOOL LOOP ─────────────────────────────────
            logger.info("⟶ Step 3: Agentic loop (max 10 steps)");
            AgenticToolLoop.LoopSession session = toolLoop.execute(
                taskId, request.goal, selectedAgents, request.taskType, plan);

            exec.setLoopSession(session);

            // ─── STEP 4: VERIFY OUTCOME (RLVR) ─────────────────────────────
            logger.info("⟶ Step 4: RLVR outcome recording");
            RLVRTrainer.VerifiableOutcome outcome = deriveOutcome(session, request.taskType);
            String details = buildDetails(session, request.goal);

            RLVRTrainer.TrainingResult training = rlvrTrainer.recordOutcome(
                selectedAgents, request.taskType, outcome, details);

            exec.setTrainingResult(training);

            // ─── STEP 5: UPDATE LEVEL 2/3 PROFILES & CHAINS ────────────────
            logger.info("⟶ Step 5: Updating L2 pattern profiles + L3 chain store");
            patternProfiler.buildAllProfiles();
            chainCopier.indexAllChains();

            // ─── STEP 6: MARK FINAL STATUS ─────────────────────────────────
            exec.setOutcome(outcome);
            exec.setSuccess(session.isCompleted());
            exec.setDurationMs(System.currentTimeMillis() - start);

            logger.info("═══════════════════════════════════════════════════════");
            logger.info("✅ Pipeline complete: taskId={} outcome={} reward={:.2f} duration={}ms",
                taskId, outcome, training.reward, exec.getDurationMs());
            logger.info("═══════════════════════════════════════════════════════");

        } catch (Exception e) {
            logger.error("💥 Pipeline error for taskId={}: {}", taskId, e.getMessage(), e);
            exec.setSuccess(false);
            exec.setErrorMessage(e.getMessage());
            exec.setDurationMs(System.currentTimeMillis() - start);
        }

        history.add(exec);
        return exec;
    }

    /**
     * Get live MoE routing summary (which agents handle which task types).
     */
    public Map<ExpertAgentRouter.TaskType, List<String>> getRoutingSummary() {
        return moeRouter.getRoutingSummary();
    }

    /**
     * Get RLVR agent performance leaderboard.
     */
    public List<Map<String, Object>> getLeaderboard() {
        return rlvrTrainer.getLeaderboard();
    }

    /**
     * Get last N task executions for monitoring.
     */
    public List<TaskExecution> getHistory(int limit) {
        List<TaskExecution> copy = new ArrayList<>(history);
        int from = Math.max(0, copy.size() - limit);
        return copy.subList(from, copy.size());
    }

    /**
     * Get MuonClip diagnostics for a specific agent.
     */
    public Map<String, Object> getMuonClipDiagnostics(String agentName) {
        return muonClip.getDiagnostics(agentName);
    }

    /**
     * Full system status snapshot.
     */
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("technique", "Adaptive Agent Orchestration (MoE + RLVR + MuonClip + Agentic Loop)");
        status.put("total_agents", ExpertAgentRouter.ALL_AGENTS.size());
        status.put("total_task_types", ExpertAgentRouter.TaskType.values().length);
        status.put("registered_tools", toolLoop.listTools().size());
        status.put("tasks_executed", history.size());

        long successes = history.stream().filter(TaskExecution::isSuccess).count();
        status.put("success_rate_pct", history.isEmpty() ? 0
            : Math.round((double) successes / history.size() * 100));

        status.put("active_loops", toolLoop.getActiveSessionsSummary().get("active_count"));
        status.put("routing_summary", getRoutingSummary());
        // Level 2/3/4 deep learning stats
        status.put("level2_profiles", patternProfiler.getAllProfileSummaries().size());
        status.put("level3_chains", chainCopier.getStats());
        status.put("level4_generator", reasoningGenerator.getStats());
        return status;
    }

    // ── Plan building ──────────────────────────────────────────────────────────

    private List<AgenticToolLoop.PlannedStep> buildPlan(
            TaskRequest request, List<String> agents) {

        if (request.customPlan != null && !request.customPlan.isEmpty()) {
            return toolLoop.buildPlan(request.customPlan.toArray(new String[0]));
        }

        // ── Level 4: try AI-generated tool plan from learned reasoning ─────
        if (!agents.isEmpty()) {
            try {
                String primaryAgent = agents.get(0);
                List<String> toolPlan = reasoningGenerator.generateToolPlan(
                    primaryAgent, request.taskType.name(), request.goal);
                if (toolPlan != null && toolPlan.size() >= 2) {
                    logger.info("   Level 4 plan for {}: {}", primaryAgent, toolPlan);
                    return toolLoop.buildPlan(toolPlan.toArray(new String[0]));
                }
            } catch (Exception e) {
                logger.debug("   Level 4 plan not available yet, using default: {}", e.getMessage());
            }
        }

        // Default plans per task type
        switch (request.taskType) {
            case CODE_GENERATION:
                return toolLoop.buildPlan(
                    "log_info:message=Starting code generation for: " + sanitize(request.goal),
                    "check_build_status",
                    "run_tests",
                    "commit_changes:message=feat: " + sanitize(request.goal));

            case BUG_FIX:
                return toolLoop.buildPlan(
                    "run_tests",
                    "analyze_error:error=" + sanitize(request.goal),
                    "apply_fix:description=" + sanitize(request.goal),
                    "run_tests",
                    "commit_changes:message=fix: " + sanitize(request.goal));

            case SECURITY_AUDIT:
                return toolLoop.buildPlan(
                    "run_security_scan",
                    "analyze_error:error=Review scan findings",
                    "log_info:message=Security scan complete");

            case DEPLOYMENT:
                return toolLoop.buildPlan(
                    "check_build_status",
                    "run_tests",
                    "check_deployment_health",
                    "commit_changes:message=deploy: " + sanitize(request.goal));

            case TESTING:
                return toolLoop.buildPlan(
                    "run_tests",
                    "analyze_error:error=Review test coverage",
                    "log_info:message=Testing phase complete");

            default:
                return toolLoop.buildPlan(
                    "log_info:message=Executing: " + sanitize(request.goal),
                    "check_build_status");
        }
    }

    private RLVRTrainer.VerifiableOutcome deriveOutcome(
            AgenticToolLoop.LoopSession session, ExpertAgentRouter.TaskType taskType) {

        if (!session.isCompleted()) {
            switch (taskType) {
                case CODE_GENERATION:
                case BUG_FIX:       return RLVRTrainer.VerifiableOutcome.BUILD_FAIL;
                case TESTING:       return RLVRTrainer.VerifiableOutcome.TEST_FAIL;
                case DEPLOYMENT:    return RLVRTrainer.VerifiableOutcome.DEPLOY_FAIL;
                case SECURITY_AUDIT: return RLVRTrainer.VerifiableOutcome.SECURITY_FAIL;
                default:            return RLVRTrainer.VerifiableOutcome.BUILD_FAIL;
            }
        }

        switch (taskType) {
            case CODE_GENERATION:
            case BUG_FIX:           return RLVRTrainer.VerifiableOutcome.BUILD_PASS;
            case TESTING:           return RLVRTrainer.VerifiableOutcome.TEST_PASS;
            case DEPLOYMENT:        return RLVRTrainer.VerifiableOutcome.DEPLOY_OK;
            case SECURITY_AUDIT:    return RLVRTrainer.VerifiableOutcome.SECURITY_OK;
            case META_IMPROVEMENT:  return RLVRTrainer.VerifiableOutcome.SELF_HEAL_OK;
            default:                return RLVRTrainer.VerifiableOutcome.BUILD_PASS;
        }
    }

    private String buildDetails(AgenticToolLoop.LoopSession session, String goal) {
        return String.format("goal='%s' steps=%d duration=%dms failed=%s",
            goal, session.getCurrentStep(), session.durationMs(), session.isFailed());
    }

    /** Sanitize user input for use in tool args / commit messages. */
    private String sanitize(String input) {
        if (input == null) return "task";
        return input.replaceAll("[^a-zA-Z0-9 _\\-.]", "")
                    .trim()
                    .substring(0, Math.min(input.length(), 80));
    }

    // ── Value objects ──────────────────────────────────────────────────────────

    /** Input to the orchestrator. */
    public static class TaskRequest {
        public String goal;
        public ExpertAgentRouter.TaskType taskType = ExpertAgentRouter.TaskType.CODE_GENERATION;
        public int topK = 3;
        public List<String> customPlan; // optional custom tool chain

        public TaskRequest() {}
        public TaskRequest(String goal, ExpertAgentRouter.TaskType taskType) {
            this.goal = goal;
            this.taskType = taskType;
        }
    }

    /** Complete execution record. */
    public static class TaskExecution {
        private final String taskId;
        private final TaskRequest request;
        private List<ExpertAgentRouter.RoutingDecision> routingDecisions;
        private List<String> selectedAgents;
        private List<AgenticToolLoop.PlannedStep> plan;
        private AgenticToolLoop.LoopSession loopSession;
        private RLVRTrainer.TrainingResult trainingResult;
        private RLVRTrainer.VerifiableOutcome outcome;
        private boolean success;
        private long durationMs;
        private String errorMessage;
        public final long createdAt = System.currentTimeMillis();

        public TaskExecution(String taskId, TaskRequest request) {
            this.taskId = taskId;
            this.request = request;
        }

        // Getters & setters
        public String getTaskId()                     { return taskId; }
        public TaskRequest getRequest()               { return request; }
        public boolean isSuccess()                    { return success; }
        public long getDurationMs()                   { return durationMs; }
        public RLVRTrainer.VerifiableOutcome getOutcome() { return outcome; }
        public List<String> getSelectedAgents()       { return selectedAgents; }
        public RLVRTrainer.TrainingResult getTrainingResult() { return trainingResult; }
        public AgenticToolLoop.LoopSession getLoopSession()   { return loopSession; }

        public void setRoutingDecisions(List<ExpertAgentRouter.RoutingDecision> d) { this.routingDecisions = d; }
        public void setSelectedAgents(List<String> a)          { this.selectedAgents = a; }
        public void setPlan(List<AgenticToolLoop.PlannedStep> p) { this.plan = p; }
        public void setLoopSession(AgenticToolLoop.LoopSession s) { this.loopSession = s; }
        public void setTrainingResult(RLVRTrainer.TrainingResult t) { this.trainingResult = t; }
        public void setOutcome(RLVRTrainer.VerifiableOutcome o)  { this.outcome = o; }
        public void setSuccess(boolean s)                        { this.success = s; }
        public void setDurationMs(long d)                        { this.durationMs = d; }
        public void setErrorMessage(String e)                    { this.errorMessage = e; }

        /** Compact summary map for API responses. */
        public Map<String, Object> toSummary() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("taskId", taskId);
            m.put("goal", request.goal);
            m.put("taskType", request.taskType);
            m.put("selectedAgents", selectedAgents);
            m.put("outcome", outcome);
            m.put("success", success);
            m.put("durationMs", durationMs);
            m.put("reward", trainingResult != null ? trainingResult.reward : null);
            m.put("stepsExecuted", loopSession != null ? loopSession.getCurrentStep() : 0);
            if (errorMessage != null) m.put("error", errorMessage);
            return m;
        }
    }
}
