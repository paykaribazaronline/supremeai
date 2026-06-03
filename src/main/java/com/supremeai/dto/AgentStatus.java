package com.supremeai.dto;

public class AgentStatus {
    private String agentId;
    private String status;
    private long lastUpdated;

    public AgentStatus() {}

    public AgentStatus(String agentId, String status, long lastUpdated) {
        this.agentId = agentId;
        this.status = status;
        this.lastUpdated = lastUpdated;
    }

    public static AgentStatusBuilder builder() {
        return new AgentStatusBuilder();
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public static class AgentStatusBuilder {
        private String agentId;
        private String status;
        private long lastUpdated;

        public AgentStatusBuilder agentId(String agentId) {
            this.agentId = agentId;
            return this;
        }

        public AgentStatusBuilder status(String status) {
            this.status = status;
            return this;
        }

        public AgentStatusBuilder lastUpdated(long lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public AgentStatus build() {
            return new AgentStatus(agentId, status, lastUpdated);
        }
    }
}
