package com.supremeai.service;

import com.supremeai.model.KnowledgeDomain;
import com.supremeai.model.SystemLearning;
import com.supremeai.repository.KnowledgeDomainRepository;
import com.supremeai.repository.KnowledgeRecommendationRepository;
import com.supremeai.repository.SystemLearningRepository;
import com.supremeai.learning.active.ActiveInternetScraper;
import com.supremeai.controller.WebSocketController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KnowledgeServiceTest {KnowledgeDomainRepositorypublic KnowledgeServiceTest(KnowledgeDomainRepository domainRepository, KnowledgeRecommendationRepository recommendationRepository, SystemLearningRepository learningRepository, ActiveInternetScraper scraper, WebSocketController webSocketController, KnowledgeService knowledgeService) {
KnowledgeDomainRepository    this.domainRepository = domainRepository;
KnowledgeDomainRepository    this.recommendationRepository = recommendationRepository;
KnowledgeDomainRepository    this.learningRepository = learningRepository;
KnowledgeDomainRepository    this.scraper = scraper;
KnowledgeDomainRepository    this.webSocketController = webSocketController;
KnowledgeDomainRepository    this.knowledgeService = knowledgeService;
KnowledgeDomainRepository}












 @InjectMocks


 @Test
 void testProcessMultipleWebsites() {
  // Arrange
  List<String> topics = Arrays.asList("Spring Boot", "React");

  KnowledgeDomain domain1 = new KnowledgeDomain("Spring Boot", Arrays.asList("spring", "boot"));
  domain1.setId("domain-1");
  KnowledgeDomain domain2 = new KnowledgeDomain("React", Arrays.asList("react"));
  domain2.setId("domain-2");

  // Mock registration and completion saves
  when(domainRepository.save(any(KnowledgeDomain.class)))
    .thenReturn(Mono.just(domain1))
    .thenReturn(Mono.just(domain2))
    .thenReturn(Mono.just(domain1))
    .thenReturn(Mono.just(domain2));

  when(domainRepository.findById("domain-1")).thenReturn(Mono.just(domain1));
  when(domainRepository.findById("domain-2")).thenReturn(Mono.just(domain2));

ActiveInternetScraper.ScrapedIssue issue = mock(ActiveInternetScraper.ScrapedIssue.class);
    when(issue.getTitle()).thenReturn("Sample Title");
    when(issue.getAuthority()).thenReturn(new ActiveInternetScraper.ScrapedIssue.AuthorityWrapper(0.9));

  when(scraper.scrapeKnowledge(anyString(), anyList())).thenReturn(Flux.just(issue));
  when(learningRepository.save(any(SystemLearning.class))).thenReturn(Mono.just(new SystemLearning()));

  // Act & Assert
  StepVerifier.create(knowledgeService.processMultipleWebsites(topics))
    .verifyComplete();

  verify(scraper, times(2)).scrapeKnowledge(anyString(), anyList());
  verify(learningRepository, times(2)).save(any(SystemLearning.class));
  verify(domainRepository, times(4)).save(any(KnowledgeDomain.class));

  // Verify WebSocket broadcasts (Start, Progress, Complete) for each of the 2
  // topics
  verify(webSocketController, times(2)).broadcastSystemEvent(eq("KNOWLEDGE_CRAWL"), anyString(), eq(0.0), isNull(),
    anyString());
  verify(webSocketController, times(2)).broadcastSystemEvent(eq("KNOWLEDGE_CRAWL"), anyString(), anyDouble(),
    anyString(), anyString());
  verify(webSocketController, times(2)).broadcastSystemEvent(eq("KNOWLEDGE_CRAWL"), anyString(), eq(100.0), isNull(),
    anyString());
 }
}