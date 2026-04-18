package com.supremeai.command;

/**
 * Command category enum
 */
public enum CommandCategory {
    MONITORING,      // Health checks, metrics
    DATA_REFRESH,    // Fetch from external sources
    PROVIDER,        // AI account management
    OPTIMIZATION,    // Auto-healing, quota adjustment
    DEPLOYMENT,      // Trigger deployments
    CONFIGURATION,   // System settings
    MAINTENANCE      // Cleanup, archival
}
