package com.supremeai.intelligence.healing;

import com.supremeai.service.SelfHealingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * The Infinite Auto-Healing Loop!
 * Now delegates to the unified SelfHealingService.
 * Ensures nothing is considered "Done" until it actually compiles and passes tests in CI/CD.
 */
@Service
public class InfiniteAutoHealer {

    private static final Logger log = LoggerFactory.getLogger(InfiniteAutoHealer.class);
    private final SelfHealingService selfHealingService;

    public InfiniteAutoHealer(SelfHealingService selfHealingService) {
        this.selfHealingService = selfHealingService;
    }

    /**
     * This method now delegates to the unified SelfHealingService.
     */
    public String developUntilPerfection(String taskCategory, String userPrompt) {
        log.info("[Infinite Auto-Healer] Delegating to unified SelfHealingService");
        return selfHealingService.developUntilPerfection(taskCategory, userPrompt);
    }

}