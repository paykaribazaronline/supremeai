package com.supremeai.config;

import com.supremeai.provider.*;
import com.supremeai.repository.ProviderRepository;
import com.supremeai.security.UnifiedSecretsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProviderConfig {
    public ProviderConfig(UnifiedSecretsService secretsService, ProviderRepository providerRepository, AIProviderFactory providerFactory) {
        this.secretsService = secretsService;
        this.providerRepository = providerRepository;
        this.providerFactory = providerFactory;
    }

    private static final Logger log = LoggerFactory.getLogger(ProviderConfig.class);




    @Bean
    public SupremeCoreProvider supremeCoreProvider() {
        log.info("[ProviderConfig] Initializing SupremeCoreProvider — cloud-only multi-orchestrated mode");
        return new SupremeCoreProvider(providerRepository, providerFactory);
    }
}
