package com.supremeai.service;

import com.supremeai.model.APIProvider;
import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import com.supremeai.repository.ProviderRepository;
import java.time.Duration;
import java.util.Comparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Service for system-level auto-detection of the best available AI provider. Provider order is now
 * fully dynamic from Firestore — sorted by APIProvider.priority. Fully reactive implementation
 * using non-blocking pipelines.
 */
@Service
public class SystemAutoDetectService {

  private static final Logger logger = LoggerFactory.getLogger(SystemAutoDetectService.class);

  @Autowired private AIProviderFactory providerFactory;

  @Autowired private ProviderRepository providerRepository;

  @Autowired private ConfigService configService;

  private volatile AIProvider cachedProvider = null;
  private volatile long cacheExpiry = 0;

  private long getCacheDurationMs() {
    return configService.getTimeout("cache_duration", 300000L);
  }

  /**
   * Get the best available provider reactively. Returns cached provider if available and not
   * expired.
   */
  public Mono<AIProvider> getProvider() {
    long now = System.currentTimeMillis();
    if (cachedProvider != null && now < cacheExpiry) {
      return Mono.just(cachedProvider);
    }

    return providerRepository
        .findByStatus("active")
        .sort(Comparator.comparingInt(APIProvider::getPriority))
        .flatMap(
            dbProvider ->
                Mono.fromCallable(() -> providerFactory.getProvider(dbProvider.getName()))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(this::isHealthyNonBlocking)
                    .filter(Boolean::booleanValue)
                    .map(healthy -> providerFactory.getProvider(dbProvider.getName())))
        .next()
        .doOnNext(
            provider -> {
              cachedProvider = provider;
              cacheExpiry = System.currentTimeMillis() + getCacheDurationMs();
              logger.info("Auto-detected provider: {}", provider.getName());
            })
        .switchIfEmpty(
            Mono.error(
                new RuntimeException(
                    "No healthy AI provider available. Configure at least one provider in admin dashboard.")));
  }

  /**
   * Truly non-blocking health check — returns Mono<Boolean>. No .block() calls; runs entirely on
   * reactive pipeline.
   */
  private Mono<Boolean> isHealthyNonBlocking(AIProvider provider) {
    return provider
        .generate("Hi")
        .timeout(Duration.ofSeconds(2))
        .subscribeOn(Schedulers.boundedElastic())
        .map(test -> test != null && !test.isEmpty())
        .onErrorReturn(false);
  }

  public void clearCache() {
    synchronized (this) {
      cachedProvider = null;
      cacheExpiry = 0;
    }
    logger.info("Auto-detect provider cache cleared");
  }
}
