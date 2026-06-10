package com.supremeai.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.supremeai.fallback.ThirdOpinionOrchestrator;
import com.supremeai.model.*;
import com.supremeai.repository.*;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class ChatProcessingServiceTest {

  @Mock private ChatHistoryRepository chatHistoryRepository;
  @Mock private ThirdOpinionOrchestrator fallbackOrchestrator;
  @Mock private AIProviderService aiProviderService;
  @Mock private com.supremeai.service.browser.BrowserService browserService;
  @Mock private AdminProviderValidationService validationService;
  @Mock private CyberSecuritySkillService cyberSecuritySkillService;
  @Mock private EnhancedLearningService enhancedLearningService;
  @Mock private KnowledgeService knowledgeService;
  @Mock private AutonomousQuestioningEngine autonomousEngine;
  @Mock private ConfigService configService;
  @Mock private MultiAIVotingService multiAIVotingService;
  @Mock private DynamicRegistryService dynamicRegistryService;

  @InjectMocks private ChatProcessingService chatProcessingService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(chatHistoryRepository.save(any())).thenReturn(Mono.just(new ChatMessage()));
    when(chatHistoryRepository.findByUserIdOrderByTimestampAsc(anyString()))
        .thenReturn(Flux.empty());
    when(enhancedLearningService.learnFromInteraction(anyString(), anyString(), anyString()))
        .thenReturn(Mono.empty());
    when(browserService.searchAndScrape(anyString(), anyString(), anyString()))
        .thenReturn(Mono.just("mock scraped data"));
    when(knowledgeService.getRelevantContext(anyString()))
        .thenReturn(Mono.just("mock relevant context"));
  }

  @Test
  void testProcessNormalMessage() {
    String userId = "user123";
    String message = "Hello AI";
    String aiReply = "Hello Human!";

    AutonomousQuestioningEngine.ValidationResult greetingValidation =
        new AutonomousQuestioningEngine.ValidationResult();
    greetingValidation.setComplete(true);
    greetingValidation.setIntentType(AutonomousQuestioningEngine.IntentType.GREETING);
    when(autonomousEngine.validateAndQuestion(anyString(), any()))
        .thenReturn(Mono.just(greetingValidation));

    when(dynamicRegistryService.getMessage(eq("chat.greeting"), anyString()))
        .thenReturn("Hello Human!");
    when(configService.getEffectiveString(eq("chat.greeting.message"), anyString()))
        .thenReturn(Mono.just("Hello Human!"));

    Map<String, Object> response =
        chatProcessingService.processMessage(userId, message, false).block();

    assertEquals("Hello Human!", response.get("response"));
    assertFalse((Boolean) response.get("needs_confirmation"));
    verify(chatHistoryRepository, times(2)).save(any());
  }
}
