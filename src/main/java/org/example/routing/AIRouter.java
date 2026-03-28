package org.example.routing;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Routes AI generation requests through a configurable provider sequence.
 *
 * <p>Key stability improvements:
 * <ul>
 *   <li>No {@code @Value} usage at construction time — provider sequence is
 *       initialised in {@link #init()} via {@code @PostConstruct}.</li>
 *   <li>HTTP client is a Spring-managed {@link RestTemplate} bean with
 *       configured timeouts (see {@code AIHttpClientConfig}).</li>
 *   <li>All response parsing validates structure before indexing; provider
 *       errors are wrapped in {@link AIProviderException} which never leaks
 *       API keys into messages or logs.</li>
 *   <li>Routing sequence can be changed at runtime (e.g. from Dashboard UI)
 *       and is persisted via {@link AIConfigRepository}.</li>
 *   <li>New providers are added via the {@link AIProvider} interface — no
 *       switch/case changes required here.</li>
 * </ul>
 */
@Service
public class AIRouter {

    private static final Logger log = LoggerFactory.getLogger(AIRouter.class);
    private static final String DEFAULT_SEQUENCE = "kimi,deepseek,gemini";

    private final AIProviderProperties properties;
    private final AIConfigRepository configRepository;
    private final Map<String, AIProvider> providerRegistry;

    /** Volatile so runtime updates are immediately visible to all threads. */
    private volatile List<String> aiSequence;

    /**
     * Constructor injection — all dependencies are fully resolved before
     * {@link #init()} runs, avoiding the {@code @Value}-in-constructor trap.
     */
    @Autowired
    public AIRouter(AIProviderProperties properties,
                    AIConfigRepository configRepository,
                    RestTemplate aiRestTemplate) {
        this.properties = properties;
        this.configRepository = configRepository;
        this.providerRegistry = buildRegistry(properties, aiRestTemplate);
    }

    /**
     * Package-private constructor for unit tests: accepts a pre-built provider
     * registry so tests can inject mocks without reflection.
     */
    AIRouter(AIProviderProperties properties,
             AIConfigRepository configRepository,
             Map<String, AIProvider> providerRegistry) {
        this.properties = properties;
        this.configRepository = configRepository;
        this.providerRegistry = Collections.unmodifiableMap(providerRegistry);
    }

    /** Initialise sequence after all Spring injection is complete. */
    @PostConstruct
    void init() {
        String order = configRepository.loadPriorityOrder()
                .filter(s -> !s.isBlank())
                .orElseGet(() -> {
                    String fromProps = properties.getPriorityOrder();
                    return (fromProps != null && !fromProps.isBlank()) ? fromProps : DEFAULT_SEQUENCE;
                });

        this.aiSequence = parseSequence(order);
        log.info("AIRouter initialised with sequence: {}", aiSequence);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Admin-callable: change the provider order at runtime.
     *
     * @param sequence comma-separated provider names, e.g. {@code "deepseek,kimi,gemini"}
     */
    public void setAISequence(String sequence) {
        List<String> parsed = parseSequence(sequence);
        this.aiSequence = parsed;
        configRepository.savePriorityOrder(sequence);
        log.info("AI sequence updated to: {}", parsed);
    }

    public String getCurrentSequence() {
        return String.join(",", aiSequence);
    }

    /**
     * Generate a response by trying each provider in sequence.
     *
     * <p>Per-provider errors are logged and accumulated; only when every
     * provider fails is an exception raised.
     *
     * @param prompt   the user prompt
     * @param taskType optional task-category hint passed to providers
     * @return successful response from the first provider that succeeds
     * @throws AIAllProvidersFailedException if every provider in the sequence fails
     */
    public AIResponse generateCode(String prompt, String taskType) {
        List<String> currentSequence = aiSequence; // snapshot for this request
        List<String> errors = new ArrayList<>(currentSequence.size());

        for (String providerName : currentSequence) {
            AIProvider provider = providerRegistry.get(providerName);
            if (provider == null) {
                log.warn("Provider '{}' in sequence is not registered — skipping", providerName);
                errors.add(providerName + ": not registered");
                continue;
            }

            try {
                log.debug("Trying provider: {}", providerName);
                AIResponse response = provider.generate(prompt, taskType);
                if (response != null && response.isSuccess()) {
                    log.info("Provider '{}' succeeded", providerName);
                    return response.withUsedAI(providerName);
                }
                errors.add(providerName + ": returned unsuccessful response");
            } catch (AIProviderException e) {
                log.warn("Provider '{}' failed: {}", providerName, e.getMessage());
                errors.add(providerName + ": " + e.getMessage());
            } catch (Exception e) {
                log.warn("Provider '{}' failed with unexpected error: {}",
                         providerName, e.getClass().getSimpleName());
                errors.add(providerName + ": unexpected error");
            }
        }

        throw new AIAllProvidersFailedException(errors);
    }

    // -------------------------------------------------------------------------
    // Package-private helpers (accessible from tests)
    // -------------------------------------------------------------------------

    /**
     * Parse a comma-separated sequence string into a list of trimmed lower-case names.
     * Returns the default sequence if the input is blank or null.
     */
    static List<String> parseSequence(String sequence) {
        if (sequence == null || sequence.isBlank()) {
            return Arrays.asList(DEFAULT_SEQUENCE.split(","));
        }
        List<String> result = Arrays.stream(sequence.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        return result.isEmpty()
                ? Arrays.asList(DEFAULT_SEQUENCE.split(","))
                : Collections.unmodifiableList(result);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static Map<String, AIProvider> buildRegistry(AIProviderProperties props,
                                                          RestTemplate restTemplate) {
        Map<String, AIProvider> registry = new java.util.LinkedHashMap<>();

        props.getProviders().forEach((name, config) -> {
            if (!config.isEnabled()) {
                log.info("Provider '{}' is disabled — skipping registration", name);
                return;
            }
            AIProvider provider;
            if ("gemini".equalsIgnoreCase(name)) {
                provider = new GeminiProvider(config, restTemplate);
            } else {
                provider = new OpenAICompatibleProvider(name, config, restTemplate);
            }
            registry.put(name.toLowerCase(), provider);
            log.info("Registered AI provider: {}", name);
        });

        return Collections.unmodifiableMap(registry);
    }

    // -------------------------------------------------------------------------
    // Response model
    // -------------------------------------------------------------------------

    public static class AIResponse {
        private final String code;
        private final boolean success;
        private String usedAI;

        public AIResponse(String code, boolean success) {
            this.code = code;
            this.success = success;
        }

        public AIResponse withUsedAI(String ai) {
            this.usedAI = ai;
            return this;
        }

        public String getCode()    { return code; }
        public boolean isSuccess() { return success; }
        public String getUsedAI()  { return usedAI; }
    }
}