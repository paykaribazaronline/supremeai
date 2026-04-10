package org.example.model;

/**
 * SupremeAI 3-Mode System
 * 
 * Controls how the system behaves:
 * - FULLY_AUTOMATIC: System decides everything (acts as developer/admin)
 * - PRESET_RULES: System follows admin-defined rules (obedient mode)
 * - MANUAL_ONLY: System only works when explicitly commanded by admin
 */
public enum SystemMode {
    /**
     * Mode 1: FULLY_AUTOMATIC
     * - System autonomously decides what to change
     * - Auto-learns and generates APIs
     * - Self-improves without asking
     * - Acts like a developer/admin
     * - Maximum productivity, moderate risk
     */
    FULLY_AUTOMATIC("Fully Automatic", 
                   "System acts as developer: decides what to change, what to learn, which APIs to add",
                   "⚡ FULL AUTONOMY"),
    
    /**
     * Mode 2: PRESET_RULES
     * - System follows admin-defined rules strictly
     * - Executes only within configured boundaries
     * - Obedient, predictable behavior
     * - Safe, rule-compliant execution
     * - Balance between autonomy and control
     */
    PRESET_RULES("Preset Rules", 
                "System follows admin-defined rules only (obedient mode)",
                "🎯 RULE-BOUND"),
    
    /**
     * Mode 3: MANUAL_ONLY
     * - System only executes when admin explicitly commands
     * - Lazy mode: waits for instructions
     * - Zero autonomy, maximum control
     * - Safest mode for sensitive operations
     * - Slowest, most controlled
     */
    MANUAL_ONLY("Manual Only", 
               "System only works when admin explicitly commands (lazy person mode)",
               "🛑 MANUAL CONTROL");

    private final String displayName;
    private final String description;
    private final String icon;

    SystemMode(String displayName, String description, String icon) {
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getIcon() {
        return icon;
    }

    /**
     * Check if system can act autonomously in this mode
     */
    public boolean isAutonomous() {
        return this == FULLY_AUTOMATIC || this == PRESET_RULES;
    }

    /**
     * Check if system requires explicit admin command
     */
    public boolean isManualOnly() {
        return this == MANUAL_ONLY;
    }

    /**
     * Check if system follows preset rules
     */
    public boolean isRuleBased() {
        return this == PRESET_RULES || this == FULLY_AUTOMATIC;
    }
}
