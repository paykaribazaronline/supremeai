package org.example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * AdminChatService — manages three chat modes:
 *
 * 1. SESSION CHAT (ChatGPT-style)
 *    - Admin creates named sessions; each has an ordered message list.
 *    - Persisted to LocalJsonStore (and mirrored to Firebase when available).
 *    - Deleting a session removes it from storage too.
 *
 * 2. PROJECT CHAT
 *    - A session is tied to a projectId.
 *    - When the project status becomes COMPLETE/FAILED the session is auto-deleted.
 *    - Stored under a separate key so project-linked sessions are easy to query.
 *
 * 3. ADMIN RULES
 *    - Admin saves "rules" (text) that the system must follow.
 *    - Rules are loaded by AIAPIService / AgentOrchestrator via getRulesPrompt().
 *    - Rules are persisted in LocalJsonStore.
 */
@Service
public class AdminChatService {

    private static final Logger logger = LoggerFactory.getLogger(AdminChatService.class);

    private static final String SESSIONS_STORE   = "chat-sessions.json";
    private static final String RULES_STORE      = "admin-rules.json";

    @Autowired
    private LocalJsonStoreService store;

    @Autowired(required = false)
    private FirebaseService firebaseService;

    // ── in-memory mirrors (loaded lazily) ────────────────────────────────────
    private final Map<String, ChatSession>  sessions = new ConcurrentHashMap<>();
    private final Map<String, AdminRule>    rules    = new ConcurrentHashMap<>();
    private boolean loaded = false;

    // ═════════════════════════════════════════════════════════════════════════
    // Session management
    // ═════════════════════════════════════════════════════════════════════════

    public ChatSession createSession(String title, String projectId) {
        ensureLoaded();
        ChatSession s = new ChatSession();
        s.id        = UUID.randomUUID().toString();
        s.title     = title != null && !title.isBlank() ? title : "New Chat";
        s.projectId = projectId;
        s.createdAt = LocalDateTime.now().toString();
        s.updatedAt = s.createdAt;
        s.messages  = new ArrayList<>();
        sessions.put(s.id, s);
        persist();
        logger.info("📝 Chat session created: {} (project={})", s.id, projectId);
        return s;
    }

    public List<ChatSession> listSessions(String projectId) {
        ensureLoaded();
        return sessions.values().stream()
            .filter(s -> projectId == null || projectId.equals(s.projectId))
            .sorted(Comparator.comparing((ChatSession s) -> s.updatedAt).reversed())
            .collect(Collectors.toList());
    }

    public Optional<ChatSession> getSession(String id) {
        ensureLoaded();
        return Optional.ofNullable(sessions.get(id));
    }

    public ChatSession addMessage(String sessionId, String sender, String content, String agentId) {
        ensureLoaded();
        ChatSession s = sessions.get(sessionId);
        if (s == null) throw new NoSuchElementException("Session not found: " + sessionId);

        ChatMsg msg = new ChatMsg();
        msg.id      = UUID.randomUUID().toString();
        msg.sender  = sender;        // "user" | "ai"
        msg.content = content;
        msg.agentId = agentId;
        msg.time    = LocalDateTime.now().toString();
        s.messages.add(msg);
        s.updatedAt = msg.time;
        persist();

        // Mirror to Firebase per-project chat (best-effort)
        if (s.projectId != null && firebaseService != null && firebaseService.isInitialized()) {
            try {
                firebaseService.saveChatMessage(s.projectId, sender, content, "admin-chat");
            } catch (Exception e) {
                logger.warn("Firebase chat mirror failed: {}", e.getMessage());
            }
        }
        return s;
    }

    public void deleteSession(String id) {
        ensureLoaded();
        ChatSession removed = sessions.remove(id);
        if (removed == null) throw new NoSuchElementException("Session not found: " + id);
        persist();
        logger.info("🗑️ Chat session deleted: {}", id);
        // Firebase cleanup (best-effort) — remove the project chat node if project-linked
        if (removed.projectId != null && firebaseService != null && firebaseService.isInitialized()) {
            try {
                firebaseService.getDatabase()
                    .getReference("projects").child(removed.projectId).child("chat")
                    .removeValueAsync();
            } catch (Exception e) {
                logger.warn("Firebase project chat cleanup failed: {}", e.getMessage());
            }
        }
    }

