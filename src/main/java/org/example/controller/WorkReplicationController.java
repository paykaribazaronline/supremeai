package org.example.controller;

import org.example.service.WorkReplicationService;
import org.example.service.WorkReplicationService.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import java.util.*;

/**
 * WorkReplicationController - API for autonomous work execution
 * 
 * Endpoints:
 * - RECORD: /api/teach/action - Admin records work for learning
 * - EXECUTE: /api/teach/execute - System executes learned pattern
 * - STATS: /api/teach/patterns - View learned patterns & stats
 */
@RestController
@RequestMapping("/api/teach")
@CrossOrigin(origins = "*")
public class WorkReplicationController {
    
    private final WorkReplicationService workReplication;
    
    public WorkReplicationController(WorkReplicationService workReplication) {
        this.workReplication = workReplication;
    }
    
    /**
     * Record admin action for the system to learn from
     * 
     * POST /api/teach/action
     * {
     *   "actionType": "CODE_GENERATION|COMMIT_CHANGES|PUSH_CODE|RUN_TESTS|DEPLOY|DOCUMENT|FIX_ERRORS",
     *   "context": {
     *     "requirement": "...",
     *     "framework": "...",
     *     "branch": "...",
     *     ...custom fields...
     *   },
     *   "result": "success|error|partial",
     *   "details": "optional description"
     * }
     */
    @PostMapping("/action")
    public ResponseEntity<?> recordAction(@RequestBody Map<String, Object> payload) {
        try {
            String actionType = (String) payload.get("actionType");
            Map<String, Object> context = (Map<String, Object>) payload.getOrDefault("context", new HashMap<>());
            String result = (String) payload.getOrDefault("result", "unknown");
            
            if (actionType == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Missing actionType",
                    "validTypes", Arrays.asList(
                        "CODE_GENERATION", "COMMIT_CHANGES", "PUSH_CODE", 
                        "RUN_TESTS", "DEPLOY", "DOCUMENT", "FIX_ERRORS"
                    )
                ));
            }
            
            workReplication.recordAction(actionType, context, result);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Action recorded: " + actionType,
                "timestamp", System.currentTimeMillis(),
                "status", "learning"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to record action: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Execute a learned work pattern autonomously
     * 
     * POST /api/teach/execute
     * {
     *   "pattern": "feature_development|bug_fix_cycle|custom_pattern_name",
     *   "inputs": {
     *     "requirement": "...",
     *     "framework": "...",
     *     "targetBranch": "...",
     *     ...pattern-specific inputs...
     *   }
     * }
     */
    @PostMapping("/execute")
    public ResponseEntity<?> executePattern(@RequestBody Map<String, Object> payload) {
        try {
            String patternName = (String) payload.get("pattern");
            Map<String, Object> inputs = (Map<String, Object>) payload.getOrDefault("inputs", new HashMap<>());
            
            if (patternName == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Missing pattern name",
                    "learnedPatterns", workReplication.getLearnedPatterns().keySet()
                ));
            }
            
            System.out.println("🚀 SupremeAI executing pattern: " + patternName);
            
            WorkReplicationService.ExecutionResult result = workReplication.executePattern(patternName, inputs);
            
            if (result.success) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "pattern", patternName,
                    "status", "completed",
                    "executedActions", result.executedActions.size(),
                    "output", result.output,
                    "timestamp", System.currentTimeMillis()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "success", false,
                    "pattern", patternName,
                    "error", result.error
                ));
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Execution failed: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get all learned patterns with statistics
     * 
     * GET /api/teach/patterns
     * GET /api/teach/patterns?sortBy=frequency|confidence|lastSeen
     */
    @GetMapping("/patterns")
    public ResponseEntity<?> getPatterns(
            @RequestParam(required = false, defaultValue = "frequency") String sortBy) {
        try {
            Map<String, WorkReplicationService.WorkPattern> patterns = workReplication.getLearnedPatterns();
            
            // Convert to list and sort
            List<Map<String, Object>> patternList = patterns.values().stream()
                .map(WorkReplicationService.WorkPattern::toMap)
                .sorted((a, b) -> {
                    switch (sortBy.toLowerCase()) {
                        case "confidence":
                            return ((String) b.get("confidence")).compareTo((String) a.get("confidence"));
                        case "lastseen":
                            return Long.compare((Long) b.get("lastSeen"), (Long) a.get("lastSeen"));
                        case "frequency":
                        default:
                            return Integer.compare((Integer) b.get("frequency"), (Integer) a.get("frequency"));
                    }
                })
                .toList();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "totalPatterns", patterns.size(),
                "patterns", patternList,
                "sortedBy", sortBy,
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to fetch patterns: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get pattern detail with example execution
     * 
     * GET /api/teach/patterns/{patternName}
     */
    @GetMapping("/patterns/{patternName}")
    public ResponseEntity<?> getPattern(@PathVariable String patternName) {
        try {
            Map<String, WorkReplicationService.WorkPattern> patterns = workReplication.getLearnedPatterns();
            WorkReplicationService.WorkPattern pattern = patterns.get(patternName);
            
            if (pattern == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Pattern not found: " + patternName,
                    "availablePatterns", patterns.keySet()
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "pattern", pattern.toMap(),
                "actionSequence", pattern.actions,
                "readyToExecute", pattern.confidence > 0.7
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to fetch pattern: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Get system learning statistics
     * 
     * GET /api/teach/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        try {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "patternsLearned", workReplication.getPatternCount(),
                "timestamp", System.currentTimeMillis(),
                "message", "SupremeAI is learning your work patterns and can now execute them autonomously"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to fetch stats: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Batch record multiple actions (for efficiency)
     * 
     * POST /api/teach/batch
     * {
     *   "actions": [
     *     { "actionType": "...", "context": {...}, "result": "..." },
     *     ...
     *   ]
     * }
     */
    @PostMapping("/batch")
    public ResponseEntity<?> batchRecordActions(@RequestBody Map<String, Object> payload) {
        try {
            List<Map<String, Object>> actions = (List<Map<String, Object>>) payload.get("actions");
            
            if (actions == null || actions.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "No actions provided"
                ));
            }
            
            int recorded = 0;
            for (Map<String, Object> action : actions) {
                String actionType = (String) action.get("actionType");
                Map<String, Object> context = (Map<String, Object>) action.getOrDefault("context", new HashMap<>());
                String result = (String) action.getOrDefault("result", "unknown");
                
                if (actionType != null) {
                    workReplication.recordAction(actionType, context, result);
                    recorded++;
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "recordedActions", recorded,
                "totalProvided", actions.size(),
                "message", "Batch learning recorded",
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to process batch: " + e.getMessage()
            ));
        }
    }
}
