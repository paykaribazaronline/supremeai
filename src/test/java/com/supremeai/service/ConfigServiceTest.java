package com.supremeai.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.supremeai.model.SystemConfig;
import com.supremeai.model.UserTier;
import com.supremeai.repository.SystemConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ConfigServiceTest {

  @Mock private SystemConfigRepository systemConfigRepository;

  @InjectMocks private ConfigService configService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testGetDefaultConfig() {
    SystemConfig config = configService.getConfig();
    assertNotNull(config);
    assertEquals("global_settings", config.getId());
    assertTrue(config.getTierQuotas().containsKey(UserTier.FREE.name()));
  }

  @Test
  void testRefreshCache() {
    SystemConfig dbConfig = new SystemConfig();
    dbConfig.setId("global_settings");
    dbConfig.setVersion(2L);
    dbConfig.getTierQuotas().put(UserTier.FREE.name(), 5000L);

    when(systemConfigRepository.findById("global_settings")).thenReturn(Mono.just(dbConfig));

    StepVerifier.create(configService.refreshCache())
        .expectNextMatches(
            config ->
                config.getVersion() == 2L
                    && config.getTierQuotas().get(UserTier.FREE.name()) == 5000L)
        .verifyComplete();

    assertEquals(5000L, configService.getQuotaForTier(UserTier.FREE));
  }

  @Test
  void testUpdateConfig() {
    SystemConfig newConfig = new SystemConfig();
    newConfig.getTierQuotas().put(UserTier.FREE.name(), 999L);

    when(systemConfigRepository.save(any()))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(configService.updateConfig(newConfig))
        .expectNextMatches(saved -> saved.getTierQuotas().get(UserTier.FREE.name()) == 999L)
        .verifyComplete();

    assertEquals(999L, configService.getQuotaForTier(UserTier.FREE));
  }
}