    /** Auto-delete all sessions linked to a project (called when project completes/fails). */
    public void deleteSessionsByProject(String projectId) {
        ensureLoaded();
        List<String> toRemove = sessions.values().stream()
            .filter(s -> projectId.equals(s.projectId))
            .map(s -> s.id)
            .collect(Collectors.toList());
        toRemove.forEach(sessions::remove);
        if (!toRemove.isEmpty()) {
            persist();
            logger.info("🗑️ Auto-deleted {} session(s) for completed project {}", toRemove.size(), projectId);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Admin Rules
    // ═════════════════════════════════════════════════════════════════════════

    public AdminRule createRule(String title, String ruleText, String category) {
        ensureLoaded();
        AdminRule r = new AdminRule();
        r.id        = UUID.randomUUID().toString();
        r.title     = title != null ? title : "Rule " + (rules.size() + 1);
        r.ruleText  = ruleText;
        r.category  = category != null ? category : "general";
        r.enabled   = true;
        r.createdAt = LocalDateTime.now().toString();
        rules.put(r.id, r);
        persistRules();
        logger.info("📜 Admin rule created: {} — {}", r.id, r.title);
        return r;
    }

    public List<AdminRule> listRules(boolean enabledOnly) {
        ensureLoaded();
        return rules.values().stream()
            .filter(r -> !enabledOnly || r.enabled)
            .sorted(Comparator.comparing((AdminRule r) -> r.createdAt).reversed())
            .collect(Collectors.toList());
    }

    public Optional<AdminRule> getRule(String id) {
        ensureLoaded();
        return Optional.ofNullable(rules.get(id));
    }

    public AdminRule updateRule(String id, String title, String ruleText, String category, Boolean enabled) {
        ensureLoaded();
        AdminRule r = rules.get(id);
        if (r == null) throw new NoSuchElementException("Rule not found: " + id);
        if (title     != null) r.title     = title;
        if (ruleText  != null) r.ruleText  = ruleText;
        if (category  != null) r.category  = category;
        if (enabled   != null) r.enabled   = enabled;
        r.updatedAt = LocalDateTime.now().toString();
        persistRules();
        return r;
    }

    public void deleteRule(String id) {
        ensureLoaded();
        if (rules.remove(id) == null) throw new NoSuchElementException("Rule not found: " + id);
        persistRules();
        logger.info("🗑️ Admin rule deleted: {}", id);
    }

    /**
     * Returns a combined system-prompt prefix built from all enabled rules.
     * Injected into every AI call so the model follows admin-set rules.
     */
    public String getRulesPrompt() {
        ensureLoaded();
        List<AdminRule> enabled = listRules(true);
        if (enabled.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(
            "## System Rules (set by admin — always follow these):\n");
        for (int i = 0; i < enabled.size(); i++) {
            AdminRule r = enabled.get(i);
            sb.append((i + 1)).append(". [").append(r.category).append("] ")
              .append(r.title).append(": ").append(r.ruleText).append("\n");
        }
        sb.append("\n---\n\n");
        return sb.toString();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Persistence
    // ═════════════════════════════════════════════════════════════════════════

    private synchronized void ensureLoaded() {
        if (loaded) return;
        try {
            List<ChatSession> ss = store.read(SESSIONS_STORE,
                new TypeReference<List<ChatSession>>() {}, new ArrayList<>());
            ss.forEach(s -> sessions.put(s.id, s));

            List<AdminRule> rs = store.read(RULES_STORE,
                new TypeReference<List<AdminRule>>() {}, new ArrayList<>());
            rs.forEach(r -> rules.put(r.id, r));
        } catch (Exception e) {
            logger.warn("Failed to load chat data: {}", e.getMessage());
        }
        loaded = true;
    }

    private void persist() {
        try {
            store.write(SESSIONS_STORE, new ArrayList<>(sessions.values()));
        } catch (Exception e) {
            logger.error("Failed to persist chat sessions: {}", e.getMessage());
        }
    }

    private void persistRules() {
        try {
            store.write(RULES_STORE, new ArrayList<>(rules.values()));
        } catch (Exception e) {
            logger.error("Failed to persist admin rules: {}", e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Data model
    // ═════════════════════════════════════════════════════════════════════════

    public static class ChatMsg {
        public String id;
        public String sender;   // "user" | "ai"
        public String content;
        public String agentId;
        public String time;
    }

    public static class ChatSession {
        public String         id;
        public String         title;
        public String         projectId;   // null = general session
        public String         createdAt;
        public String         updatedAt;
        public List<ChatMsg>  messages = new ArrayList<>();
    }

    public static class AdminRule {
        public String  id;
        public String  title;
        public String  ruleText;
        public String  category;   // general | safety | formatting | behaviour
        public boolean enabled = true;
        public String  createdAt;
        public String  updatedAt;
    }
}
