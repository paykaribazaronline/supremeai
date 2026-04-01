package org.example.selfhealing;

/**
 * Minimal AutoRecoveryHandler for SelfHealing
 */
public interface AutoRecoveryHandler {
    void recover();
    String getServiceName();
    boolean isHealthy();
}
