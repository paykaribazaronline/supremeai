package com.supremeai.service;

import org.springframework.stereotype.Service;

/**
 * Service to enhance simple Admin/User commands into detailed system instructions.
 */
@Service
public class PromptEnhancerService {

    public String enhanceAdminCommand(String simpleCommand) {
        // Here we inject professional system orchestration context
        return "ACT AS SYSTEM ORCHESTRATOR. " +
               "CONTEXT: Admin requested: '" + simpleCommand + "'. " +
               "REQUIREMENT: Provide a structured, security-focused, and highly optimized execution plan. " +
               "Include audit logs for every step.";
    }

    public String enhanceUserPrompt(String userPrompt) {
        return "You are an expert AI assistant. User said: '" + userPrompt + "'. " +
               "Please provide a clear, concise, and helpful response.";
    }
}
