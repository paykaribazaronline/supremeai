package org.example.agentorchestration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST API for SupremeAI's Adaptive Agent Orchestration system.
 *
 * Endpoints:
 *   POST /api/agent-orchestration/submit          — Submit a task through the full pipeline
 *   GET  /api/agent-orchestration/status          — Full system status snapshot
 *   GET  /api/agent-orchestration/routing         — MoE routing table (which agents handle what)
 *   GET  /api/agent-orchestration/leaderboard     — Agent performance leaderboard (RLVR scores)
 *   GET  /api/agent-orchestration/history         — Recent task execution history
 *   GET  /api/agent-orchestration/tools           — List all registered agentic tools
 *   GET  /api/agent-orchestration/muonclip/{agent}— MuonClip optimizer diagnostics
 *   GET  /api/agent-orchestration/task-types      — Supported task type enum values
 */
@RestController("adaptiveAgentOrchestrationController")
@RequestMapping("/api/agent-orchestration")
public class AgentOrchestrationController {

    @Autowired
    private AdaptiveAgentOrchestrator orchestrator;

    @Autowired
    private AgenticToolLoop toolLoop;

    /**
    * Submit a task to the full adaptive orchestration pipeline.
     *
     * Request body (JSON):
     * {
     *   "goal":       "Fix the failing AuthenticationFilterTest",
    *   "taskType":   "BUG_FIX",          // see /api/agent-orchestration/task-types
     *   "topK":       3,                   // optional, default 3
     *   "customPlan": ["run_tests", "analyze_error:error=auth failure", "apply_fix"]  // optional
     * }
     */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submit(
            @RequestBody Map<String, Object> body) {

        String goal = (String) body.getOrDefault("goal", "");
        if (goal.isBlank()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "goal is required"));
        }

        String taskTypeName = (String) body.getOrDefault("taskType", "CODE_GENERATION");
        ExpertAgentRouter.TaskType taskType;
        try {
            taskType = ExpertAgentRouter.TaskType.valueOf(taskTypeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid taskType: " + taskTypeName));
        }

        int topK = body.containsKey("topK") ? (int) body.get("topK") : 3;
        topK = Math.max(1, Math.min(topK, 5)); // bounded [1,5]

        AdaptiveAgentOrchestrator.TaskRequest req = new AdaptiveAgentOrchestrator.TaskRequest(goal, taskType);
        req.topK = topK;

        @SuppressWarnings("unchecked")
        List<String> customPlan = (List<String>) body.get("customPlan");
        req.customPlan = customPlan;

        AdaptiveAgentOrchestrator.TaskExecution exec = orchestrator.submit(req);
        return ResponseEntity.ok(exec.toSummary());
    }

    /**
     * Full system status: all 4 techniques, agent counts, success rates.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(orchestrator.getSystemStatus());
    }

    /**
     * MoE routing table: for each task type, which top-3 agents are assigned.
     */
    @GetMapping("/routing")
    public ResponseEntity<Map<String, Object>> routing() {
        Map<ExpertAgentRouter.TaskType, List<String>> raw = orchestrator.getRoutingSummary();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("description", "MoE routing table for Adaptive Agent Orchestration");
        response.put("total_agents", ExpertAgentRouter.ALL_AGENTS.size());
        response.put("all_agents", ExpertAgentRouter.ALL_AGENTS);
        Map<String, List<String>> routing = new LinkedHashMap<>();
        raw.forEach((k, v) -> routing.put(k.name(), v));
        response.put("routing", routing);
        return ResponseEntity.ok(response);
    }

    /**
     * RLVR agent performance leaderboard.
     */
    @GetMapping("/leaderboard")
    public ResponseEntity<Map<String, Object>> leaderboard() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("description", "RLVR agent performance leaderboard");
        response.put("leaderboard", orchestrator.getLeaderboard());
        return ResponseEntity.ok(response);
    }

    /**
     * Recent task execution history.
     *
     * @param limit how many recent tasks to return (default 20)
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> history(
            @RequestParam(defaultValue = "20") int limit) {
        limit = Math.max(1, Math.min(limit, 100));
        List<Map<String, Object>> summaries = new ArrayList<>();
        for (AdaptiveAgentOrchestrator.TaskExecution exec : orchestrator.getHistory(limit)) {
            summaries.add(exec.toSummary());
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("count", summaries.size());
        response.put("tasks", summaries);
        return ResponseEntity.ok(response);
    }

    /**
     * Backward-compatible task lookup endpoint used by older dashboard flows.
     */
    @GetMapping("/task/{taskId}")
    public ResponseEntity<Map<String, Object>> taskStatus(@PathVariable String taskId) {
        if (taskId == null || taskId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "taskId is required"));
        }

        for (AdaptiveAgentOrchestrator.TaskExecution exec : orchestrator.getHistory(1000)) {
            if (taskId.equals(exec.getTaskId())) {
                return ResponseEntity.ok(exec.toSummary());
            }
        }

        return ResponseEntity.status(404)
            .body(Map.of("error", "Task not found", "taskId", taskId));
    }

    /**
     * List all agentic tools currently registered in the loop.
     */
    @GetMapping("/tools")
    public ResponseEntity<Map<String, Object>> tools() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("description", "Agentic tool registry for Adaptive Agent Orchestration");
        response.put("tools", toolLoop.listTools());
        return ResponseEntity.ok(response);
    }

    /**
     * Active tool loop sessions (in-flight).
     */
    @GetMapping("/active-loops")
    public ResponseEntity<Map<String, Object>> activeLoops() {
        return ResponseEntity.ok(toolLoop.getActiveSessionsSummary());
    }

    /**
     * MuonClip optimizer diagnostics for a specific agent.
     */
    @GetMapping("/muonclip/{agentName}")
    public ResponseEntity<Map<String, Object>> muonClipDiagnostics(
            @PathVariable String agentName) {
        if (!ExpertAgentRouter.ALL_AGENTS.contains(agentName)) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Unknown agent: " + agentName,
                             "valid_agents", ExpertAgentRouter.ALL_AGENTS));
        }
        return ResponseEntity.ok(orchestrator.getMuonClipDiagnostics(agentName));
    }

    /**
     * List all supported task types.
     */
    @GetMapping("/task-types")
    public ResponseEntity<Map<String, Object>> taskTypes() {
        List<String> types = Arrays.stream(ExpertAgentRouter.TaskType.values())
            .map(Enum::name)
            .collect(java.util.stream.Collectors.toList());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("task_types", types);
        response.put("example", Map.of(
            "goal", "Fix AuthenticationFilterTest",
            "taskType", "BUG_FIX",
            "topK", 3
        ));
        return ResponseEntity.ok(response);
    }
}
