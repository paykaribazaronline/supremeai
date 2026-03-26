package org.example.model;

import java.util.List;

public class SystemConfig {
    private int agentCount;
    private double consensusThreshold;
    private boolean rotationEnabled;
    private boolean vpnEnabled;
    private AIPool aiPool;

    // Getters and Setters
    public int getAgentCount() { return agentCount; }
    public void setAgentCount(int agentCount) { this.agentCount = agentCount; }

    public double getConsensusThreshold() { return consensusThreshold; }
    public void setConsensusThreshold(double consensusThreshold) { this.consensusThreshold = consensusThreshold; }

    public boolean isRotationEnabled() { return rotationEnabled; }
    public void setRotationEnabled(boolean rotationEnabled) { this.rotationEnabled = rotationEnabled; }

    public boolean isVpnEnabled() { return vpnEnabled; }
    public void setVpnEnabled(boolean vpnEnabled) { this.vpnEnabled = vpnEnabled; }

    public AIPool getAiPool() { return aiPool; }
    public void setAiPool(AIPool aiPool) { this.aiPool = aiPool; }

    public static class AIPool {
        private List<String> top10;
        private List<String> safezone;

        public List<String> getTop10() { return top10; }
        public void setTop10(List<String> top10) { this.top10 = top10; }

        public List<String> getSafezone() { return safezone; }
        public void setSafezone(List<String> safezone) { this.safezone = safezone; }
    }
}
