package com.supremeai.service;

import com.supremeai.model.*;
import com.supremeai.repository.*;
import com.supremeai.fallback.ThirdOpinionOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatProcessingServiceTest {ChatHistoryRepositorypublic ChatProcessingServiceTest(ChatHistoryRepository chatHistoryRepository, ThirdOpinionOrchestrator fallbackOrchestrator, AIProviderService aiProviderService, com.supremeai.service.browser.BrowserService browserService, AdminProviderValidationService validationService, CyberSecuritySkillService cyberSecuritySkillService, EnhancedLearningService enhancedLearningService, KnowledgeService knowledgeService, AutonomousQuestioningEngine autonomousEngine, ConfigService configService, MultiAIVotingService multiAIVotingService, ChatProcessingService chatProcessingService) {
ChatHistoryRepository    this.chatHistoryRepository = chatHistoryRepository;
ChatHistoryRepository    this.fallbackOrchestrator = fallbackOrchestrator;
ChatHistoryRepository    this.aiProviderService = aiProviderService;
ChatHistoryRepository    this.browserService = browserService;
ChatHistoryRepository    this.validationService = validationService;
ChatHistoryRepository    this.cyberSecuritySkillService = cyberSecuritySkillService;
ChatHistoryRepository    this.enhancedLearningService = enhancedLearningService;
ChatHistoryRepository    this.knowledgeService = knowledgeService;
ChatHistoryRepository    this.autonomousEngine = autonomousEngine;
ChatHistoryRepository    this.configService = configService;
ChatHistoryRepository    this.multiAIVotingService = multiAIVotingService;
ChatHistoryRepository    this.chatProcessingService = chatProcessingService;
ChatHistoryRepository}














    @InjectMocks


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(chatHistoryRepository.save(any())).thenReturn(Mono.just(new ChatMessage()));
        when(chatHistoryRepository.findByUserIdOrderByTimestampAsc(anyString())).thenReturn(Flux.empty());
        when(enhancedLearningService.learnFromInteraction(anyString(), anyString(), anyString())).thenReturn(Mono.empty());
        when(browserService.searchAndScrape(anyString(), anyString(), anyString())).thenReturn(Mono.just("mock scraped data"));
        when(knowledgeService.getRelevantContext(anyString())).thenReturn(Mono.just("mock relevant context"));
        
        AutonomousQuestioningEngine.ValidationResult mockValidation = new AutonomousQuestioningEngine.ValidationResult();
        mockValidation.setComplete(true);
        mockValidation.setIntentType(AutonomousQuestioningEngine.IntentType.FACTUAL);
        when(autonomousEngine.validateAndQuestion(anyString(), any())).thenReturn(Mono.just(mockValidation));
        
        when(configService.getEffectiveString(anyString(), anyString()))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(1)));
    }

    @Test
    void testProcessNormalMessage() {
        String userId = "user123";
        String message = "Hello AI";
        String aiReply = "Hello Human!";
        
        when(fallbackOrchestrator.executeWithSupremeIntelligence(anyString(), anyString(), anyString(), anyString()))
            .thenReturn(Mono.just(aiReply));
        
        Map<String, Object> response = chatProcessingService.processMessage(userId, message, false).block();
        
        assertEquals(aiReply, response.get("response"));
        assertFalse((Boolean) response.get("needs_confirmation"));
        verify(chatHistoryRepository, times(2)).save(any());
    }

}