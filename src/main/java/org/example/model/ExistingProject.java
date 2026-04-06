package org.example.model;

import java.util.*;

/**
 * ExistingProject — a GitHub repository registered by admin for continuous AI improvement.
 *
 * Lifecycle:
 *  1. Admin registers the repo (repoUrl, branch, optional token, improvementGoal).
 *  2. Admin discusses the plan with SupremeAI (chat-like conversation stored here).
 *  3. System clones / pulls the repo, analyses it, applies AI-suggested fixes/enhancements.
 *  4. If continuousImprovement=true the cycle repeats on each idle check.
 */
public class ExistingProject {

    private String id = UUID.randomUUID().toString();

    /** Human-readable project name (e.g. "Guitar App") */
    private String name;

    /** Full GitHub (or any git) HTTPS URL, e.g. https://github.com/user/guitar-app */
    private String repoUrl;

    /** Branch to work on, e.g. main / develop / feat-xyz */
    private String branch = "main";

    /** Optional personal-access token for private repos (stored in memory only, never logged) */
    private String repoToken;

    /** Admin's high-level improvement goal, e.g. "Fix audio bug and improve tab editor UX" */
    private String improvementGoal;

    /**
     * Conversation log between admin and SupremeAI about this project.
     * Each entry: { "role": "admin"|"ai", "message": "...", "timestamp": <ms> }
     */
    private List<Map<String, Object>> conversation = new ArrayList<>();

    /**
     * Log of improvement cycles applied by the system.
     * Each entry: { "summary": "...", "filesChanged": [...], "appliedAt": <ms>, "status": "ok"|"failed" }
     */
    private List<Map<String, Object>> improvementHistory = new ArrayList<>();

    /** REGISTERED → ANALYSING → IMPROVING → IDLE → ERROR */
    private String status = "REGISTERED";

    /** When true, system automatically improves the project during idle time */
    private boolean continuousImprovement = true;

    private long createdAt = System.currentTimeMillis();
    private long lastImprovedAt = 0;
    private long lastAnalysedAt = 0;

    /** Latest AI-generated analysis / improvement summary */
    private String latestAnalysis;

    /** Number of successful improvement cycles */
    private int totalImprovementsApplied = 0;

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRepoUrl() { return repoUrl; }
    public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getRepoToken() { return repoToken; }
    public void setRepoToken(String repoToken) { this.repoToken = repoToken; }

    public String getImprovementGoal() { return improvementGoal; }
    public void setImprovementGoal(String improvementGoal) { this.improvementGoal = improvementGoal; }

    public List<Map<String, Object>> getConversation() { return conversation; }
    public void setConversation(List<Map<String, Object>> conversation) { this.conversation = conversation; }

    public List<Map<String, Object>> getImprovementHistory() { return improvementHistory; }
    public void setImprovementHistory(List<Map<String, Object>> improvementHistory) { this.improvementHistory = improvementHistory; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isContinuousImprovement() { return continuousImprovement; }
    public void setContinuousImprovement(boolean continuousImprovement) { this.continuousImprovement = continuousImprovement; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getLastImprovedAt() { return lastImprovedAt; }
    public void setLastImprovedAt(long lastImprovedAt) { this.lastImprovedAt = lastImprovedAt; }

    public long getLastAnalysedAt() { return lastAnalysedAt; }
    public void setLastAnalysedAt(long lastAnalysedAt) { this.lastAnalysedAt = lastAnalysedAt; }

    public String getLatestAnalysis() { return latestAnalysis; }
    public void setLatestAnalysis(String latestAnalysis) { this.latestAnalysis = latestAnalysis; }

    public int getTotalImprovementsApplied() { return totalImprovementsApplied; }
    public void setTotalImprovementsApplied(int totalImprovementsApplied) { this.totalImprovementsApplied = totalImprovementsApplied; }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    public void addConversationMessage(String role, String message) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("role", role);
        entry.put("message", message);
        entry.put("timestamp", System.currentTimeMillis());
        conversation.add(entry);
    }

    public void addImprovementRecord(String summary, List<String> filesChanged, boolean success) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("summary", summary);
        record.put("filesChanged", filesChanged != null ? filesChanged : List.of());
        record.put("status", success ? "ok" : "failed");
        record.put("appliedAt", System.currentTimeMillis());
        improvementHistory.add(record);
        if (success) {
            totalImprovementsApplied++;
            lastImprovedAt = System.currentTimeMillis();
        }
    }

    /**
     * Return a safe summary map (token excluded for security).
     */
    public Map<String, Object> toSummaryMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("repoUrl", repoUrl);
        m.put("branch", branch);
        m.put("hasToken", repoToken != null && !repoToken.isBlank());
        m.put("improvementGoal", improvementGoal);
        m.put("status", status);
        m.put("continuousImprovement", continuousImprovement);
        m.put("createdAt", createdAt);
        m.put("lastImprovedAt", lastImprovedAt);
        m.put("lastAnalysedAt", lastAnalysedAt);
        m.put("latestAnalysis", latestAnalysis);
        m.put("totalImprovementsApplied", totalImprovementsApplied);
        m.put("conversationLength", conversation.size());
        m.put("improvementHistoryLength", improvementHistory.size());
        return m;
    }
}
