package com.supremeai.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AgentOrchestrationHubTest {

    private AgentOrchestrationHub hub;

    @BeforeEach
    void setUp() {
        hub = new AgentOrchestrationHub();
    }

    @Test
    void hub_shouldBeInstantiable() {
        assertNotNull(hub);
        assertTrue(hub instanceof AgentOrchestrationHub);
    }

    @Test
    void executeAgent_shouldHandleKnownAgents() {
        // The hub routes to executeAgent with agent name
        assertNotNull(hub.executeAgent("ReverseEngineeringAgent", Map.of("url", "https://example.com")));
    }

    @Test
    void executeAgent_shouldRejectUnknownAgent() {
        assertThrows(RuntimeException.class, () ->
            hub.executeAgent("UnknownAgent", Map.of("test", false)).block()
        );
    }

    @Test
    void executeAgent_codeGenerationWorks() {
        var result = hub.executeAgent("CodeGenerationAgent", Map.of("requirements", "test requirements"));
        assertNotNull(result);
    }

    @Test
    void executeAgent_simulatorWorks() {
        var result = hub.executeAgent("SimulatorAgent", Map.of("app_id", "test_app"));
        assertNotNull(result);
    }
}