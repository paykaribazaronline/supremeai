package com.supremeai.service;

import com.supremeai.model.ReverseEngineeringJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentOrchestrationHubTest {

    @Mock
    private ReverseEngineeringIntegrationService reverseEngineeringIntegrationService;

    @Mock
    private CodeGenerationService codeGenerationService;

    @Mock
    private SimulatorService simulatorService;

    @InjectMocks
    private AgentOrchestrationHub hub;

    @Test
    void hub_shouldBeInstantiable() {
        assertNotNull(hub);
        assertTrue(hub instanceof AgentOrchestrationHub);
    }

    @Test
    void executeAgent_shouldHandleKnownAgents() {
        ReverseEngineeringJob mockJob = new ReverseEngineeringJob();
        mockJob.setJobId("job_123");
        mockJob.setStatus("STARTED");
        
        when(reverseEngineeringIntegrationService.startJob(anyString(), anyString(), anyString(), any(), any()))
            .thenReturn(Mono.just(mockJob));

        var result = hub.executeAgent("ReverseEngineeringAgent", Map.of("url", "https://example.com")).block();
        assertNotNull(result);
        assertEquals("job_123", result.get("jobId"));
    }

    @Test
    void executeAgent_shouldRejectUnknownAgent() {
        assertThrows(RuntimeException.class, () ->
            hub.executeAgent("UnknownAgent", Map.of("test", false)).block()
        );
    }

    @Test
    void executeAgent_codeGenerationWorks() {
        var result = hub.executeAgent("CodeGenerationAgent", Map.of("requirements", "test requirements")).block();
        assertNotNull(result);
        assertTrue(result.containsKey("appId"));
    }

    @Test
    void executeAgent_simulatorWorks() {
        var result = hub.executeAgent("SimulatorAgent", Map.of("app_id", "test_app")).block();
        assertNotNull(result);
        assertTrue(result.containsKey("sessionId"));
    }
}