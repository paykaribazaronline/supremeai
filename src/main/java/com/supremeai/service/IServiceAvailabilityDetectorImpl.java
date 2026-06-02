package com.supremeai.service;

import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

@Service
public class IServiceAvailabilityDetectorImpl implements IServiceAvailabilityDetector {
    public IServiceAvailabilityDetectorImpl(AIProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
    }


    private static final Logger logger = LoggerFactory.getLogger(IServiceAvailabilityDetectorImpl.class);


    @Override
    public Mono<Boolean> isHealthy(String providerName) {
        return Mono.fromCallable(() -> {
                AIProvider provider = providerFactory.getProvider(providerName);
                return provider;
            })
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(provider -> provider.generate("Hi")
                .timeout(Duration.ofSeconds(3))
                .subscribeOn(Schedulers.boundedElastic())
                .map(response -> response != null && !response.isEmpty())
                .onErrorReturn(false));
    }
}
