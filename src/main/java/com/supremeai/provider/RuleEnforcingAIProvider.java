package com.supremeai.provider;

import com.supremeai.agent.AgentRuleService;
import reactor.core.publisher.Mono;
import java.util.Map;

/**
 * Decorator for AIProvider that automatically attaches global agent rules to every prompt.
 * This ensures system-wide adherence to the AGENTS.md guidelines.
 */
public class RuleEnforcingAIProvider implements AIProvider {
    private final AIProvider delegate;
    private final AgentRuleService ruleService;

    public RuleEnforcingAIProvider(AIProvider delegate, AgentRuleService ruleService) {
        this.delegate = delegate;
        this.ruleService = ruleService;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public Map<String, Object> getCapabilities() {
        return delegate.getCapabilities();
    }

    @Override
    public Mono<String> generate(String prompt) {
        // We assume "build" context as per user request for SupremeAI build rules
        return ruleService.enrichPrompt(prompt, "build")
                .flatMap(delegate::generate);
    }
}
