package com.supremeai.controller;

import com.supremeai.service.PromptEnhancerService;
import com.supremeai.service.AgentOrchestrationHub;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/admin/rules")
@PreAuthorize("hasRole('ADMIN')")
public class SystemAdminRuleController {
    public SystemAdminRuleController(PromptEnhancerService promptEnhancer, AgentOrchestrationHub orchestrationHub) {
        this.promptEnhancer = promptEnhancer;
        this.orchestrationHub = orchestrationHub;
    }


    private final Map<String, String> activeRules = new ConcurrentHashMap<>();



    @PostMapping("/execute")
    public String executeAdminCommand(@RequestBody String rawCommand) {
        // Step 1: Enhance the simple admin command
        String enhancedCommand = promptEnhancer.enhanceAdminCommand(rawCommand);
        
        // Step 2: Orchestrate based on enhanced command
        // Note: Future versions will route this to a real LLM task executor
        return "Executed: " + enhancedCommand;
    }

    @PostMapping("/update")
    public String setSystemRule(@RequestParam String key, @RequestParam String value) {
        activeRules.put(key, value);
        return "Rule updated: " + key + " = " + value;
    }

    @GetMapping("/list")
    public Map<String, String> getAllRules() {
        return activeRules;
    }
}
