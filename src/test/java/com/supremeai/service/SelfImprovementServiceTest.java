package com.supremeai.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.supremeai.admin.ProviderAdminService;
import com.supremeai.model.APIProvider;
import com.supremeai.model.SystemLearning;
import com.supremeai.repository.SystemLearningRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class SelfImprovementServiceTest {

  @Mock private SystemLearningRepository learningRepository;
  @Mock private MultiAIVotingService votingService;
  @Mock private ConfigService configService;
  @Mock private ProviderAdminService providerAdminService;

  private SelfImprovementService service;

  @BeforeEach
  void setUp() {
    service =
        new SelfImprovementService(
            learningRepository, votingService, configService, providerAdminService);
  }

  @Test
  void hourlyImprovementLoop_shouldCompleteWithoutError() {
    when(configService.getEffectiveSetting("learning_interval_minutes", 60L))
        .thenReturn(10L); // Force run

    SystemLearning entry = new SystemLearning();
    entry.setLearnedAt(LocalDateTime.now());
    entry.setContent("test prompt");
    when(learningRepository.findAll()).thenReturn(Flux.just(entry));

    APIProvider provider = new APIProvider();
    provider.setId("openai");
    provider.setActive(true);
    provider.setValidated(true);
    APIProvider provider2 = new APIProvider();
    provider2.setId("anthropic");
    provider2.setActive(true);
    provider2.setValidated(true);

    when(providerAdminService.getAllProviders()).thenReturn(Flux.just(provider, provider2));
    when(votingService.askConsensus(anyString(), anyList(), anyLong()))
        .thenReturn(Mono.just(new com.supremeai.model.ConsensusResult()));

    service.hourlyImprovementLoop();

    verify(learningRepository, times(1)).findAll();
  }
}
