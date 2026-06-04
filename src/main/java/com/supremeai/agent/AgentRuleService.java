package com.supremeai.agent;

import com.supremeai.service.DynamicInstructionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Enhanced Service to load and enforce global agent rules dynamically.
 * Bridges the gap between file-based rules and Firestore-based dynamic rules.
 */
@Service
public class AgentRuleService {

    @Autowired
    private DynamicInstructionService dynamicInstructionService;

    /**
     * Enriches a prompt with dynamic rules from Firestore or file fallback.
     */
    public Mono<String> enrichPrompt(String prompt, String taskType) {
        return dynamicInstructionService.getCombinedInstructions(taskType)
                .map(rules -> "### SUPREMEAI BUILD & ARCHITECTURE GUIDELINES ###\n" +
                             "The following rules MUST be followed when generating code or building features:\n\n" +
                             rules + "\n\n" +
                             "### USER TASK ###\n" +
                             prompt);
    }

    /**
     * Legacy support for static wrapping (not recommended for dynamic rules but kept for compatibility)
     */
    public String wrapWithRules(String prompt) {
        // Since rules are now reactive/dynamic, we prefer enrichPrompt.
        // For sync calls, we provide a basic wrapper.
        return "### SUPREMEAI GLOBAL RULES ###\n" + prompt;
    }
}
