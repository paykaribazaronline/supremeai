package com.supremeai.controller;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class WebSocketPipelineIntegrationTest {SimpMessagingTemplatepublic WebSocketPipelineIntegrationTest(SimpMessagingTemplate messagingTemplate, WebSocketController webSocketController) {
SimpMessagingTemplate    this.messagingTemplate = messagingTemplate;
SimpMessagingTemplate    this.webSocketController = webSocketController;
SimpMessagingTemplate}




    @InjectMocks


    @Test
    public void testBroadcastAppGenProgress_SendsCorrectPayloadToTopic() {
        String requestId = "req-123";
        String appName = "SuperApp";
        String phase = "GENERATING_BACKEND";
        int progressPercentage = 45;
        String message = "Generating Spring Boot controllers";

        webSocketController.broadcastAppGenProgress(requestId, appName, phase, progressPercentage, message);

        verify(messagingTemplate).convertAndSend(eq("/topic/app-gen"), any(Map.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/app-gen/req-123"), any(Map.class));
    }

    @Test
    public void testBroadcastAnalysisProgress_SendsCorrectPayloadToTopic() {
        String jobId = "job-456";
        String projectName = "AlphaProject";
        String phase = "SCANNING";
        int filesProcessed = 5;
        int totalFiles = 10;
        String currentAgent = "SecurityAgent";
        int findingsSoFar = 2;
        String message = "Scanning AuthController.java";

        webSocketController.broadcastAnalysisProgress(jobId, projectName, phase, filesProcessed, totalFiles, currentAgent, findingsSoFar, message);

        verify(messagingTemplate).convertAndSend(eq("/topic/analysis"), any(Map.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/analysis/job-456"), any(Map.class));
    }
}
