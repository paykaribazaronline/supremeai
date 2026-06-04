package com.supremeai.service;

import com.supremeai.model.APIProvider;
import com.supremeai.repository.ProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AutoProviderDiscoveryServiceTest {

    @Mock
    private ProviderRepository providerRepository;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec getUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec getHeaderSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private AutoProviderDiscoveryService autoDiscoveryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(autoDiscoveryService, "webClient", webClient);

        // Mock WebClient chains
        when(webClient.get()).thenReturn(getUriSpec);
        when(getUriSpec.uri(anyString())).thenReturn(getHeaderSpec);
        when(getHeaderSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void testFetchOpenRouterProviders_Success() {
        Map<String, Object> modelPricing = Map.of("prompt", "0.0", "completion", "0.0");
        Map<String, Object> modelData = Map.of(
                "id", "meta-llama/llama-3-8b-instruct:free",
                "name", "Llama 3 8B Instruct Free",
                "description", "Llama 3 model",
                "pricing", modelPricing
        );
        Map<String, Object> openRouterResponse = Map.of("data", List.of(modelData));

        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(openRouterResponse));

        Flux<APIProvider> result = autoDiscoveryService.fetchOpenRouterProviders();

        StepVerifier.create(result)
                .expectNextMatches(provider -> 
                        provider.getId().startsWith("openrouter_") &&
                        provider.getName().equals("OpenRouter - Llama 3 8B Instruct Free") &&
                        "openai".equals(provider.getType()) &&
                        "active".equals(provider.getStatus())
                )
                .verifyComplete();
    }

    @Test
    void testFetchHuggingFaceProviders_Success() {
        Map<String, Object> hfModel = Map.of(
                "id", "meta-llama/Meta-Llama-3-8B-Instruct",
                "author", "meta-llama"
        );

        when(responseSpec.bodyToFlux(Map.class)).thenReturn(Flux.just(hfModel));

        Flux<APIProvider> result = autoDiscoveryService.fetchHuggingFaceProviders();

        StepVerifier.create(result)
                .expectNextMatches(provider -> 
                        provider.getId().startsWith("huggingface_") &&
                        provider.getName().equals("HuggingFace - meta-llama/Meta-Llama-3-8B-Instruct") &&
                        "huggingface".equals(provider.getType())
                )
                .verifyComplete();
    }

    @Test
    void testScanForNewProviders_ResilientFallback() {
        // Mock API failures to trigger fallback
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.error(new RuntimeException("API Down")));
        when(responseSpec.bodyToFlux(Map.class)).thenReturn(Flux.error(new RuntimeException("API Down")));

        when(providerRepository.findByName(anyString())).thenReturn(Mono.empty());
        when(providerRepository.save(any(APIProvider.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        autoDiscoveryService.scanForNewProviders();

        // Verify that fallback mock provider is saved
        verify(providerRepository, atLeastOnce()).save(any(APIProvider.class));
    }
}
