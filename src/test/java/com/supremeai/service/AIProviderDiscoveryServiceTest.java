package com.supremeai.service;

import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AIProviderDiscoveryServiceTest {

    @Mock
    private AIProviderFactory providerFactory;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec getUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec getHeaderSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private AIProviderDiscoveryService discoveryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Mock WebClient fluent API chains
        when(webClient.get()).thenReturn(getUriSpec);
        when(getUriSpec.uri(anyString(), (Object[]) any())).thenReturn(getHeaderSpec);
        when(getUriSpec.uri(anyString())).thenReturn(getHeaderSpec);
        when(getHeaderSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void testDiscoverModels_Success() {
        Map<String, Object> hfModel = Map.of("modelId", "gpt2");
        Map<String, Object> orModel = Map.of("id", "openai/gpt-3.5-turbo", "description", "Fast model");
        Map<String, Object> orResponse = Map.of("data", Collections.singletonList(orModel));

        // Mock HuggingFace response
        when(responseSpec.bodyToFlux(Map.class)).thenReturn(Flux.just(hfModel));
        
        // Mock OpenRouter response
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(orResponse));

        Flux<Map<String, Object>> result = discoveryService.discoverModels("gpt");

        StepVerifier.create(result)
                .expectNextMatches(m -> "gpt2".equals(m.get("name")) && "huggingface".equals(m.get("provider")))
                .expectNextMatches(m -> "openai/gpt-3.5-turbo".equals(m.get("name")) && "openrouter".equals(m.get("provider")))
                .verifyComplete();
    }

    @Test
    void testValidateKey_Success() {
        AIProvider provider = mock(AIProvider.class);
        when(providerFactory.getProvider("openai", "test-key")).thenReturn(provider);
        when(provider.generate("hi")).thenReturn(Mono.just("Hello!"));

        Mono<Boolean> result = discoveryService.validateKey("openai", "test-key");

        StepVerifier.create(result)
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void testValidateKey_Failure() {
        when(providerFactory.getProvider(anyString(), anyString())).thenThrow(new RuntimeException("Invalid provider"));

        Mono<Boolean> result = discoveryService.validateKey("invalid", "key");

        StepVerifier.create(result)
                .expectNext(false)
                .verifyComplete();
    }
}
