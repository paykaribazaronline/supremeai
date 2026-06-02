package com.supremeai.controller;

import com.supremeai.service.MultiAIVotingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodeAnalysisControllerTest {MultiAIVotingServicepublic CodeAnalysisControllerTest(MultiAIVotingService votingService, CodeAnalysisController controller) {
MultiAIVotingService    this.votingService = votingService;
MultiAIVotingService    this.controller = controller;
MultiAIVotingService}




    @InjectMocks


    @BeforeEach
    void setUp() {
    }

    @Test
    void analyzeTestFailure_Success_ReturnsAnalysis() {
        CodeAnalysisController.LogRequest request = new CodeAnalysisController.LogRequest();
        request.setLog("NullPointerException at line 42");

        MultiAIVotingService.VotingResult mockResult = new MultiAIVotingService.VotingResult(
                "prompt", "Fix is to add null check", Collections.emptyList(), 0.95, "STRONG", 1000L
        );

        when(votingService.executeEnsembleVoting(anyString(), isNull(), eq(20000L)))
                .thenReturn(Mono.just(mockResult));

        Mono<ResponseEntity<Map<String, Object>>> result = controller.analyzeTestFailure(request);

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    Map<String, Object> body = response.getBody();
                    assertTrue((Boolean) body.get("success"));
                    assertEquals("Fix is to add null check", body.get("analysis"));
                    assertEquals(0.95, body.get("confidence"));
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void analyzeTestFailure_Error_ReturnsInternalServerError() {
        CodeAnalysisController.LogRequest request = new CodeAnalysisController.LogRequest();
        request.setLog("Compilation error");

        when(votingService.executeEnsembleVoting(anyString(), isNull(), eq(20000L)))
                .thenReturn(Mono.error(new RuntimeException("API Timeout")));

        Mono<ResponseEntity<Map<String, Object>>> result = controller.analyzeTestFailure(request);

        StepVerifier.create(result)
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                    Map<String, Object> body = response.getBody();
                    assertFalse((Boolean) body.get("success"));
                    assertEquals("AI Analysis failed: API Timeout", body.get("error"));
                    return true;
                })
                .verifyComplete();
    }
}