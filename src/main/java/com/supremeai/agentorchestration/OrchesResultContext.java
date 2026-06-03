package com.supremeai.agentorchestration;

import java.util.Date;
import java.util.Map;

public class OrchesResultContext {
    private Map<String, Object> context;
    private Date startedAt;
    private Date completedAt;
    private String status;
    private String mode;
    private Map<String, Object> generationContext;

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
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public Map<String, Object> getGenerationContext() { return generationContext; }
    public void setGenerationContext(Map<String, Object> generationContext) { this.generationContext = generationContext; }
}
