package com.supremeai.config;

import com.supremeai.provider.*;
import com.supremeai.repository.ProviderRepository;
import com.supremeai.security.UnifiedSecretsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProviderConfig {
    private static final Logger log = LoggerFactory.getLogger(ProviderConfig.class);

    @Autowired
    private UnifiedSecretsService secretsService;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private AIProviderFactory providerFactory;

    @Bean
    public SupremeCoreProvider supremeCoreProvider() {
        log.info("[ProviderConfig] Initializing SupremeCoreProvider — cloud-only multi-orchestrated mode");
        return new SupremeCoreProvider(providerRepository, providerFactory);
    }
}
