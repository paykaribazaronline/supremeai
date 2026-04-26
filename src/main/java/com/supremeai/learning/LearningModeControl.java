package com.supremeai.learning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * LearningModeControl - Global learning intensity control.
 *
 * Admin can set the learning mode to control how aggressively the system
 * acquires and integrates new knowledge.
 *
 * Modes:
 * - AGGRESSIVE: Scrape all sources frequently, auto-approve low-risk learnings, minimal human review
 * - BALANCED:   Default. Scrape scheduled sources, require admin approval for new learnings
 * - MANUAL:     No automatic learning. Only explicit admin-triggered actions allowed
 * - PAUSED:     Completely disable all learning activities (emergency stop)
 */
@Service
public class LearningModeControl {

    private static final Logger log = LoggerFactory.getLogger(LearningModeControl.class);

    public enum LearningMode {
        AGGRESSIVE("Aggressive - Fast learning, auto-approve low-risk content"),
        BALANCED("Balanced - Scheduled learning, admin approval required"),
        MANUAL("Manual - Only admin-triggered learning"),
        PAUSED("Paused - All learning disabled");

        private final String description;

        LearningMode(String description) {
            this.description = description;
        }

        public String getDescription() { return description; }
    }

    // Current mode, thread-safe with volatile
    private volatile LearningMode currentMode = LearningMode.BALANCED;

    // Emergency pause override (takes precedence over mode)
    private volatile boolean emergencyPause = false;

    /**
     * Get current learning mode.
     */
    public LearningMode getCurrentMode() {
        return emergencyPause ? LearningMode.PAUSED : currentMode;
    }

    /**
     * Set learning mode (admin endpoint).
     */
    public void setMode(LearningMode mode) {
        this.currentMode = mode;
        log.info("[LearningModeControl] Mode changed to: {}", mode);
    }

    /**
     * Check if scraping is allowed under current mode.
     */
    public boolean isScrapingAllowed() {
        LearningMode effective = getCurrentMode();
        return effective == LearningMode.AGGRESSIVE || effective == LearningMode.BALANCED;
    }

    /**
     * Check if auto-approval is allowed (for low-risk items).
     */
    public boolean isAutoApprovalAllowed() {
        return getCurrentMode() == LearningMode.AGGRESSIVE;
    }

    /**
     * Check if any learning is permitted at all.
     */
    public boolean isLearningAllowed() {
        return getCurrentMode() != LearningMode.PAUSED && getCurrentMode() != LearningMode.MANUAL;
    }

    /**
     * Emergency pause all learning immediately.
     */
    public void emergencyPause() {
        this.emergencyPause = true;
        log.error("[LearningModeControl] EMERGENCY PAUSE activated. All learning halted.");
    }

    /**
     * Resume from emergency pause (returns to previous mode).
     */
    public void resumeFromPause() {
        this.emergencyPause = false;
        log.warn("[LearningModeControl] Emergency pause lifted. Resuming mode: {}", currentMode);
    }

    /**
     * Check if emergency pause is active.
     */
    public boolean isEmergencyPaused() {
        return emergencyPause;
    }

    /**
     * Trigger learning manually (for MANUAL mode only).
     * Returns true if manual trigger is allowed, false otherwise.
     */
    public boolean allowManualTrigger() {
        return getCurrentMode() == LearningMode.MANUAL || getCurrentMode() == LearningMode.BALANCED;
    }
}
