package com.supremeai.controller;

import com.supremeai.service.PromptEnhancerService;
import com.supremeai.service.AgentOrchestrationHub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/admin/rules")
public class AdminRuleController {

    private final Map<String, String> activeRules = new ConcurrentHashMap<>();

    @Autowired
    private PromptEnhancerService promptEnhancer;

    @Autowired
    private AgentOrchestrationHub orchestrationHub;

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
