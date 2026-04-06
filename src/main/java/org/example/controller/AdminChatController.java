package org.example.controller;

import org.example.service.AdminChatService;
import org.example.service.AdminChatService.*;
import org.example.api.ChatController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * REST API for the upgraded admin chat system.
 *
 * Session Chat (ChatGPT-style):
 *   POST   /api/admin-chat/sessions                      — create session
 *   GET    /api/admin-chat/sessions                      — list sessions
 *   GET    /api/admin-chat/sessions/{id}                 — get session + messages
 *   POST   /api/admin-chat/sessions/{id}/send            — send message (AI replies)
 *   DELETE /api/admin-chat/sessions/{id}                 — delete session + DB
 *
 * Project Chat:
 *   GET    /api/admin-chat/sessions?projectId=X          — sessions for project
 *   POST   /api/admin-chat/project/{projectId}/session   — create project session
 *   DELETE /api/admin-chat/project/{projectId}/sessions  — delete all project sessions
 *
 * Admin Rules:
 *   GET    /api/admin-chat/rules                         — list rules
 *   POST   /api/admin-chat/rules                         — create rule
 *   PATCH  /api/admin-chat/rules/{id}                    — update rule
 *   DELETE /api/admin-chat/rules/{id}                    — delete rule
 */
@RestController
@RequestMapping("/api/admin-chat")
@CrossOrigin(origins = "*")
public class AdminChatController {

    @Autowired
    private AdminChatService chatService;

    @Autowired
    private ChatController legacyChatController;

    // ── Sessions ─────────────────────────────────────────────────────────────

    @PostMapping("/sessions")
    public ResponseEntity<?> createSession(@RequestBody Map<String, String> body) {
        try {
            ChatSession s = chatService.createSession(
                body.get("title"), body.get("projectId"));
            return ResponseEntity.ok(s);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/sessions")
    public ResponseEntity<?> listSessions(
            @RequestParam(required = false) String projectId) {
        try {
            return ResponseEntity.ok(Map.of(
                "sessions", chatService.listSessions(projectId)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<?> getSession(@PathVariable String id) {
        return chatService.getSession(id)
            .<ResponseEntity<?>>map(ResponseEntity::ok)
            .orElse(ResponseEntity.status(404).body(Map.of("error", "Session not found")));
    }

    /**
     * Send a message in a session — calls the existing ChatController AI logic,
     * then persists both sides in the session.
     */
    @PostMapping("/sessions/{id}/send")
    public ResponseEntity<?> sendMessage(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        try {
            if (chatService.getSession(id).isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Session not found"));
            }
            String userMessage = (String) body.getOrDefault("userMessage", "");
            String taskType    = (String) body.getOrDefault("taskType", "general");
            if (userMessage.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Empty message"));
            }

            // Persist user message
            chatService.addMessage(id, "user", userMessage, null);

            // Delegate to legacy ChatController for AI response
            Map<String, Object> req = new HashMap<>(body);
            req.put("userMessage", userMessage);
            req.put("taskType", taskType);
            req.put("metadata", body.getOrDefault("metadata", Map.of()));
            Map<String, Object> aiResp = legacyChatController.sendMessage(req);

            String aiContent = (String) aiResp.getOrDefault("content",
                aiResp.getOrDefault("message", ""));
            String agentId   = (String) aiResp.getOrDefault("agentId", "SupremeAI");

            // Persist AI reply
            chatService.addMessage(id, "ai", aiContent, agentId);

            // Return full updated session
            return chatService.getSession(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(aiResp));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<?> deleteSession(@PathVariable String id) {
        try {
            chatService.deleteSession(id);
            return ResponseEntity.ok(Map.of("success", true, "deletedId", id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── Project chat helpers ──────────────────────────────────────────────────

    @PostMapping("/project/{projectId}/session")
    public ResponseEntity<?> createProjectSession(
            @PathVariable String projectId,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String title = body != null ? body.get("title") : null;
            ChatSession s = chatService.createSession(
                title != null ? title : "Project Chat", projectId);
            return ResponseEntity.ok(s);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/project/{projectId}/sessions")
    public ResponseEntity<?> deleteProjectSessions(@PathVariable String projectId) {
        try {
            chatService.deleteSessionsByProject(projectId);
            return ResponseEntity.ok(Map.of("success", true, "projectId", projectId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── Admin Rules ───────────────────────────────────────────────────────────

    @GetMapping("/rules")
    public ResponseEntity<?> listRules(
            @RequestParam(defaultValue = "false") boolean enabledOnly) {
        try {
            return ResponseEntity.ok(Map.of("rules", chatService.listRules(enabledOnly)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/rules")
    public ResponseEntity<?> createRule(@RequestBody Map<String, String> body) {
        try {
            AdminRule r = chatService.createRule(
                body.get("title"), body.get("ruleText"), body.get("category"));
            return ResponseEntity.ok(r);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/rules/{id}")
    public ResponseEntity<?> updateRule(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        try {
            Boolean enabled = body.containsKey("enabled")
                ? (Boolean) body.get("enabled") : null;
            AdminRule r = chatService.updateRule(
                id,
                (String) body.get("title"),
                (String) body.get("ruleText"),
                (String) body.get("category"),
                enabled);
            return ResponseEntity.ok(r);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<?> deleteRule(@PathVariable String id) {
        try {
            chatService.deleteRule(id);
            return ResponseEntity.ok(Map.of("success", true, "deletedId", id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /** Return combined rules prompt (for diagnostics / preview). */
    @GetMapping("/rules/prompt")
    public ResponseEntity<?> getRulesPrompt() {
        try {
            return ResponseEntity.ok(Map.of("prompt", chatService.getRulesPrompt()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
