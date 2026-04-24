package com.supremeai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supremeai.model.ActivityLog;
import com.supremeai.model.SystemConfig;
import com.supremeai.model.UserTier;
import com.supremeai.repository.ActivityLogRepository;
import com.supremeai.repository.SystemConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigServiceTest {

    @Mock
    private SystemConfigRepository configRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ActivityLogRepository activityLogRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ConfigService configService;

    @BeforeEach
    void setUp() throws Exception {
        // Reset state before each test - set cachedConfig to null
        java.lang.reflect.Field cachedConfigField = ConfigService.class.getDeclaredField("cachedConfig");
        cachedConfigField.setAccessible(true);
        cachedConfigField.set(configService, null);
    }

    @Test
    void init_CallsRefreshCache() {
        SystemConfig mockConfig = new SystemConfig();
        when(configRepository.findById("global_settings")).thenReturn(Mono.just(mockConfig));

        configService.init();

        verify(configRepository, times(1)).findById("global_settings");
    }

    @Test
    void refreshCache_ConfigExists_ReturnsConfigAndUpdatesCache() {
        SystemConfig mockConfig = new SystemConfig();
        mockConfig.setId("global_settings");
        when(configRepository.findById("global_settings")).thenReturn(Mono.just(mockConfig));

        StepVerifier.create(configService.refreshCache())
                .expectNext(mockConfig)
                .verifyComplete();

        assertEquals(mockConfig, configService.getConfig());
    }

    @Test
    void refreshCache_ConfigDoesNotExist_CreatesDefaultAndSaves() {
        SystemConfig mockConfig = new SystemConfig();
        when(configRepository.findById("global_settings")).thenReturn(Mono.empty());
        when(configRepository.save(any(SystemConfig.class))).thenReturn(Mono.just(mockConfig));

        StepVerifier.create(configService.refreshCache())
                .expectNext(mockConfig)
                .verifyComplete();

        verify(configRepository, times(1)).save(any(SystemConfig.class));
    }

    @Test
    void getConfig_CacheNull_ReturnsNewConfig() {
        // Ensure cache is null
        SystemConfig config = configService.getConfig();
        assertNotNull(config);
        assertNull(config.getId()); // It's a fresh instance, not from cache
    }

    @Test
    void getQuotaForTier_Admin_ReturnsMinusOne() {
        long quota = configService.getQuotaForTier(UserTier.ADMIN);
        assertEquals(-1L, quota);
    }

    @Test
    void getQuotaForTier_Basic_ReturnsConfiguredQuota() {
        SystemConfig mockConfig = new SystemConfig();
        Map<String, Long> quotas = new HashMap<>();
        quotas.put(UserTier.BASIC.name(), 1000L);
        mockConfig.setTierQuotas(quotas);
        
        when(configRepository.findById("global_settings")).thenReturn(Mono.just(mockConfig));
        configService.refreshCache().block(); // Populate cache

        long quota = configService.getQuotaForTier(UserTier.BASIC);
        assertEquals(1000L, quota);
    }
    
    @Test
    void getMaxApisForTier_Basic_ReturnsConfiguredMaxApis() {
        SystemConfig mockConfig = new SystemConfig();
        Map<String, Integer> maxApis = new HashMap<>();
        maxApis.put(UserTier.BASIC.name(), 5);
        mockConfig.setTierMaxApis(maxApis);
        
        when(configRepository.findById("global_settings")).thenReturn(Mono.just(mockConfig));
        configService.refreshCache().block(); // Populate cache

        int apis = configService.getMaxApisForTier(UserTier.BASIC);
        assertEquals(5, apis);
    }

    @Test
    void updateConfig_SavesAndPublishesEvent() throws JsonProcessingException {
        SystemConfig newConfig = new SystemConfig();
        newConfig.setFullAuthority(true); // Trigger normalizeAuthoritySettings
        newConfig.setPermissions(new HashMap<>());
        
        when(configRepository.save(any(SystemConfig.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));
        when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(Mono.just(new ActivityLog()));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        StepVerifier.create(configService.updateConfig(newConfig, "user123", "127.0.0.1"))
                .consumeNextWith(saved -> {
                    assertEquals("global_settings", saved.getId());
                    assertEquals("allow", saved.getPermissions().get("read"));
                })
                .verifyComplete();

        verify(configRepository, times(1)).save(any(SystemConfig.class));
        verify(eventPublisher, times(1)).publishEvent(any(SystemConfig.class));
        verify(activityLogRepository, times(1)).save(any(ActivityLog.class));
    }

    @Test
    void updateTierQuota_UpdatesQuotaAndSaves() throws JsonProcessingException {
        SystemConfig mockConfig = new SystemConfig();
        when(configRepository.findById("global_settings")).thenReturn(Mono.just(mockConfig));
        configService.refreshCache().block(); // Populate cache

        when(configRepository.save(any(SystemConfig.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));
        when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(Mono.just(new ActivityLog()));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        StepVerifier.create(configService.updateTierQuota(UserTier.PRO, 5000L))
                .consumeNextWith(saved -> {
                    assertEquals(5000L, saved.getTierQuotas().get(UserTier.PRO.name()));
                })
                .verifyComplete();
    }
}