package com.supremeai.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.supremeai.model.ConsensusResult;
import com.supremeai.provider.AIProviderFactory;
import com.supremeai.service.MultiAIVotingService;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class MultiAIConsensusControllerTest {

  private MultiAIConsensusController controller;

  @Mock private MultiAIVotingService votingService;

  @Mock private AIProviderFactory providerFactory;

  private ConsensusResult consensusResult;

  @BeforeEach
  public void setUp() {
    controller = new MultiAIConsensusController();
    setField(controller, "votingService", votingService);
    setField(controller, "providerFactory", providerFactory);

    consensusResult = new ConsensusResult();
    consensusResult.setQuestion("What is AI?");
    consensusResult.setConsensusAnswer("Test Answer");
    consensusResult.setVotes(Collections.emptyList());
    consensusResult.setAverageConfidence(0.85);
    consensusResult.setStrength("STRONG");
  }

  private void setField(Object target, String fieldName, Object value) {
    try {
      java.lang.reflect.Field field = MultiAIConsensusController.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testAskAllAIs_Success() {
    // Arrange
    Map<String, Object> request = new HashMap<>();
    request.put("question", "What is AI?");
    request.put("providers", Collections.singletonList("openai"));
    request.put("timeout", 5000);

    when(votingService.askConsensus(eq("What is AI?"), anyList(), anyLong()))
        .thenReturn(Mono.just(consensusResult));

    // Act
    Mono<ResponseEntity<Object>> result = controller.voteOnQuestion(request);

    // Assert
    StepVerifier.create(result)
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
              Map<String, Object> body = (Map<String, Object>) response.getBody();
              assertNotNull(body);
              assertEquals("Test Answer", body.get("consensus"));
              assertEquals(0.85, body.get("confidence"));
              assertEquals("STRONG", body.get("strength"));
            })
        .verifyComplete();
  }

  @Test
  public void testAskAllAIs_EmptyProviders() {
    // Arrange
    Map<String, Object> request = new HashMap<>();
    request.put("question", "What is AI?");
    request.put("providers", Collections.emptyList());
    request.put("timeout", 5000);

    when(providerFactory.getAvailableProviderIds()).thenReturn(Collections.singletonList("openai"));
    when(votingService.askConsensus(eq("What is AI?"), anyList(), anyLong()))
        .thenReturn(Mono.just(consensusResult));

    // Act
    Mono<ResponseEntity<Object>> result = controller.voteOnQuestion(request);

    // Assert
    StepVerifier.create(result)
        .assertNext(
            response -> {
              assertEquals(HttpStatus.OK, response.getStatusCode());
            })
        .verifyComplete();
  }

  @Test
  public void testAskAllAIs_ServiceError() {
    // Arrange
    Map<String, Object> request = new HashMap<>();
    request.put("question", "What is AI?");
    request.put("providers", Collections.singletonList("openai"));
    request.put("timeout", 5000);

    when(votingService.askConsensus(anyString(), anyList(), anyLong()))
        .thenReturn(Mono.error(new RuntimeException("Service unavailable")));

    // Act
    Mono<ResponseEntity<Object>> result = controller.voteOnQuestion(request);

    // Assert
    StepVerifier.create(result).expectError(RuntimeException.class).verify();
  }

  @Test
  public void testAskAllAIs_MissingQuestion() {
    // Arrange
    Map<String, Object> request = new HashMap<>();
    request.put("providers", Collections.singletonList("openai"));
    request.put("timeout", 5000);

    // Act
    Mono<ResponseEntity<Object>> result = controller.voteOnQuestion(request);

    // Assert
    StepVerifier.create(result)
        .assertNext(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              Map<String, Object> body = (Map<String, Object>) response.getBody();
              assertNotNull(body);
              assertEquals("Question is required", body.get("error"));
            })
        .verifyComplete();
  }

  @Test
  public void testCompareStrategies_InvalidProviders() {
    // Arrange
    Map<String, Object> request = new HashMap<>();
    request.put("question", "What is AI?");
    request.put("providers", Collections.singletonList("openai"));

    // Act
    Mono<ResponseEntity<Object>> result = controller.compareStrategies(request);

    // Assert
    StepVerifier.create(result)
        .assertNext(
            response -> {
              assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
              Map<String, Object> body = (Map<String, Object>) response.getBody();
              assertNotNull(body);
              assertEquals("At least 2 providers required for comparison", body.get("error"));
            })
        .verifyComplete();
  }
}
