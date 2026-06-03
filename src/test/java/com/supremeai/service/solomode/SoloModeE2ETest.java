package com.supremeai.service.solomode;

import com.supremeai.provider.AIProviderFactory;
import com.supremeai.repository.ProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SoloModeE2ETest {









    @BeforeEach
    void setUp() {ProviderRepositorypublic SoloModeE2ETest(ProviderRepository providerRepository, AIProviderFactory providerFactory, WebClient.Builder webClientBuilder, SoloModeManagerService soloModeManager) {
ProviderRepository    this.providerRepository = providerRepository;
ProviderRepository    this.providerFactory = providerFactory;
ProviderRepository    this.webClientBuilder = webClientBuilder;
ProviderRepository    this.soloModeManager = soloModeManager;
ProviderRepository}

        soloModeManager = new SoloModeManagerService();
        try {
            java.lang.reflect.Field providerRepoField = SoloModeManagerService.class.getDeclaredField("providerRepository");
            providerRepoField.setAccessible(true);
            providerRepoField.set(soloModeManager, providerRepository);

            java.lang.reflect.Field factoryField = SoloModeManagerService.class.getDeclaredField("providerFactory");
            factoryField.setAccessible(true);
            factoryField.set(soloModeManager, providerFactory);

            java.lang.reflect.Field webClientField = SoloModeManagerService.class.getDeclaredField("webClientBuilder");
            webClientField.setAccessible(true);
            webClientField.set(soloModeManager, webClientBuilder);

            java.lang.reflect.Field firebaseField = SoloModeManagerService.class.getDeclaredField("firebaseRealtimeService");
            firebaseField.setAccessible(true);
            firebaseField.set(soloModeManager, mock(com.supremeai.service.FirebaseRealtimeService.class));
        } catch (Exception e) {
            fail("Failed to inject mocks: " + e.getMessage());
        }
    }

    @Test
    void testEmergencyCodeGeneration_springBoot() {
        String code = soloModeManager.generateEmergencyScaffold("Spring Boot REST API");
        
        assertNotNull(code);
        assertTrue(code.contains("@RestController"));
        assertTrue(code.contains("/api/health"));
        assertTrue(code.contains("Emergency"));
    }

    @Test
    void testEmergencyCodeGeneration_react() {
        String code = soloModeManager.generateEmergencyScaffold("React frontend application");
        
        assertNotNull(code);
        assertTrue(code.contains("React"));
        assertTrue(code.contains("EmergencyApp"));
        assertTrue(code.contains("Emergency Web Scaffold"));
    }

    @Test
    void testEmergencyCodeGeneration_generic() {
        String code = soloModeManager.generateEmergencyScaffold("generic script");
        
        assertNotNull(code);
        assertTrue(code.contains("Emergency scaffold"));
        assertTrue(code.contains("console.log"));
    }

    @Test
    void testStepLimitGuard_initialState() {
        assertTrue(soloModeManager.canExecuteAutonomousStep());
    }

    @Test
    void testStepCounterReset() {
        for (int i = 0; i < 10; i++) {
            soloModeManager.incrementStepCounter();
        }
        
        soloModeManager.resetStepCounter();
        
        assertTrue(soloModeManager.canExecuteAutonomousStep());
    }

    @Test
    void testStepCounterThreadSafety() throws InterruptedException {
        int numThreads = 10;
        int incrementsPerThread = 100;
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(numThreads);
        
        for (int t = 0; t < numThreads; t++) {
            new Thread(() -> {
                try {
                    for (int i = 0; i < incrementsPerThread; i++) {
                        soloModeManager.incrementStepCounter();
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        
        assertEquals(numThreads * incrementsPerThread, soloModeManager.getStepCounterForTest());
    }

    @Test
    void testVisionServiceFallback() {
        Mono<String> result = soloModeManager.getVisionFallback("describe this image", new byte[]{1, 2, 3});
        
        assertNotNull(result);
        String response = result.block();
        assertNotNull(response);
        assertTrue(response.contains("[VISION_FALLBACK]"));
        assertTrue(response.contains("Image analysis skipped"));
    }

    @Test
    void testProviderRecovery_emptyList() {
        when(providerRepository.findAll()).thenReturn(Flux.empty());
        
        Mono<java.util.List<String>> result = soloModeManager.recoverFailedProviders();
        
        assertNotNull(result);
        assertTrue(result.block().isEmpty());
    }
}