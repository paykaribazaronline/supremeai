package com.supremeai.service;

import reactor.core.publisher.Mono;

public interface IServiceAvailabilityDetector {
    Mono<Boolean> isHealthy(String providerName);
}
