package org.example.kimik2.learning;

import com.google.firebase.database.*;
import org.example.service.FirebaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.*;

/**
 * Firebase persistence layer for ALL levels of SupremeAI's learning system.
 *
 * Path layout in Firebase Realtime Database:
 *   learning/
 *     agent_profiles/{agentName}          ← Level 2: pattern profiles
 *     reasoning_chains/{agent}/{task}/... ← Level 3: successful chains
 *     routing_weights/{agent}/{task}      ← Level 1: MoE routing weights
 *     knowledge_seed/
 *       ai_providers/{provider}           ← base knowledge for admin-configured external AIs
 *       moe_agents/{agent}                ← base knowledge for 20 internal agents
 *       metadata                          ← seed version + timestamp
 *
 * This covers ALL AI model knowledge — not just Kimi K2 internals.
 * External providers (OpenAI, Anthropic, etc.) and internal MoE agents
 * are treated uniformly: every "agent" that touches a task gets profiled.
 *
 * Design:
 *  - Writes are fire-and-forget (setValueAsync / updateChildrenAsync)
 *  - Reads have 5-second timeout with HashMap fallback (never blocks startup)
 *  - Firebase unavailability is silent — in-memory maps still work
 */
@Repository
public class LearningFirebaseRepository {

    private static final Logger logger = LoggerFactory.getLogger(LearningFirebaseRepository.class);
    static final String BASE = "learning";
    private static final int TIMEOUT_SEC = 5;

    @Autowired
    private FirebaseService firebase;

    public boolean isAvailable() {
        return firebase.isInitialized();
    }

    // ── Level 2: Agent Profiles ───────────────────────────────────────────────

    /**
     * Save a fully built agent profile to Firebase.
     * Called from AgentPatternProfiler after buildAllProfiles().
     */
    public void saveProfile(String agentName, Map<String, Object> profile) {
        write(BASE + "/agent_profiles/" + key(agentName), profile);
    }

    /** Load all stored profiles on startup (warm-start on restart). */
    public Map<String, Object> loadAllProfiles() {
        return read(BASE + "/agent_profiles");
    }

    // ── Level 3: Reasoning Chains ─────────────────────────────────────────────

    /**
     * Save a single reasoning chain.
     * Path: learning/reasoning_chains/{agent}/{taskType}/{chainId}
     */
    public void saveChain(String agentName, String taskType, String chainId,
                          Map<String, Object> chainData) {
        write(BASE + "/reasoning_chains/"
            + key(agentName) + "/" + key(taskType) + "/" + key(chainId), chainData);
    }

    /** Load all stored chains on startup. */
    public Map<String, Object> loadAllChains() {
        return read(BASE + "/reasoning_chains");
    }

    // ── Level 1: Routing Weights ──────────────────────────────────────────────

    /**
     * Persist updated routing weight (after RLVR step).
     * Path: learning/routing_weights/{agent}/{taskType}
     */
    public void saveRoutingWeight(String agentName, String taskType, double weight) {
        write(BASE + "/routing_weights/" + key(agentName) + "/" + key(taskType), weight);
    }

    /** Load persisted routing weights (warm routing on restart). */
    public Map<String, Object> loadRoutingWeights() {
        return read(BASE + "/routing_weights");
    }

    // ── Generic seed (used by KnowledgeSeedService) ───────────────────────────

    /**
     * Seed structured data at any path.
     * Uses updateChildren — safe to run multiple times (idempotent within a key).
     */
    public void seed(String path, Map<String, Object> data) {
        if (!isAvailable()) {
            logger.debug("Firebase not initialized — skipping seed at: {}", path);
            return;
        }
        try {
            firebase.getDatabase().getReference(path).updateChildrenAsync(data);
            logger.info("🌱 Seeded Firebase → {}", path);
        } catch (Exception e) {
            logger.warn("Firebase seed failed ({}): {}", path, e.getMessage());
        }
    }

    /** Set a single value (overwrites). */
    public void set(String path, Object value) {
        write(path, value);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private void write(String path, Object value) {
        if (!isAvailable()) return;
        try {
            firebase.getDatabase().getReference(path).setValueAsync(value);
        } catch (Exception e) {
            logger.debug("Firebase write failed ({}): {}", path, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> read(String path) {
        if (!isAvailable()) return new HashMap<>();
        try {
            CompletableFuture<DataSnapshot> future = new CompletableFuture<>();
            firebase.getDatabase().getReference(path)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot s) { future.complete(s); }
                    @Override public void onCancelled(DatabaseError e) {
                        future.completeExceptionally(e.toException());
                    }
                });
            DataSnapshot snap = future.get(TIMEOUT_SEC, TimeUnit.SECONDS);
            if (!snap.exists()) return new HashMap<>();
            Object val = snap.getValue();
            return val instanceof Map ? (Map<String, Object>) val : new HashMap<>();
        } catch (TimeoutException e) {
            logger.warn("Firebase read timed out at path: {}", path);
            return new HashMap<>();
        } catch (Exception e) {
            logger.debug("Firebase read failed ({}): {}", path, e.getMessage());
            return new HashMap<>();
        }
    }

    /** Sanitize key: Firebase disallows . # $ [ ] / in path segments. */
    static String key(String s) {
        if (s == null) return "unknown";
        return s.replaceAll("[.#$\\[\\]/]", "_");
    }
}
