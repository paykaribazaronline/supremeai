package com.supremeai.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class IServiceAvailabilityDetectorImplTest {

  @Mock private AIProviderFactory providerFactory;

  @Mock private AIProvider provider;

  private IServiceAvailabilityDetectorImpl service;

  @BeforeEach
  void setUp() {
    service = new IServiceAvailabilityDetectorImpl(providerFactory);
  }

  @Test
  void isHealthy_shouldReturnTrueWhenProviderResponds() {
    when(providerFactory.getProvider(anyString())).thenReturn(provider);
    when(provider.generate(anyString())).thenReturn(Mono.just("ok"));

    Mono<Boolean> result = service.isHealthy("openai");

    StepVerifier.create(result).expectNext(true).verifyComplete();
  }

  @Test
  void isHealthy_shouldReturnFalseWhenProviderFails() {
    when(providerFactory.getProvider(anyString())).thenReturn(provider);
    when(provider.generate(anyString())).thenReturn(Mono.error(new RuntimeException("fail")));

    Mono<Boolean> result = service.isHealthy("openai");

    StepVerifier.create(result).expectNext(false).verifyComplete();
  }
}
