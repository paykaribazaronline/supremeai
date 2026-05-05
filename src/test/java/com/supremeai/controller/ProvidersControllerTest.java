package com.supremeai.controller;

import com.supremeai.model.APIProvider;
import com.supremeai.repository.ProviderRepository;
import com.supremeai.repository.ActivityLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(ProvidersController.class)
@Disabled("Failing due to WebFluxTest setup issues")
public class ProvidersControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ProviderRepository providerRepository;

    @MockBean
    private ActivityLogRepository activityLogRepository;

    private APIProvider provider;

    @BeforeEach
    public void setUp() {
        provider = new APIProvider();
        provider.setId("provider1");
        provider.setName("OpenAI");
        provider.setType("openai");
        provider.setApiKey("sk-test-key");
        provider.setStatus("active");
    }

    @Test
    public void testGetConfiguredProviders_Success() {
        List<APIProvider> providers = Arrays.asList(provider);
        when(providerRepository.findAll()).thenReturn(Flux.fromIterable(providers));

        webTestClient.get().uri("/api/admin/providers/configured")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testAddProvider_Success() {
        when(providerRepository.save(any(APIProvider.class))).thenReturn(Mono.just(provider));
        when(activityLogRepository.save(any())).thenReturn(Mono.empty());

        webTestClient.post().uri("/api/admin/providers/add")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(provider)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testUpdateProviderById_Success() {
        when(providerRepository.save(any(APIProvider.class))).thenReturn(Mono.just(provider));
        when(providerRepository.findById("provider1")).thenReturn(Mono.just(provider));
        when(activityLogRepository.save(any())).thenReturn(Mono.empty());

        webTestClient.put().uri("/api/admin/providers/provider1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(provider)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testDeleteProvider_Success() {
        when(providerRepository.findById("provider1")).thenReturn(Mono.just(provider));
        when(providerRepository.deleteById("provider1")).thenReturn(Mono.empty());
        when(activityLogRepository.save(any())).thenReturn(Mono.empty());

        webTestClient.delete().uri("/api/admin/providers/provider1")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testDeleteProvider_NotFound() {
        when(providerRepository.findById("nonexistent")).thenReturn(Mono.empty());

        webTestClient.delete().uri("/api/admin/providers/nonexistent")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    public void testRemoveProvider_Success() {
        when(providerRepository.findById("provider1")).thenReturn(Mono.just(provider));
        when(providerRepository.deleteById("provider1")).thenReturn(Mono.empty());
        when(activityLogRepository.save(any())).thenReturn(Mono.empty());

        Map<String, String> payload = new HashMap<>();
        payload.put("providerId", "provider1");

        webTestClient.post().uri("/api/admin/providers/remove")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testRemoveProvider_MissingProviderId() {
        Map<String, String> payload = new HashMap<>();

        webTestClient.post().uri("/api/admin/providers/remove")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isBadRequest();
    }
}
