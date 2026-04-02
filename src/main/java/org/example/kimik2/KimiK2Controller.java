package org.example.kimik2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST API for SupremeAI's Kimi K2 Training System.
 *
 * Endpoints:
 *   POST /api/kimik2/submit          — Submit a task through the full pipeline
 *   GET  /api/kimik2/status          — Full system status snapshot
 *   GET  /api/kimik2/routing         — MoE routing table (which agents handle what)
 *   GET  /api/kimik2/leaderboard     — Agent performance leaderboard (RLVR scores)
 *   GET  /api/kimik2/history         — Recent task execution history
 *   GET  /api/kimik2/tools           — List all registered agentic tools
 *   GET  /api/kimik2/muonclip/{agent}— MuonClip optimizer diagnostics
 *   GET  /api/kimik2/task-types      — Supported task type enum values
 */
@RestController
@RequestMapping("/api/kimik2")
public class KimiK2Controller {

    @Autowired
    private KimiK2Orchestrator orchestrator;

    @Autowired
    private AgenticToolLoop toolLoop;

    /**
     * Submit a task to the full Kimi K2 pipeline.
     *
     * Request body (JSON):
     * {
     *   "goal":       "Fix the failing AuthenticationFilterTest",
     *   "taskType":   "BUG_FIX",          // see /api/kimik2/task-types
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
        KimiMoERouter.TaskType taskType;
        try {
            taskType = KimiMoERouter.TaskType.valueOf(taskTypeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid taskType: " + taskTypeName));
        }

        int topK = body.containsKey("topK") ? (int) body.get("topK") : 3;
        topK = Math.max(1, Math.min(topK, 5)); // bounded [1,5]

        KimiK2Orchestrator.TaskRequest req = new KimiK2Orchestrator.TaskRequest(goal, taskType);
        req.topK = topK;

        @SuppressWarnings("unchecked")
        List<String> customPlan = (List<String>) body.get("customPlan");
        req.customPlan = customPlan;

        KimiK2Orchestrator.TaskExecution exec = orchestrator.submit(req);
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
        Map<KimiMoERouter.TaskType, List<String>> raw = orchestrator.getRoutingSummary();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("description", "MoE routing table (Kimi K2 technique 1)");
        response.put("total_agents", KimiMoERouter.ALL_AGENTS.size());
        response.put("all_agents", KimiMoERouter.ALL_AGENTS);
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
        response.put("description", "RLVR Agent Performance Leaderboard (Kimi K2 technique 2)");
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
        for (KimiK2Orchestrator.TaskExecution exec : orchestrator.getHistory(limit)) {
            summaries.add(exec.toSummary());
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("count", summaries.size());
        response.put("tasks", summaries);
        return ResponseEntity.ok(response);
    }

    /**
     * List all agentic tools currently registered in the loop.
     */
    @GetMapping("/tools")
    public ResponseEntity<Map<String, Object>> tools() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("description", "Agentic tool registry (Kimi K2 technique 4)");
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
        if (!KimiMoERouter.ALL_AGENTS.contains(agentName)) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Unknown agent: " + agentName,
                             "valid_agents", KimiMoERouter.ALL_AGENTS));
        }
        return ResponseEntity.ok(orchestrator.getMuonClipDiagnostics(agentName));
    }

    /**
     * List all supported task types.
     */
    @GetMapping("/task-types")
    public ResponseEntity<Map<String, Object>> taskTypes() {
        List<String> types = Arrays.stream(KimiMoERouter.TaskType.values())
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
