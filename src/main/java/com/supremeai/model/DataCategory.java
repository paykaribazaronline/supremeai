package com.supremeai.model;

/**
 * Data Category Classification System
 * 
 * Every piece of stored data is explicitly categorized with clear retention policies
 */
public enum DataCategory {

    // ==============================================
    // 🟢 ADMIN CREATED - PERMANENT
    // ==============================================
    /**
     * Admin created system configuration, rules, and settings
     * PERMANENT - NEVER DELETED
     */
    ADMIN_RULES(true, -1),
    
    /**
     * Admin configured AI providers and API keys
     * PERMANENT - NEVER DELETED
     */
    ADMIN_PROVIDERS(true, -1),
    
    /**
     * Admin user management and tier assignments
     * PERMANENT - NEVER DELETED
     */
    ADMIN_USER_MANAGEMENT(true, -1),

    // ==============================================
    // 🔵 SYSTEM GENERATED - PERMANENT KNOWLEDGE
    // ==============================================
    /**
     * Machine learning training data and system learning
     * PERMANENT - This is the system's brain
     */
    SYSTEM_LEARNING(true, -1),
    
    /**
     * Security audit logs and admin actions
     * PERMANENT for compliance
     */
    SYSTEM_AUDIT_LOG(true, -1),

    // ==============================================
    // 🟡 USER CREATED - USER OWNED
    // ==============================================
    /**
     * User account data
     * Retained until user account is deleted
     */
    USER_ACCOUNT(true, -1),
    
    /**
     * User created projects and work
     * Retained until user deletes them
     */
    USER_PROJECTS(true, -1),
    
    /**
     * User provided API keys and credentials
     * Retained until user deletes them
     */
    USER_CREDENTIALS(true, -1),

    // ==============================================
    // 🟠 USER INTERACTION - RETAINED
    // ==============================================
    /**
     * Chat with AI conversations
     * Retained for 30 days unless user saves
     */
    USER_CHAT(false, 30),
    
    /**
     * User activity history
     * Retained for 90 days
     */
    USER_ACTIVITY(false, 90),

    // ==============================================
    // 🔴 TEMPORARY OPERATIONAL DATA
    // ==============================================
    /**
     * Guest user sessions and unauthenticated usage
     * Auto deleted after 24 hours
     */
    GUEST_SESSION(false, 1),
    
    /**
     * Running agent instances and status
     * Cleaned up when stopped
     */
    AGENT_RUNTIME(false, 7),
    
    /**
     * Active VPN connections
     * Cleaned up when disconnected
     */
    VPN_SESSION(false, 1),
    
    /**
     * Health checks, metrics, performance data
     * Auto deleted after 30 days
     */
    SYSTEM_METRICS(false, 30);

    private final boolean isPermanent;
    private final int retentionDays; // -1 = forever

    DataCategory(boolean isPermanent, int retentionDays) {
        this.isPermanent = isPermanent;
        this.retentionDays = retentionDays;
    }

    public boolean isPermanent() {
        return isPermanent;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public boolean shouldAutoDelete() {
        return !isPermanent && retentionDays > 0;
    }
}
