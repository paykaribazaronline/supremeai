package org.example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.PreDestroy;
import org.example.model.AdminSuggestion;
import org.example.model.ConsensusVote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * AdminSuggestionService — stores admin tab suggestions and optionally processes
 * them immediately via AI consensus.
 *
 * Save:   stores the suggestion with status PENDING.
 * Apply:  routes suggestion through MultiAIConsensusService so the AI understands
 *         what change is needed on that tab and returns a structured action plan.
 */
@Service
public class AdminSuggestionService {

    private static final Logger logger = LoggerFactory.getLogger(AdminSuggestionService.class);
    private static final String STORE_KEY = "admin-suggestions.json";

    private final Map<String, AdminSuggestion> suggestions = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean loaded = false;

    @Autowired
    private LocalJsonStoreService store;

    @Autowired(required = false)
    private MultiAIConsensusService consensusService;

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Save a suggestion. If applyNow=true, processing is triggered asynchronously
     * and the suggestion is returned immediately with status PROCESSING.
     */
    public AdminSuggestion submit(AdminSuggestion suggestion) {
        ensureLoaded();
        suggestion.setId(UUID.randomUUID().toString());
        if (suggestion.getStatus() == null) {
            suggestion.setStatus(AdminSuggestion.Status.PENDING);
        }

        if (suggestion.isApplyNow()) {
            suggestion.setStatus(AdminSuggestion.Status.PROCESSING);
            suggestions.put(suggestion.getId(), suggestion);
            persist();
            String id = suggestion.getId();
            executor.submit(() -> applyAsync(id));
        } else {
            suggestions.put(suggestion.getId(), suggestion);
            persist();
        }

        logger.info("📝 Admin suggestion submitted [tab={}, applyNow={}]",
                suggestion.getTabKey(), suggestion.isApplyNow());
        return suggestion;
    }

    /** Return all suggestions, newest first. */
    public List<AdminSuggestion> listAll() {
        ensureLoaded();
        return suggestions.values().stream()
                .sorted(Comparator.comparingLong(AdminSuggestion::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    /** Return suggestions for a specific tab. */
    public List<AdminSuggestion> listByTab(String tabKey) {
        ensureLoaded();
        return suggestions.values().stream()
                .filter(s -> tabKey.equals(s.getTabKey()))
                .sorted(Comparator.comparingLong(AdminSuggestion::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    /** Manually trigger application of a saved (PENDING) suggestion. */
    public AdminSuggestion applyNow(String id) {
        ensureLoaded();
        AdminSuggestion s = suggestions.get(id);
        if (s == null) throw new NoSuchElementException("Suggestion not found: " + id);
        if (s.getStatus() == AdminSuggestion.Status.APPLIED) return s;
        s.setStatus(AdminSuggestion.Status.PROCESSING);
        persist();
        executor.submit(() -> applyAsync(id));
        return s;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }

    private void applyAsync(String id) {
        AdminSuggestion s = suggestions.get(id);
        if (s == null) return;
        try {
            String prompt = buildPrompt(s);
            String aiResult;
            if (consensusService != null) {
                ConsensusVote vote = consensusService.askAllAISystemLevel(prompt);
                aiResult = vote.getWinningResponse() != null
                        ? vote.getWinningResponse()
                        : "AI processed suggestion — no consensus reached.";
            } else {
                aiResult = "AI service unavailable. Suggestion saved for manual review.";
            }
            s.setAiResponse(aiResult);
            s.setStatus(AdminSuggestion.Status.APPLIED);
            s.setAppliedAt(System.currentTimeMillis());
            logger.info("✅ Admin suggestion applied [id={}, tab={}]", id, s.getTabKey());
        } catch (Exception e) {
            logger.error("❌ Failed to apply admin suggestion [id={}]: {}", id, e.getMessage());
            s.setStatus(AdminSuggestion.Status.FAILED);
            s.setAiResponse("Processing failed: " + e.getMessage());
        }
        persist();
    }

    private String buildPrompt(AdminSuggestion s) {
        return String.format(
                "You are the SupremeAI system. An admin has submitted the following suggestion for the '%s' tab of the admin panel:\n\n" +
                "\"%s\"\n\n" +
                "Analyse the suggestion and provide:\n" +
                "1. A clear action plan describing exactly what changes should be made.\n" +
                "2. Which backend service/config/endpoint to update.\n" +
                "3. The expected outcome after the change.\n\n" +
                "Be concise and actionable.",
                s.getTabLabel() != null ? s.getTabLabel() : s.getTabKey(),
                s.getSuggestion()
        );
    }

    private void ensureLoaded() {
        if (!loaded) {
            List<AdminSuggestion> saved = store.read(
                    STORE_KEY,
                    new TypeReference<List<AdminSuggestion>>() {},
                    Collections.emptyList()
            );
            saved.forEach(s -> suggestions.put(s.getId(), s));
            loaded = true;
        }
    }

    private void persist() {
        store.write(STORE_KEY, new ArrayList<>(suggestions.values()));
    }
}
