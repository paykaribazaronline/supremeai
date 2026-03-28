package org.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Provides a Spring-managed {@link RestTemplate} for AI provider HTTP calls.
 *
 * <p>Sane timeouts are configured to protect Cloud Run instances from hanging
 * threads caused by slow or unresponsive AI provider endpoints:
 * <ul>
 *   <li>Connect timeout: 5 s — fail fast if the provider is unreachable</li>
 *   <li>Read timeout: 60 s — allow enough time for LLM inference</li>
 * </ul>
 */
@Configuration
public class AIHttpClientConfig {

    /** Connect timeout in milliseconds. Default: 5 s. */
    private static final int CONNECT_TIMEOUT_MS = 5_000;

    /** Read timeout in milliseconds. Default: 60 s (LLM inference can be slow). */
    private static final int READ_TIMEOUT_MS = 60_000;

    /**
     * Named bean so it can be injected specifically into AI-related services
     * without colliding with any other {@code RestTemplate} beans in the context.
     */
    @Bean("aiRestTemplate")
    public RestTemplate aiRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return new RestTemplate(factory);
    }
}
