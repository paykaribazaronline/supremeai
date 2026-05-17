package com.supremeai.service;

import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import com.supremeai.model.APIProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * Service for system-level auto-detection of the best available AI provider.
 * Provider order is now fully dynamic from Firestore — sorted by APIProvider.priority.
 */
@Service
public class SystemAutoDetectService {

    private static final Logger logger = LoggerFactory.getLogger(SystemAutoDetectService.class);

    @Autowired
    private AIProviderFactory providerFactory;

    @Autowired
    private com.supremeai.repository.ProviderRepository providerRepository;

    @Autowired
    private ConfigService configService;

    private volatile AIProvider cachedProvider = null;
    private volatile long cacheExpiry = 0;

    private long getCacheDurationMs() {
        return configService.getTimeout("cache_duration", 300000L);
    }

    public AIProvider getProvider() {
        long now = System.currentTimeMillis();
        if (cachedProvider != null && now < cacheExpiry) {
            return cachedProvider;
        }
        synchronized (this) {
            if (cachedProvider != null && now < cacheExpiry) {
                return cachedProvider;
            }

            List<APIProvider> activeProviders = providerRepository.findByStatus("active")
                    .sort(java.util.Comparator.comparingInt(APIProvider::getPriority))
                    .collectList()
                    .block();

            if (activeProviders != null) {
                for (APIProvider dbProvider : activeProviders) {
                    try {
                        String name = dbProvider.getName();
                        String key = dbProvider.getApiKey();
                        AIProvider provider = (key != null && !key.isEmpty())
                                ? providerFactory.getProvider(name, key)
                                : providerFactory.getProvider(name);
                        if (provider != null && isHealthy(provider)) {
                            logger.info("Auto-detected provider: {} (priority={})", name, dbProvider.getPriority());
                            cachedProvider = provider;
                            cacheExpiry = now + getCacheDurationMs();
                            return provider;
                        }
                    } catch (Exception e) {
                        logger.debug("Provider {} unavailable: {}", dbProvider.getName(), e.getMessage());
                    }
                }
            }

            throw new RuntimeException("No healthy AI provider available. Configure at least one provider in admin dashboard.");
        }
    }

    private boolean isHealthy(AIProvider provider) {
        try {
            String test = provider.generate("Hi").block(Duration.ofSeconds(2));
            return test != null && !test.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public void clearCache() {
        synchronized (this) {
            cachedProvider = null;
            cacheExpiry = 0;
        }
        logger.info("Auto-detect provider cache cleared");
    }
}
