package com.supremeai.service;

import com.supremeai.model.SystemConfig;
import com.supremeai.model.UserTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigServiceTest {

    @InjectMocks
    private ConfigService configService;

    @BeforeEach
    void setUp() {
        // Reset the cached config to null before each test to ensure clean state
        configService = new ConfigService();
    }

    @Test
    void getConfig_ReturnsNonNullConfig() {
        SystemConfig config = configService.getConfig();
        assertNotNull(config);
        assertEquals("global_settings", config.getId());
        assertEquals(1L, config.getVersion());
    }

    @Test
    void getQuotaForTier_Admin_ReturnsMinusOne() {
        long quota = configService.getQuotaForTier(UserTier.ADMIN);
        assertEquals(-1L, quota);
    }

    @Test
    void getQuotaForTier_Free_ReturnsDefaultQuota() {
        long quota = configService.getQuotaForTier(UserTier.FREE);
        assertEquals(1000L, quota);
    }

    @Test
    void getQuotaForTier_Basic_ReturnsDefaultQuota() {
        long quota = configService.getQuotaForTier(UserTier.BASIC);
        assertEquals(10000L, quota);
    }

    @Test
    void getQuotaForTier_Pro_ReturnsDefaultQuota() {
        long quota = configService.getQuotaForTier(UserTier.PRO);
        assertEquals(100000L, quota);
    }

    @Test
    void getMaxApisForTier_Admin_ReturnsMinusOne() {
        int maxApis = configService.getMaxApisForTier(UserTier.ADMIN);
        assertEquals(-1, maxApis);
    }

    @Test
    void getMaxApisForTier_Free_ReturnsDefaultMaxApis() {
        int maxApis = configService.getMaxApisForTier(UserTier.FREE);
        assertEquals(5, maxApis);
    }

    @Test
    void getMaxApisForTier_Basic_ReturnsDefaultMaxApis() {
        int maxApis = configService.getMaxApisForTier(UserTier.BASIC);
        assertEquals(20, maxApis);
    }

    @Test
    void getMaxSimulatorInstallsForTier_Admin_ReturnsFifty() {
        int maxInstalls = configService.getMaxSimulatorInstallsForTier(UserTier.ADMIN);
        assertEquals(50, maxInstalls);
    }

    @Test
    void getMaxSimulatorInstallsForTier_Free_ReturnsThree() {
        int maxInstalls = configService.getMaxSimulatorInstallsForTier(UserTier.FREE);
        assertEquals(3, maxInstalls);
    }

    @Test
    void refreshCache_ReturnsCurrentConfig() {
        StepVerifier.create(configService.refreshCache())
                .expectNextMatches(config -> 
                    "global_settings".equals(config.getId()) &&
                    config.getVersion() == 1L)
                .verifyComplete();
    }

    @Test
    void updateConfig_UpdatesConfigAndReturnsNewVersion() {
        SystemConfig newConfig = new SystemConfig();
        Map<String, Long> quotas = new HashMap<>();
        quotas.put(UserTier.FREE.name(), 5000L);
        newConfig.setTierQuotas(quotas);

        StepVerifier.create(configService.updateConfig(newConfig))
                .consumeNextWith(saved -> {
                    assertEquals("global_settings", saved.getId());
                    assertEquals(2L, saved.getVersion());
                    assertEquals(5000L, saved.getTierQuotas().get(UserTier.FREE.name()));
                })
                .verifyComplete();
    }

    @Test
    void updateConfig_WithActorInfo_UpdatesConfig() {
        SystemConfig newConfig = new SystemConfig();
        Map<String, Integer> maxApis = new HashMap<>();
        maxApis.put(UserTier.BASIC.name(), 50);
        newConfig.setTierMaxApis(maxApis);

        StepVerifier.create(configService.updateConfig(newConfig, "user123", "127.0.0.1"))
                .consumeNextWith(saved -> {
                    assertEquals("global_settings", saved.getId());
                    assertEquals(2L, saved.getVersion());
                    assertEquals(50, saved.getTierMaxApis().get(UserTier.BASIC.name()));
                })
                .verifyComplete();
    }
}
