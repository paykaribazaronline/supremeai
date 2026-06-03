package com.supremeai.event;

public class AgentStatusEvent {
    private String agentId;
    private String newStatus;

    public AgentStatusEvent(String agentId, String newStatus) {
        this.agentId = agentId;
        this.newStatus = newStatus;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }
}
