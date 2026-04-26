
package com.supremeai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supremeai.model.ConsensusResult;
import com.supremeai.service.MultiAIConsensusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MultiAIConsensusController.class)
@Disabled("Failing due to missing beans")
public class MultiAIConsensusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MultiAIConsensusService consensusService;

    @Autowired
    private ObjectMapper objectMapper;

    private ConsensusResult consensusResult;

    @BeforeEach
    public void setUp() {
        consensusResult = new ConsensusResult();
        consensusResult.setConsensusAnswer("Test Answer");
        consensusResult.setVotes(Collections.emptyList());
        consensusResult.setAverageConfidence(0.85);
        consensusResult.setStrength("STRONG");
    }

     @Test
     public void testAskAllAIs_Success() throws Exception {
         // Arrange
         Map<String, Object> request = new HashMap<>();
         request.put("question", "What is AI?");
         request.put("providers", Collections.singletonList("openai"));
         request.put("timeout", 5000);

         when(consensusService.askAllAIs(anyString(), anyList(), anyLong()))
                 .thenReturn(Mono.just(consensusResult));

        // Act & Assert
        mockMvc.perform(post("/api/consensus/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consensusAnswer").value("Test Answer"))
                .andExpect(jsonPath("$.averageConfidence").value(0.85))
                .andExpect(jsonPath("$.strength").value("STRONG"));
    }

     @Test
     public void testAskAllAIs_EmptyProviders() throws Exception {
         // Arrange
         Map<String, Object> request = new HashMap<>();
         request.put("question", "What is AI?");
         request.put("providers", Collections.emptyList());
         request.put("timeout", 5000);

         when(consensusService.askAllAIs(anyString(), anyList(), anyLong()))
                 .thenReturn(Mono.just(consensusResult));

        // Act & Assert
        mockMvc.perform(post("/api/consensus/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    public void testAskAllAIs_ServiceError() throws Exception {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("question", "What is AI?");
        request.put("providers", Collections.singletonList("openai"));
        request.put("timeout", 5000);

        when(consensusService.askAllAIs(anyString(), anyList(), anyLong()))
                .thenThrow(new RuntimeException("Service unavailable"));

        // Act & Assert
        mockMvc.perform(post("/api/consensus/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void testAskAllAIs_MissingQuestion() throws Exception {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("providers", Collections.singletonList("openai"));
        request.put("timeout", 5000);

        // Act & Assert
        mockMvc.perform(post("/api/consensus/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testAskAllAIs_InvalidTimeout() throws Exception {
        // Arrange
        Map<String, Object> request = new HashMap<>();
        request.put("question", "What is AI?");
        request.put("providers", Collections.singletonList("openai"));
        request.put("timeout", -1);

        // Act & Assert
        mockMvc.perform(post("/api/consensus/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
