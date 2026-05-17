package com.supremeai.service;

import com.supremeai.model.*;
import com.supremeai.repository.*;
import com.supremeai.fallback.AIFallbackOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatProcessingServiceTest {

    @Mock
    private ChatClassifier chatClassifier;
    @Mock
    private ChatRuleRepository chatRuleRepository;
    @Mock
    private ChatPlanRepository chatPlanRepository;
    @Mock
    private ChatCommandRepository chatCommandRepository;
    @Mock
    private ChatConfirmationRepository chatConfirmationRepository;
    @Mock
    private ChatHistoryRepository chatHistoryRepository;
    @Mock
    private AIFallbackOrchestrator fallbackOrchestrator;

    @InjectMocks
    private ChatProcessingService chatProcessingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(chatHistoryRepository.save(any())).thenReturn(Mono.just(new ChatMessage()));
    }

    @Test
    void testProcessNormalMessage() {
        String userId = "user123";
        String message = "Hello AI";
        String aiReply = "Hello Human!";
        
        ChatClassifier.ClassificationResult result = mock(ChatClassifier.ClassificationResult.class);
        when(result.getChatType()).thenReturn(ChatClassifier.ChatType.NORMAL);
        when(result.getConfidence()).thenReturn(0.95);
        when(result.getReason()).thenReturn("Greeting");
        
        when(chatClassifier.classify(message)).thenReturn(result);
        when(fallbackOrchestrator.executeWithSupremeIntelligence(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(Mono.just(aiReply));
        
        Map<String, Object> response = chatProcessingService.processMessage(userId, message, false).block();
        
        assertEquals("normal", response.get("chat_type"));
        assertEquals(aiReply, response.get("response"));
        assertFalse((Boolean) response.get("needs_confirmation"));
        verify(chatHistoryRepository, times(2)).save(any());
    }

    @Test
    void testProcessRuleMessage() {
        String userId = "admin123";
        String message = "Rule: Always be polite";
        
        ChatClassifier.ClassificationResult result = mock(ChatClassifier.ClassificationResult.class);
        when(result.getChatType()).thenReturn(ChatClassifier.ChatType.RULE);
        when(result.getConfidence()).thenReturn(0.88);
        
        when(chatClassifier.classify(message)).thenReturn(result);
        when(chatClassifier.extractContent(eq(message), eq(ChatClassifier.ChatType.RULE))).thenReturn("Always be polite");
        when(chatRuleRepository.save(any())).thenReturn(Mono.just(new ChatRule()));
        
        Map<String, Object> response = chatProcessingService.processMessage(userId, message, true).block();
        
        assertEquals("rule", response.get("chat_type"));
        assertTrue((Boolean) response.get("needs_confirmation"));
        assertNotNull(response.get("item_id"));
        verify(chatRuleRepository, times(1)).save(any());
    }

    @Test
    void testConfirmItem() {
        // First process a rule to put it in pending
        String userId = "admin123";
        String message = "Rule: test";
        ChatClassifier.ClassificationResult classResult = mock(ChatClassifier.ClassificationResult.class);
        when(classResult.getChatType()).thenReturn(ChatClassifier.ChatType.RULE);
        when(chatClassifier.classify(message)).thenReturn(classResult);
        when(chatRuleRepository.save(any())).thenReturn(Mono.just(new ChatRule()));
        
        Map<String, Object> processResponse = chatProcessingService.processMessage(userId, message, true).block();
        String itemId = (String) processResponse.get("item_id");
        
        // Now confirm it
        when(chatConfirmationRepository.save(any())).thenReturn(Mono.just(new ChatConfirmation()));
        when(chatRuleRepository.findById(itemId)).thenReturn(Mono.just(new ChatRule()));
        
        Map<String, Object> confirmResponse = chatProcessingService.confirmItem(itemId, true, userId).block();
        
        assertTrue((Boolean) confirmResponse.get("success"));
        assertEquals(itemId, confirmResponse.get("item_id"));
        verify(chatConfirmationRepository, times(1)).save(any());
    }
}
