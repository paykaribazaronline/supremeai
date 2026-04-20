package com.supremeai.agentorchestration;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of orchestration - contains full context for code generation.
 */
public class OrchesResultContext {
    private Map<String, Object> context;
    private Date startedAt;
    private Date completedAt;
    private String status;

    public OrchesResultContext() {}

    public OrchesResultContext(Map<String, Object> context) {
        this.context = context;
    }

    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }
    public Date getStartedAt() { return startedAt; }
    public void setStartedAt(Date startedAt) { this.startedAt = startedAt; }
    public Date getCompletedAt() { return completedAt; }
    public void setCompletedAt(Date completedAt) { this.completedAt = completedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // Helper methods
    @SuppressWarnings("unchecked")
    public String getDecision(String key) {
        var decisions = (List<VotingDecision>) context.get("decisions");
        if (decisions != null) {
            for (VotingDecision d : decisions) {
                if (d.getDecisionKey().equals(key)) {
                    return d.getAiConsensus();
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getGenerationContext() {
        return (Map<String, Object>) context.get("generationContext");
    }
}