package com.supremeai.agentorchestration;

public class ExpertAgentRouter {
    public String route(String prompt) {
        if (prompt.contains("Java") || prompt.contains("function") || prompt.contains("sort")) {
            return "CODING_AGENT";
        }
        return "GENERAL_AGENT";
    }
}
